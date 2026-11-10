package web;

import consts.ConstMap;
import consts.ConstNpc;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import map.ItemMap;
import map.Service.ChangeMapService;
import map.Service.MapService;
import map.WayPoint;
import map.Zone;
import mob.Mob;
import player.Player;
import player.Service.InventoryService;
import server.MenuController;
import services.SkillService;
import services.TaskService;
import services.func.TransactionService;
import services.func.UseItem;
import skill.Skill;
import utils.Util;

import static consts.ConstTask.*;

/** Runs on the authoritative world thread. Each session controls only its own character. */
final class WebAutomation {
    private enum Mode { OFF, QUEST, FARM }
    private record Leg(WayPoint portal, WebQuestTransit.Trip trip) {}
    private record Route(map.Map map, Leg first) {}
    private static final IntPredicate ANY_MOB = template -> true;
    private static final long QUEST_TIMEOUT_MS = 90000;
    private final WebSession session;
    private Mode mode = Mode.OFF;
    private Zone farmZone;
    private long lastProgress;
    private long lastBossSearch;
    private int objective = -1;
    private int objectiveCount = -1;
    private long objectivePower;
    private Zone previousZone;
    private Leg nextLeg;
    private long transitStarted;
    private int previousX;
    private int previousDirection;
    private int blockedMoves;

    WebAutomation(WebSession session) { this.session = session; }

    boolean active() { return mode != Mode.OFF; }

    void setMode(String value) {
        Mode requested = switch (value) {
            case "off" -> Mode.OFF;
            case "quest" -> Mode.QUEST;
            case "farm" -> Mode.FARM;
            default -> throw new IllegalArgumentException("Chế độ Auto không hợp lệ.");
        };
        if (requested == Mode.OFF) {
            stop("");
            return;
        }
        Player player = session.player;
        if (player.zone == null || player.isDie() || player.pvp != null || TransactionService.gI().check(player)) {
            stop("Hãy kết thúc giao dịch hoặc trận đấu, rồi bật Auto khi nhân vật còn sống.");
            return;
        }
        if (requested == Mode.FARM && !hasMob(player.zone, ANY_MOB)) {
            stop("Khu vực này không có quái để tự đánh.");
            return;
        }
        resetMotion();
        mode = requested;
        farmZone = requested == Mode.FARM ? player.zone : null;
        previousZone = null;
        nextLeg = null;
        transitStarted = 0;
        objective = objectiveCount = -1;
        lastBossSearch = 0;
        lastProgress = System.currentTimeMillis();
        publish("");
    }

    void stop(String reason) {
        if (active()) resetMotion();
        mode = Mode.OFF;
        farmZone = previousZone = null;
        nextLeg = null;
        publish(reason);
    }

    private void publish(String reason) {
        String value = switch (mode) { case OFF -> "off"; case QUEST -> "quest"; case FARM -> "farm"; };
        session.send(Map.of("type", "automation", "mode", value, "reason", reason));
    }

    private void resetMotion() {
        session.direction = session.vertical = previousDirection = 0;
        session.jump = false;
        session.autoPortalAttempt = null;
        blockedMoves = 0;
    }

    void update() {
        Player player = session.player;
        if (player == null || player.zone == null || player.isDie()) {
            stop("Auto đã dừng vì nhân vật đã ngã gục.");
            return;
        }
        if (player.pvp != null || TransactionService.gI().check(player)) {
            stop("Auto đã dừng vì nhân vật đang giao dịch hoặc thách đấu.");
            return;
        }
        session.direction = session.vertical = 0;
        session.jump = false;
        if (player.newSkill.isStartSkillSpecial || player.effectSkill.isHaveEffectSkill()) return;
        try {
            if (mode == Mode.FARM) {
                if (player.zone != farmZone) stop("Auto đánh đã dừng vì nhân vật đổi khu vực.");
                else fightMob(player, ANY_MOB);
            } else {
                quest(player);
            }
        } catch (Exception error) {
            System.err.println("Auto stopped: " + error.getClass().getSimpleName() + ": " + error.getMessage());
            stop("Auto đã dừng ở bước này. Hãy thử thực hiện bước hiện tại bằng tay.");
        }
        previousX = player.location.x;
        previousDirection = session.direction;
    }

