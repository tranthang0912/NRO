package web;

import consts.ConstMap;
import consts.ConstNpc;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import map.ItemMap;
import map.Service.ChangeMapService;
import map.Service.MapService;
import map.WayPoint;
import map.Zone;
import mob.Mob;
import npc.list.Whis;
import player.Player;
import player.Service.InventoryService;
import server.MenuController;
import services.Service;
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
    private record StudyTrip(WebQuestStudy.Mentor mentor, WebQuestStudy.Lesson lesson, long deadline) {}
    private static final Predicate<Mob> ANY_MOB = mob -> true;
    private static final long QUEST_TIMEOUT_MS = 90000;
    private final WebSession session;
    private final WebCombatTarget targets = new WebCombatTarget();
    private final WebQuestCombat questCombat = new WebQuestCombat();
    private final Map<WayPoint, Long> blockedExits = new IdentityHashMap<>();
    private Mode mode = Mode.OFF;
    private String waiting = "";
    private boolean recovering;
    private boolean needBagSpace;
    private Zone farmZone;
    private Zone combatZone;
    private long lastProgress;
    private long lastBossSearch;
    private long retryRouteAt;
    private long retryActionAt;
    private long reviveAt;
    private long retryDamageTrainingAt;
    private long retryStudyAt;
    private long potentialForSkill;
    private StudyTrip studyTrip;
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
        if (player.zone == null || (requested == Mode.FARM && player.isDie()) || player.pvp != null || TransactionService.gI().check(player)) {
            stop("Hãy kết thúc giao dịch hoặc trận đấu, rồi bật Auto khi nhân vật còn sống.");
            return;
        }
        if (requested == Mode.FARM && !hasMob(player.zone, ANY_MOB)) {
            stop("Khu vực này không có quái để tự đánh.");
            return;
        }
        resetMotion();
        questCombat.reset(player);
        mode = requested;
        farmZone = requested == Mode.FARM ? player.zone : null;
        previousZone = null;
        nextLeg = null;
        transitStarted = 0;
        objective = objectiveCount = -1;
        lastBossSearch = retryRouteAt = retryActionAt = reviveAt = retryDamageTrainingAt = 0;
        retryStudyAt = potentialForSkill = 0;
        studyTrip = null;
        lastProgress = System.currentTimeMillis();
        waiting = "";
        recovering = false;
        needBagSpace = false;
        targets.clear();
        blockedExits.clear();
        publish("");
    }

    void restore(WebAutomation old) {
        if (!old.active()) return;
        old.questCombat.reset(session.player);
        mode = old.mode;
        farmZone = old.farmZone;
        recovering = old.recovering;
        needBagSpace = old.needBagSpace;
        reviveAt = old.reviveAt;
        retryDamageTrainingAt = old.retryDamageTrainingAt;
        lastProgress = System.currentTimeMillis();
        publish("");
    }

    boolean claims(Mob mob) {
        return active() && !recovering && targets.current() == mob;
    }

    void stop(String reason) {
        if (active()) resetMotion();
        questCombat.reset(session.player);
        mode = Mode.OFF;
        farmZone = previousZone = null;
        nextLeg = null;
        waiting = "";
        targets.clear();
        publish(reason);
    }

    private void publish(String reason) {
        String value = switch (mode) { case OFF -> "off"; case QUEST -> "quest"; case FARM -> "farm"; };
        session.send(Map.of("type", "automation", "mode", value, "reason", reason, "waiting", !waiting.isEmpty()));
    }

    private void waitFor(String reason) {
        if (waiting.equals(reason)) return;
        waiting = reason;
        publish(reason);
    }

    private void resumeWork() {
        if (waiting.isEmpty()) return;
        waiting = "";
        publish("");
    }

    private void resetMotion() {
        session.direction = session.vertical = previousDirection = 0;
        session.jump = false;
        session.autoPortalAttempt = null;
        blockedMoves = 0;
    }

    void update() {
        Player player = session.player;
        if (player == null || player.zone == null) {
            stop("Auto đã dừng vì nhân vật đã ngã gục.");
            return;
        }
        if (player.pvp != null || TransactionService.gI().check(player)) {
            stop("Auto đã dừng vì nhân vật đang giao dịch hoặc thách đấu.");
            return;
        }
        session.direction = session.vertical = 0;
        session.jump = false;
        if (player.isDie()) {
            questCombat.reset(player);
            if (mode == Mode.FARM) stop("Auto đánh đã dừng vì nhân vật ngã gục trong khu treo.");
            else reviveForQuest(player);
            return;
        }
        reviveAt = 0;
        if (combatZone != player.zone) {
            combatZone = player.zone;
            targets.clear();
            questCombat.reset(player);
        }
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

    private void reviveForQuest(Player player) {
        var task = player.playerTask.taskMain;
        if (task == null || task.id == 29 || task.index >= task.subTasks.size()
                || player.isPKDHVT || player.idNRNM != -1 || MapService.gI().isMapBlackBallWar(player.zone.map.mapId)) {
            stop("Auto NV không thể hồi sinh ở khu vực hoặc nhiệm vụ này.");
            return;
        }
        long now = System.currentTimeMillis();
        if (reviveAt == 0) {
            reviveAt = now + 2_000;
            resetMotion();
            session.velocityY = 0;
            targets.clear();
            waitFor("Auto NV sẽ hồi sinh miễn phí tại chỗ.");
            return;
        }
        if (now < reviveAt) return;
        nextLeg = null;
        lastProgress = now;
        Service.gI().hsChar(player, player.nPoint.hpMax, player.nPoint.mpMax);
        reviveAt = now + 2_000;
        resumeWork();
    }

    private void quest(Player player) throws Exception {
        if (WebQuestTransit.pending(player)) {
            long now = System.currentTimeMillis();
            if (transitStarted == 0) transitStarted = now;
            waitFor(now - transitStarted > 75_000 ? "Tàu tới tương lai vẫn chưa hoàn tất. Auto NV đang chờ máy chủ."
                    : "Đang chờ tàu đưa nhân vật tới tương lai.");
            lastProgress = now;
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
        if (id != objective) questCombat.reset(player);
        if (player.zone != previousZone || id != objective || step.count != objectiveCount) {
            previousZone = player.zone;
            nextLeg = null;
            objective = id;
            objectiveCount = step.count;
            lastProgress = System.currentTimeMillis();
            lastBossSearch = retryRouteAt = retryActionAt = 0;
            objectivePower = player.nPoint.power;
        }
        boolean gainPower = WebQuestTargets.power(id);
        if (gainPower && player.nPoint.power > objectivePower) {
            objectivePower = player.nPoint.power;
            lastProgress = System.currentTimeMillis();
        }
        long now = System.currentTimeMillis();
        if (nextLeg != null && now - lastProgress > QUEST_TIMEOUT_MS) {
            if (nextLeg.portal != null) blockedExits.put(nextLeg.portal, now + 15_000);
            nextLeg = null;
            retryRouteAt = now + 2_000;
            lastProgress = now;
            waitFor("Auto NV đang thử tìm lối khác tới mục tiêu.");
            return;
        }
        int home = 21 + player.gender;
        if (needBagSpace && makeBagSpace(player, home)) return;
        if (id == TASK_3_0 || id == TASK_27_0) {
            switch (WebQuestActions.train(player, id)) {
                case PROGRESSED -> { resumeWork(); lastProgress = now; }
                case NEED_POTENTIAL -> grind(player);
                case NEED_POWER_LIMIT -> waitFor("Auto NV đang chờ mở giới hạn Sức mạnh để kiếm thêm tiềm năng.");
                case AT_LIMIT -> waitFor("Auto NV đang chờ nhân vật đủ giới hạn chỉ số để làm nhiệm vụ.");
            }
            return;
        }
        if (id > TASK_3_0 && !WebMabuQuest.applies(id) && studySkills(player, now)) return;
        if (id == TASK_13_0) {
            if (WebQuestActions.checkClan(player)) { resumeWork(); return; }
            if (now >= retryActionAt) {
                retryActionAt = now + 3_000;
                if (session.world.tryQuestClanPair(session)) { resumeWork(); return; }
            }
            if (player.clan == null && player.inventory.gold < WebQuestActions.BASIC_CLAN_PRICE) { grind(player); return; }
            waitFor("Auto NV đang chờ người cùng bật Auto ở bước bang hội hoặc bang đủ thành viên.");
            return;
        }
        if (id == TASK_28_7) {
            boolean hasOsin = player.zone.map.npcs.stream().anyMatch(npc -> npc.tempId == ConstNpc.OSIN);
            if (hasOsin || atMap(player, WebMabuQuest.ENTRY_MAP)) talk(player, ConstNpc.OSIN);
            return;
        }
        if (WebMabuQuest.applies(id) && prepareMabu(player, id)) {
            return;
        }
        if (id == TASK_0_3 || id == TASK_0_4) {
            if (atMap(player, home)) starterItem(player, id);
            return;
        }
        if (id == TASK_3_1) {
            if (atMap(player, step.mapId) && !collect(player, 78)) waitFor("Auto NV đang chờ vật thể Sao băng xuất hiện.");
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
                waitFor("Auto NV đang chờ nhân vật đủ điều kiện bang hội của nhiệm vụ.");
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
            if (WebQuestActions.activateDetector(player)) hunt(player, WebQuestTargets::dropsDetectorItem);
            else prepareDetector(player, home);
            return;
        }
        if (gainPower) {
            TaskService.gI().checkDoneTaskPower(player, player.nPoint.power);
            if (id == TaskService.gI().getIdTask(player)) {
                grind(player);
            }
            return;
        }
        if (step.npcId >= 0) {
            if (step.mapId >= 0 && !atMap(player, step.mapId)) return;
            talk(player, step.npcId);
        } else {
            waitFor("Auto NV đang chờ điều kiện của bước hiện tại trong tab Nhiệm vụ.");
        }
    }

    private boolean studySkills(Player player, long now) throws Exception {
        if (player.zone.map.mapId == 154) {
            for (var npc : player.zone.map.npcs) {
                if (!(npc instanceof Whis whis) || !whis.canLearnFirstSpecial(player)) continue;
                if (player.zone.map.getNpc(player, ConstNpc.WHIS) == null) {
                    aim(player, npc.cx, npc.cy, 14, 24);
                    return true;
                }
                if (whis.learnFirstSpecial(player)) {
                    session.send(PlayerView.inventory(player));
                    session.send(PlayerView.stats(player));
                }
                return true;
            }
        }
        if (studyTrip != null && now >= studyTrip.deadline()) {
            studyTrip = null;
            nextLeg = null;
            potentialForSkill = 0;
            retryStudyAt = now + 60_000;
        }
        if (studyTrip == null && now >= retryStudyAt) {
            retryStudyAt = now + 3_000;
            potentialForSkill = 0;
            long studyTime = player.LearnSkill.Time;
            if (studyTime > now) return false;
            if (studyTime == -1 && WebQuestStudy.learnFromBag(session)) {
                session.send(PlayerView.inventory(player));
                session.send(PlayerView.stats(player));
                return true;
            }
            var lesson = studyTime == -1 ? WebQuestStudy.nextLesson(player) : null;
            if (studyTime == -1 && lesson == null) return false;
            var mentor = WebQuestStudy.mentor(player);
            boolean alreadyThere = player.zone.map.mapId == mentor.map();
            Leg first = alreadyThere ? null : findRoute(player, map -> map.mapId == mentor.map());
            if (!alreadyThere && first == null) return false;
            potentialForSkill = lesson == null ? 0 : lesson.potential();
            if (player.nPoint.tiemNang >= potentialForSkill) {
                studyTrip = new StudyTrip(mentor, lesson, now + 120_000);
                questCombat.reset(player);
                nextLeg = first;
                retryRouteAt = 0;
                lastProgress = now;
                if (!alreadyThere) session.notice("Auto NV đang đến gặp sư phụ để học kỹ năng.");
            }
        }
        if (studyTrip == null) return false;
        var mentor = studyTrip.mentor();
        if (!atMap(player, mentor.map())) return true;
        var npc = player.zone.map.getNpc(player, mentor.npc());
        if (npc == null) { approachNpc(player, mentor.npc()); return true; }
        var lesson = studyTrip.lesson();
        boolean completed = lesson == null ? WebQuestStudy.finish(session, npc, now) : WebQuestStudy.start(session, lesson);
        studyTrip = null;
        nextLeg = null;
        potentialForSkill = 0;
        retryStudyAt = now + (completed ? 3_000 : 30_000);
        if (completed) {
            resumeWork();
            session.send(PlayerView.stats(player));
            if (lesson == null) session.notice("Auto NV đã nhận kỹ năng đã học từ sư phụ.");
        }
        return true;
    }

    boolean readyForClanPair() {
        return mode == Mode.QUEST && session.connected && session.player != null && !session.player.isDie()
                && session.player.pvp == null && !TransactionService.gI().check(session.player)
                && TaskService.gI().getIdTask(session.player) == TASK_13_0;
    }

    private void prepareDetector(Player player, int home) throws Exception {
        var inventory = InventoryService.gI();
        if (inventory.findItemBag(player, WebQuestActions.DETECTOR) != null) {
            waitFor("Auto NV đang chờ nhân vật đủ điều kiện dùng máy dò.");
            return;
        }
        if (inventory.getCountEmptyBag(player) <= 0) { requireBagSpace(); return; }
        var stored = inventory.findItemBox(player, WebQuestActions.DETECTOR);
        if (stored != null) {
            if (!atMap(player, home)) return;
            if (player.zone.map.getNpc(player, ConstNpc.RUONG_DO) == null) { approachNpc(player, ConstNpc.RUONG_DO); return; }
            long now = System.currentTimeMillis();
            if (now >= retryActionAt) {
                retryActionAt = now + 1_000;
                session.world.withdraw(session, player.inventory.itemsBox.indexOf(stored));
            }
            return;
        }
        long cost = WebQuestActions.detectorGoldCost();
        if (cost < 0) { waitFor("Auto NV đang chờ cửa hàng gốc mở bán máy dò."); return; }
        if (player.inventory.gold < cost) { grind(player); return; }
        if (!atMap(player, WebQuestActions.DETECTOR_SHOP_MAP)) return;
        if (player.zone.map.getNpc(player, ConstNpc.BUNMA_TL) == null) { approachNpc(player, ConstNpc.BUNMA_TL); return; }
        long now = System.currentTimeMillis();
        if (now >= retryActionAt) {
            retryActionAt = now + 3_000;
            if (WebQuestActions.buyDetector(player)) resumeWork();
            else waitFor("Auto NV đang chờ đủ điều kiện mua máy dò bằng vàng trong game.");
        }
    }

    private void requireBagSpace() {
        if (!needBagSpace) nextLeg = null;
        needBagSpace = true;
    }

    private boolean makeBagSpace(Player player, int home) {
        var inventory = InventoryService.gI();
        if (inventory.getCountEmptyBag(player) > 0) {
            needBagSpace = false;
            nextLeg = null;
            resumeWork();
            return false;
        }
        int spare = -1;
        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
            var item = player.inventory.itemsBag.get(i);
            if (item.isNotNullItem() && item.template.type != 6 && item.template.type != 12
                    && switch (item.template.id) { case 12, 73, 74, 78, 379, 380, 444, 457 -> false; default -> true; }) {
                spare = i;
                break;
            }
        }
        if (spare < 0 || inventory.getCountEmptyListItem(player.inventory.itemsBox) <= 0) {
            waitFor("Auto NV đang chờ chỗ trống. Túi hoặc rương không còn chỗ phù hợp.");
            return true;
        }
        if (!atMap(player, home)) return true;
        if (player.zone.map.getNpc(player, ConstNpc.RUONG_DO) == null) { approachNpc(player, ConstNpc.RUONG_DO); return true; }
        long now = System.currentTimeMillis();
        if (now >= retryActionAt) {
            retryActionAt = now + 1_000;
            inventory.itemBagToBox(player, spare);
            session.send(PlayerView.inventory(player));
            lastProgress = now;
        }
        return true;
    }

    private void starterItem(Player player, int id) throws Exception {
        int npcId = id == TASK_0_3 ? ConstNpc.RUONG_DO : ConstNpc.DAU_THAN;
        var npc = player.zone.map.getNpc(player, npcId);
        if (npc == null) { approachNpc(player, npcId); return; }
        if (id == TASK_0_3) {
            long now = System.currentTimeMillis();
            if (now < retryActionAt) return;
            retryActionAt = now + 1_000;
            var box = player.inventory.itemsBox;
            var radar = InventoryService.gI().findItem(box, 12);
            if (radar == null) session.world.restoreChestObjective(player);
            else session.world.withdraw(session, box.indexOf(radar));
            if (TaskService.gI().getIdTask(player) == id) {
                if (InventoryService.gI().getCountEmptyBag(player) <= 0) requireBagSpace();
                else waitFor("Auto NV đang chờ lấy được rađa từ rương.");
            } else resumeWork();
        } else {
            if (player.magicTree.currPeas <= 0 || player.magicTree.isUpgrade) {
                waitFor("Auto NV đang chờ đậu thần chín để thu hoạch.");
                return;
            }
            if (InventoryService.gI().getCountEmptyBag(player) <= 0) { requireBagSpace(); return; }
            npc.openBaseMenu(player);
            npc.confirmMenu(player, 0);
            resumeWork();
        }
    }

    private void talk(Player player, int npcId) {
        var npc = player.zone.map.getNpc(player, npcId);
        if (npc == null) { approachNpc(player, npcId); return; }
        long now = System.currentTimeMillis();
        if (now < retryActionAt) return;
        retryActionAt = now + 4_000;
        session.autoMenu = true;
        try {
            MenuController.gI().openMenuNPC(session, npcId, player);
        } finally {
            session.autoMenu = false;
        }
        if (TaskService.gI().getIdTask(player) == objective) {
            waitFor("Auto NV đang chờ đủ điều kiện nhận hoặc báo nhiệm vụ với NPC.");
        } else resumeWork();
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
        resumeWork();
        if (best <= 65) {
            if (Util.canDoWithTime(player.idMark.getLastTimePickItem(), 1000)) {
                int dropId = nearest.itemMapId;
                session.world.pickup(session, nearest);
                if (TaskService.gI().getIdTask(player) == objective && player.zone.getItemMapByItemMapId(dropId) == nearest
                        && InventoryService.gI().getCountEmptyBag(player) == 0) {
                    requireBagSpace();
                }
            }
        } else aim(player, nearest.x, nearest.y, 12, 24);
        return true;
    }

    private void hunt(Player player, IntPredicate matches) {
        Predicate<Mob> filter = mob -> matches.test(mob.tempId);
        if (hasMob(player.zone, filter)) fightMob(player, filter);
        else travel(player, map -> hasMob(map.zones.get(0), filter));
    }

    private void grind(Player player) {
        int level = Service.gI().getCurrLevel(player);
        Predicate<Mob> suitable = mob -> mob.tempId != consts.ConstMob.MOC_NHAN
                && mob.level >= Math.max(1, level - 5) && mob.level <= level + 3;
        if (hasMob(player.zone, suitable)) {
            nextLeg = null;
            fightMob(player, suitable);
            return;
        }
        long now = System.currentTimeMillis();
        if (nextLeg == null && now >= retryRouteAt) {
            nextLeg = findRoute(player, map -> hasMob(map.zones.get(0), suitable));
            if (nextLeg == null && !hasMob(player.zone, ANY_MOB)) {
                nextLeg = findRoute(player, map -> hasMob(map.zones.get(0), ANY_MOB));
            }
            lastProgress = now;
            retryRouteAt = now + 3_000;
        }
        if (nextLeg != null) {
            resumeWork();
            followLeg(player);
        } else if (hasMob(player.zone, ANY_MOB)) {
            fightMob(player, ANY_MOB);
        } else {
            waitFor("Auto NV đang chờ khu vực phù hợp để luyện tập.");
        }
    }

    private static boolean hasMob(Zone zone, Predicate<Mob> matches) {
        return zone.mobs.stream().anyMatch(mob -> !mob.isBigBoss() && matches.test(mob));
    }

    private void fightMob(Player player, Predicate<Mob> matches) {
        collectNearbyLoot(player);
        Mob target = targets.select(player, matches, mob -> session.world.mobClaimedByAnother(session, mob), System.currentTimeMillis());
        if (target != null) strike(player, null, target, target.location.x, target.location.y);
        else {
            questCombat.reset(player);
            waitFor("Đang chờ quái phù hợp xuất hiện hoặc tìm đường tới mục tiêu khác.");
        }
    }

    private void collectNearbyLoot(Player player) {
        if (!Util.canDoWithTime(player.idMark.getLastTimePickItem(), 1000)) return;
        boolean bagHasRoom = InventoryService.gI().getCountEmptyBag(player) > 0;
        for (ItemMap item : player.zone.items) {
            if (item.playerId != player.id || item.itemTemplate == null || distance(player, item.x, item.y) > 65) continue;
            int type = item.itemTemplate.type;
            if (type == 9 || type == 10 || type == 34 || type == 6 && bagHasRoom) {
                session.world.pickup(session, item);
                return;
            }
        }
    }

    private void huntBoss(Player player, int id, int boss) {
        if (fightQuestBossInZone(player, id, boss)) return;
        int trigger = WebQuestTargets.bossTrigger(id);
        if (trigger != 0 && fightQuestBossInZone(player, id, trigger)) return;
        questCombat.reset(player);
        long now = System.currentTimeMillis();
        if (now - lastBossSearch >= 2_000) {
            lastBossSearch = now;
            nextLeg = findRoute(player, map -> map.zones.get(0).getBosses().stream()
                    .anyMatch(target -> WebQuestTargets.matchesBoss(target, id, boss)));
            int spawn = WebQuestTargets.bossMap(id);
            if (nextLeg == null && spawn >= 0 && spawn != player.zone.map.mapId) {
                nextLeg = findRoute(player, map -> map.mapId == spawn);
            }
        }
        if (nextLeg != null) followLeg(player);
        else waitFor("Auto NV đang chờ boss nhiệm vụ xuất hiện.");
    }

    private boolean fightQuestBossInZone(Player player, int id, int boss) {
        for (Player target : player.zone.getBosses()) {
            if (WebQuestTargets.matchesBoss(target, id, boss)) {
                nextLeg = null;
                if (SkillService.gI().canAttackPlayer(player, target)) {
                    long hp = target.nPoint.hp;
                    strike(player, target, null, target.location.x, target.location.y);
                    if (target.nPoint != null && target.nPoint.hp < hp) lastProgress = System.currentTimeMillis();
                } else {
                    questCombat.reset(player);
                    waitFor("Auto NV đang chờ boss có thể giao chiến.");
                }
                return true;
            }
        }
        return false;
    }

    private void strike(Player player, Player target, Mob mob, int x, int y) {
        int skillId = switch (player.gender) { case 0 -> Skill.DRAGON; case 1 -> Skill.DEMON; default -> Skill.GALICK; };
        Skill basic = player.playerSkill.getSkillbyId(skillId);
        if (basic == null || basic.point <= 0) {
            stop("Nhân vật cần học đòn cơ bản để Auto đánh.");
            return;
        }
        Skill skill = basic;
        double distance = distance(player, x, y);
        boolean quest = mode == Mode.QUEST;
        if (quest) {
            if (player.nPoint.stamina <= 0) { waitFor("Auto NV đang chờ hồi thể lực."); return; }
            long now = System.currentTimeMillis();
            if (objective > TASK_3_0 && objective != TASK_27_0 && now >= retryDamageTrainingAt) {
                retryDamageTrainingAt = now + 1_000;
                WebQuestActions.trainDamage(player.nPoint, player.nPoint.getDameLimit(), potentialForSkill);
            }
            skill = questCombat.select(player, target, mob, basic, distance);
            if (skill == null) return;
            if (skill.template.type == 3 || WebQuestCombat.selfHeal(skill)) {
                resumeWork();
                session.world.attack(session, skill, WebQuestCombat.selfHeal(skill) ? player : null, null, false);
                return;
            }
        } else {
            player.playerSkill.skillSelect = basic;
            if (!readyForCombat(player)) {
                if (player.nPoint.hp <= player.nPoint.hpMax / 4) retreat(player, target);
                return;
            }
        }
        resumeWork();
        if (distance > (skill == basic ? 78 : WebQuestCombat.range(skill))) { aim(player, x, y, 32, 42); return; }
        session.facing = x < player.location.x ? -1 : 1;
        session.world.attack(session, skill, target, mob, quest && skill == basic);
    }

    private boolean readyForCombat(Player player) {
        int minimumHp = Math.max(1, player.nPoint.hpMax / (recovering ? 2 : 4));
        if (player.nPoint.stamina > 0 && player.nPoint.hp >= minimumHp && SkillService.gI().canUseSkillWithMana(player)) {
            recovering = false;
            return true;
        }
        recovering = true;
        lastProgress = System.currentTimeMillis();
        targets.release();
        if (player.nPoint.stamina <= 0) {
            waitFor("Auto đang chờ hồi thể lực.");
            return false;
        }
        if (player.inventory.itemsBag.stream().anyMatch(item -> item.isNotNullItem() && item.template.type == 6)) {
            UseItem.gI().eatPea(player);
        }
        waitFor("Auto đang hồi HP và KI, đủ sức sẽ tự đánh tiếp.");
        return false;
    }

    private void retreat(Player player, Player boss) {
        int threatX = player.location.x;
        double nearest = 120;
        if (boss != null) {
            double distance = distance(player, boss.location.x, boss.location.y);
            if (distance < nearest) { nearest = distance; threatX = boss.location.x; }
        }
        for (Mob mob : player.zone.mobs) {
            if (mob.isDie() || !(mob.temporaryEnemies.contains(player) || mob.tempId > 18 || mob.tempId > 9 && mob.type == 4)) continue;
            double distance = distance(player, mob.location.x, mob.location.y);
            if (distance < nearest) { nearest = distance; threatX = mob.location.x; }
        }
        if (nearest >= 120) return;
        int x = player.location.x;
        int direction = x == threatX ? -session.facing : Integer.compare(x, threatX);
        int safeX = Math.max(0, Math.min(player.zone.map.mapWidth - 1, x + 160 * direction));
        if (safeX == x) safeX = Math.max(0, Math.min(player.zone.map.mapWidth - 1, x - 160 * direction));
        aim(player, safeX, player.location.y, 12, 24);
    }

    private boolean atMap(Player player, int mapId) {
        if (player.zone.map.mapId == mapId) return true;
        travel(player, map -> map.mapId == mapId);
        return false;
    }

    private void travel(Player player, Predicate<map.Map> goal) {
        long now = System.currentTimeMillis();
        if (nextLeg == null && now >= retryRouteAt) {
            nextLeg = findRoute(player, goal);
            lastProgress = now;
            retryRouteAt = now + 3_000;
        }
        if (nextLeg == null) { waitFor("Auto NV đang chờ lối hợp lệ tới map nhiệm vụ."); return; }
        resumeWork();
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
        if (player.zone == before && !WebQuestTransit.pending(player)) {
            retryRouteAt = System.currentTimeMillis() + 3_000;
            waitFor("Auto NV đang chờ NPC mở chuyến đi tới map nhiệm vụ.");
        }
    }

    private boolean prepareMabu(Player player, int id) {
        int mapId = player.zone.map.mapId;
        int floorBoss = WebMabuQuest.floorBoss(mapId);
        if (floorBoss == 0) {
            if (mapId == 127) {
                mabuTransport(player, new WebQuestTransit.Trip(WebMabuQuest.ENTRY_MAP, ConstNpc.OSIN,
                        player.isPhuHoMapMabu ? 1 : 2, ConstNpc.BASE_MENU));
            } else if (atMap(player, WebMabuQuest.ENTRY_MAP)) {
                if (WebMabuQuest.entryOpen()) mabuTransport(player, WebMabuQuest.entry());
                else waitFor("Auto NV đang chờ tới giờ mở phó bản Ma Bư của nhiệm vụ.");
            }
            return true;
        }
        int goalFloor = WebMabuQuest.floor(id);
        if (id == TASK_28_0 || goalFloor == mapId) return false;
        if (goalFloor < mapId) {
            waitFor("Auto NV đang chờ chuyến trở về để vào lại đúng tầng Ma Bư.");
            return true;
        }
        var trip = WebMabuQuest.descent(player);
        if (trip != null) mabuTransport(player, trip);
        else if (!fightQuestBossInZone(player, id, floorBoss)) {
            waitFor("Auto NV đang chờ boss tầng hiện tại để lấy điểm xuống tầng dưới.");
        }
        return true;
    }

    private void mabuTransport(Player player, WebQuestTransit.Trip trip) {
        var npc = player.zone.map.getNpc(player, trip.npcId());
        if (npc == null) { approachNpc(player, trip.npcId()); return; }
        long now = System.currentTimeMillis();
        if (now < retryActionAt) return;
        retryActionAt = now + 3_000;
        Zone before = player.zone;
        confirmTransport(player, npc, trip.menu(), trip.option());
        if (player.zone == before) waitFor("Auto NV đang chờ đủ điều kiện qua tầng Ma Bư.");
        else resumeWork();
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
        if (before == player.zone) {
            long now = System.currentTimeMillis();
            blockedExits.put(portal, now + 15_000);
            retryRouteAt = now + 3_000;
            waitFor("Auto NV đang chờ lối đi mở hoặc tìm đường khác.");
        }
    }

    private Leg findRoute(Player player, Predicate<map.Map> goal) {
        long now = System.currentTimeMillis();
        blockedExits.values().removeIf(until -> until <= now);
        var seen = new HashSet<Integer>();
        var queue = new ArrayDeque<Route>();
        queue.add(new Route(player.zone.map, null));
        seen.add(player.zone.map.mapId);
        while (!queue.isEmpty()) {
            Route route = queue.remove();
            if (route.first != null && goal.test(route.map)) return route.first;
            for (var exit : route.map.wayPoints) {
                if (route.first == null && blockedExits.containsKey(exit)) continue;
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