    private void quest(Player player) throws Exception {
        if (WebQuestTransit.pending(player)) {
            long now = System.currentTimeMillis();
            if (transitStarted == 0) transitStarted = now;
            if (now - transitStarted > 75000) stop("Chuyến đi chưa hoàn tất. Hãy kiểm tra lại lối vào map nhiệm vụ.");
            else lastProgress = now;
            return;
        }
        transitStarted = 0;
        var task = player.playerTask.taskMain;
        if (task == null || task.id == 29 || task.index >= task.subTasks.size()) {
            stop("Không còn nhiệm vụ chính để tự làm.");
            return;
        }
        var step = task.subTasks.get(task.index);
        int id = TaskService.gI().getIdTask(player);
        if (player.zone != previousZone || id != objective || step.count != objectiveCount) {
            previousZone = player.zone;
            nextLeg = null;
            objective = id;
            objectiveCount = step.count;
            lastProgress = System.currentTimeMillis();
            lastBossSearch = 0;
            objectivePower = player.nPoint.power;
        }
        boolean gainPower = WebQuestTargets.power(id);
        if (gainPower && player.nPoint.power > objectivePower) {
            objectivePower = player.nPoint.power;
            lastProgress = System.currentTimeMillis();
        }
        if (System.currentTimeMillis() - lastProgress > QUEST_TIMEOUT_MS) {
            stop(WebQuestTargets.boss(id) != 0 ? "Auto chưa thể tiếp tục săn boss. Hãy kiểm tra mục tiêu của nhiệm vụ."
                    : "Auto chưa thể tiếp tục. Hãy kiểm tra bước đang đánh dấu trong tab Nhiệm vụ.");
            return;
        }
        int home = 21 + player.gender;
        if (id == TASK_3_0 || id == TASK_27_0) {
            stop("Hãy tự chọn chỉ số cần tăng trong tab Kỹ năng, rồi bật lại Auto NV.");
            return;
        }
        if (id == TASK_13_0) {
            stop("Hãy tự tạo hoặc gia nhập bang hội theo nhiệm vụ, rồi bật lại Auto NV.");
            return;
        }
        if (id >= TASK_28_0 && id <= TASK_28_6 && !MapService.gI().isMapMaBu(player.zone.map.mapId)) {
            if (atMap(player, 52)) enterMabu(player);
            return;
        }
        if (id == TASK_0_3 || id == TASK_0_4) {
            if (atMap(player, home)) starterItem(player, id);
            return;
        }
        if (id == TASK_3_1) {
            if (atMap(player, step.mapId) && !collect(player, 78)) stop("Chưa tìm thấy vật thể nhiệm vụ trên map.");
            return;
        }
        if ((id == TASK_2_0 && collect(player, 73)) || ((id == TASK_8_1 || id == TASK_8_2) && collect(player, 444))
                || (id == TASK_27_1 && collect(player, 380))) return;

        int visit = WebQuestTargets.visit(id, player.gender, step.mapId);
        if (visit >= 0) {
            if (atMap(player, visit)) {
                var tasks = TaskService.gI();
                tasks.checkDoneTaskGoToMap(player, player.zone);
                tasks.restoreStarterHomeArrival(player);
            }
            return;
        }
        int monster = WebQuestTargets.mob(id, player.gender);
        if (monster >= 0) {
            if ((id == TASK_14_0 || id == TASK_14_1 || id == TASK_14_2 || id == TASK_15_0
                    || id == TASK_15_1 || id == TASK_15_2) && player.clan == null) {
                stop("Nhiệm vụ này cần nhân vật tham gia bang hội.");
                return;
            }
            hunt(player, template -> template == monster);
            return;
        }
        int boss = WebQuestTargets.boss(id);
        if (boss != 0) {
            huntBoss(player, id, boss);
            return;
        }
        if (id == TASK_27_1) {
            if (!player.itemTime.isUseMayDo) stop("Hãy bật máy dò của nhiệm vụ rồi bật lại Auto NV.");
            else hunt(player, WebQuestTargets::dropsDetectorItem);
            return;
        }
        if (gainPower) {
            TaskService.gI().checkDoneTaskPower(player, player.nPoint.power);
            if (id == TaskService.gI().getIdTask(player)) {
                hunt(player, ANY_MOB);
            }
            return;
        }
        if (step.npcId >= 0) {
            if (step.mapId >= 0 && !atMap(player, step.mapId)) return;
            talk(player, step.npcId);
        } else {
            stop("Bước này cần bạn thao tác trực tiếp. Xem tab Nhiệm vụ để tiếp tục.");
        }
    }

    private void starterItem(Player player, int id) throws Exception {
        int npcId = id == TASK_0_3 ? ConstNpc.RUONG_DO : ConstNpc.DAU_THAN;
        var npc = player.zone.map.getNpc(player, npcId);
        if (npc == null) { approachNpc(player, npcId); return; }
        if (id == TASK_0_3) {
            var box = player.inventory.itemsBox;
            var radar = InventoryService.gI().findItem(box, 12);
            if (radar == null) session.world.restoreChestObjective(player);
            else session.world.withdraw(session, box.indexOf(radar));
            if (TaskService.gI().getIdTask(player) == id) stop("Chưa lấy được rađa. Hãy kiểm tra chỗ trống trong hành trang.");
        } else {
            if (player.magicTree.currPeas <= 0 || player.magicTree.isUpgrade
                    || InventoryService.gI().getCountEmptyBag(player) <= 0) {
                stop("Chưa thể thu hoạch. Hãy kiểm tra cây đậu và chỗ trống trong hành trang.");
                return;
            }
            npc.openBaseMenu(player);
            npc.confirmMenu(player, 0);
        }
    }

    private void talk(Player player, int npcId) {
        var npc = player.zone.map.getNpc(player, npcId);
        if (npc == null) { approachNpc(player, npcId); return; }
        MenuController.gI().openMenuNPC(session, npcId, player);
        if (TaskService.gI().getIdTask(player) == objective) {
            stop("Bước gặp NPC này cần bạn thao tác trực tiếp.");
        }
    }

    private void approachNpc(Player player, int npcId) {
        for (var npc : player.zone.map.npcs) {
            if (npc.tempId == npcId) { aim(player, npc.cx, npc.cy, 14, 24); return; }
        }
        travel(player, map -> map.npcs.stream().anyMatch(npc -> npc.tempId == npcId));
    }

    private boolean collect(Player player, int itemId) {
        ItemMap nearest = null;
        double best = Double.MAX_VALUE;
        for (var item : player.zone.items) {
            if (!player.zone.isItemVisibleTo(player, item) || item.itemTemplate.id != itemId
                    || (item.playerId != player.id && item.playerId != -1)) continue;
            double distance = distance(player, item.x, item.y);
            if (distance < best) { nearest = item; best = distance; }
        }
        if (nearest == null) return false;
        if (best <= 65) {
            if (Util.canDoWithTime(player.idMark.getLastTimePickItem(), 1000)) {
                int dropId = nearest.itemMapId;
                session.world.pickup(session, nearest);
                if (TaskService.gI().getIdTask(player) == objective && player.zone.getItemMapByItemMapId(dropId) == nearest
                        && InventoryService.gI().getCountEmptyBag(player) == 0) {
                    stop("Hành trang đầy. Hãy dọn chỗ rồi bật lại Auto NV để nhặt vật phẩm nhiệm vụ.");
                }
            }
        } else aim(player, nearest.x, nearest.y, 12, 24);
        return true;
    }

    private void hunt(Player player, IntPredicate matches) {
        if (hasMob(player.zone, matches)) fightMob(player, matches);
        else travel(player, map -> hasMob(map.zones.get(0), matches));
    }

    private static boolean hasMob(Zone zone, IntPredicate matches) {
        return zone.mobs.stream().anyMatch(mob -> !mob.isBigBoss() && matches.test(mob.tempId));
    }

    private void fightMob(Player player, IntPredicate matches) {
        Mob nearest = null;
        double best = Double.MAX_VALUE;
        boolean bestReserved = true;
        for (Mob mob : player.zone.mobs) {
            if (mob.isDie() || mob.isBigBoss() || !matches.test(mob.tempId)) continue;
            boolean reserved = !mob.temporaryEnemies.contains(player) && mob.temporaryEnemies.stream()
                    .anyMatch(other -> other != null && other.zone == player.zone && !other.isDie()
                            && distance(other, mob.location.x, mob.location.y) <= 120);
            double distance = distance(player, mob.location.x, mob.location.y);
            if (reserved == bestReserved ? distance < best : !reserved) {
                nearest = mob;
                best = distance;
                bestReserved = reserved;
            }
        }
        if (nearest != null) strike(player, null, nearest, nearest.location.x, nearest.location.y);
    }

    private void huntBoss(Player player, int id, int boss) {
        for (Player target : player.zone.getBosses()) {
            if (WebQuestTargets.matchesBoss(target, id, boss)) {
                nextLeg = null;
                if (SkillService.gI().canAttackPlayer(player, target)) {
                    long hp = target.nPoint.hp;
                    strike(player, target, null, target.location.x, target.location.y);
                    if (target.nPoint != null && target.nPoint.hp < hp) lastProgress = System.currentTimeMillis();
                }
                return;
            }
        }
        long now = System.currentTimeMillis();
        if (now - lastBossSearch >= 2000) {
            lastBossSearch = now;
            nextLeg = findRoute(player, map -> map.zones.get(0).getBosses().stream()
                    .anyMatch(target -> WebQuestTargets.matchesBoss(target, id, boss)));
        }
        if (nextLeg != null) followLeg(player);
    }

    private void strike(Player player, Player target, Mob mob, int x, int y) {
        int skillId = switch (player.gender) { case 0 -> Skill.DRAGON; case 1 -> Skill.DEMON; default -> Skill.GALICK; };
        Skill skill = player.playerSkill.getSkillbyId(skillId);
        if (skill == null || skill.point <= 0 || player.nPoint.stamina <= 0) {
            stop("Nhân vật cần có đòn cơ bản và thể lực để Auto đánh.");
            return;
        }
        player.playerSkill.skillSelect = skill;
        boolean enoughMana = SkillService.gI().canUseSkillWithMana(player);
        if (player.nPoint.hp <= player.nPoint.hpMax / 4 || !enoughMana) {
            boolean hasPea = player.inventory.itemsBag.stream().anyMatch(item -> item.isNotNullItem() && item.template.type == 6);
            if (hasPea) UseItem.gI().eatPea(player);
            else stop("Auto đã dừng vì HP hoặc KI thấp và hết đậu thần.");
            return;
        }
        if (distance(player, x, y) > 78) { aim(player, x, y, 32, 42); return; }
        session.facing = x < player.location.x ? -1 : 1;
        session.world.attack(session, skill, target, mob);
    }

    private boolean atMap(Player player, int mapId) {
        if (player.zone.map.mapId == mapId) return true;
        travel(player, map -> map.mapId == mapId);
        return false;
    }

    private void travel(Player player, Predicate<map.Map> goal) {
        if (nextLeg == null) nextLeg = findRoute(player, goal);
        if (nextLeg == null) { stop("Chưa có lối tự đi đến map nhiệm vụ. Hãy xem chỉ dẫn trong tab Nhiệm vụ."); return; }
        followLeg(player);
    }

    private void followLeg(Player player) {
        if (nextLeg.portal != null) {
            moveToPortal(player, nextLeg.portal);
            return;
        }
        var trip = nextLeg.trip;
        var npc = player.zone.map.getNpc(player, trip.npcId());
        if (npc == null) {
            if (player.zone.map.npcs.stream().noneMatch(n -> n.tempId == trip.npcId())) nextLeg = null;
            else approachNpc(player, trip.npcId());
            return;
        }
        Zone before = player.zone;
        confirmTransport(player, npc, trip.menu(), trip.option());
        nextLeg = null;
        if (player.zone == before && !WebQuestTransit.pending(player)) stop("NPC chưa mở chuyến đi này. Hãy xem điều kiện vào map.");
        if (WebQuestTransit.pending(player)) session.notice("Đang đi tới tương lai theo thời gian chờ gốc của tàu.");
    }

    private void enterMabu(Player player) {
        var npc = player.zone.map.getNpc(player, ConstNpc.OSIN);
        if (npc == null) { approachNpc(player, ConstNpc.OSIN); return; }
        if (!utils.TimeUtil.isMabuOpen() || utils.TimeUtil.isMabu14HOpen()) {
            npc.openBaseMenu(player);
            stop("Chưa đến giờ mở phó bản Ma Bư của nhiệm vụ. Hãy bật lại Auto khi sự kiện mở.");
            return;
        }
        Zone before = player.zone;
        confirmTransport(player, npc, ConstNpc.MENU_OPEN_MMB, 0);
        if (player.zone == before) stop("Chưa thể vào phó bản Ma Bư. Hãy xem điều kiện của Ôsin.");
    }

    private void confirmTransport(Player player, npc.Npc npc, int menu, int option) {
        session.menuChoices = 0;
        session.autoMenu = true;
        try {
            npc.openBaseMenu(player);
            if (session.menuNpc == npc.tempId && session.menuChoices > option && player.idMark.getIndexMenu() == menu) {
                session.menuChoices = 0;
                npc.confirmMenu(player, option);
            }
        } finally {
            session.autoMenu = false;
        }
    }

    private void moveToPortal(Player player, WayPoint portal) {
        if (player.zone.map.getWaypointAt(player.location.x, player.location.y) == portal) {
            enterPortal(player, portal);
        } else {
            int margin = Math.max(4, (portal.maxX - portal.minX) / 2 - 2);
            int x = (portal.minX + portal.maxX) / 2;
            int y = (portal.minY + portal.maxY) / 2;
            boolean leftEdge = portal.minX == 0;
            boolean rightEdge = portal.maxX >= player.zone.map.mapWidth - 1;
            if ((leftEdge || rightEdge) && Math.abs(player.location.x - x) <= margin + 48) {
                if (player.location.y < portal.minY || player.location.y > portal.maxY) {
                    // Adjust height from inside the map before entering a boundary tile.
                    int approachX = leftEdge ? portal.maxX + 18 : portal.minX - 18;
                    aim(player, approachX, y, 9, 4);
                    return;
                }
                aim(player, x, y, margin, 4);
                if (Math.abs(player.location.y - y) > 4) session.vertical = Integer.signum(y - player.location.y);
            } else {
                aim(player, x, y, margin, 8);
            }
        }
    }

    boolean approaches(WayPoint portal) {
        return mode == Mode.QUEST && portal != null && nextLeg != null && nextLeg.portal == portal;
    }

    void enterPortal(Player player, WayPoint portal) {
        Zone before = player.zone;
        session.world.portal(session, portal);
        nextLeg = null;
        if (before == player.zone) stop("Lối đi chưa mở. Hãy xem điều kiện trong tab Nhiệm vụ.");
    }

    private Leg findRoute(Player player, Predicate<map.Map> goal) {
        var seen = new HashSet<Integer>();
        var queue = new ArrayDeque<Route>();
        queue.add(new Route(player.zone.map, null));
        seen.add(player.zone.map.mapId);
        while (!queue.isEmpty()) {
            Route route = queue.remove();
            if (route.first != null && goal.test(route.map)) return route.first;
            for (var exit : route.map.wayPoints) {
                addRoute(player, seen, queue, exit.goMap, route.first == null ? new Leg(exit, null) : route.first);
            }
            for (var trip : WebQuestTransit.from(player, route.map, objective)) {
                addRoute(player, seen, queue, trip.destination(), route.first == null ? new Leg(null, trip) : route.first);
            }
        }
        return null;
    }

    private static void addRoute(Player player, HashSet<Integer> seen, ArrayDeque<Route> queue, int destination, Leg first) {
        if (!seen.add(destination)) return;
        var map = MapService.gI().getMapById(destination);
        if (map == null || map.zones.isEmpty() || (map.type != ConstMap.MAP_NORMAL && map.type != ConstMap.MAP_OFFLINE)
                || ChangeMapService.gI().checkMapCanJoin(player, map.zones.get(0)) == null) return;
        queue.add(new Route(map, first));
    }

    private void aim(Player player, int x, int y, int xTolerance, int yTolerance) {
        int dx = x - player.location.x;
        int dy = y - player.location.y;
        int direction = Math.abs(dx) > xTolerance ? Integer.signum(dx) : 0;
        boolean blocked = direction != 0 && direction == previousDirection && player.location.x == previousX;
        blockedMoves = blocked ? blockedMoves + 1 : 0;
        session.direction = direction;
        if (direction != 0) session.facing = direction;
        if (blockedMoves >= 6 && player.nPoint.mp > 0) session.vertical = -1;
        else if (blocked) session.jump = true;
        else if (direction == 0 && Math.abs(dy) > yTolerance) session.vertical = Integer.signum(dy);
    }

    private static double distance(Player player, int x, int y) {
        return GameWorld.distance(player.location.x, player.location.y, x, y);
    }
}
