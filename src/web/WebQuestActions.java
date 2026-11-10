package web;

import consts.ConstNpc;
import player.NPoint;
import player.Player;
import player.Service.ClanService;
import player.Service.InventoryService;
import server.Client;
import server.Manager;
import services.FlagBagService;
import services.Service;
import services.TaskService;
import services.func.UseItem;
import shop.Shop;
import shop.ShopService;

import static consts.ConstTask.TASK_3_0;
import static consts.ConstTask.TASK_13_0;
import static consts.ConstTask.TASK_27_0;
import static consts.ConstTask.TASK_27_1;

final class WebQuestActions {
    enum Training { PROGRESSED, NEED_POTENTIAL, NEED_POWER_LIMIT, AT_LIMIT }

    static final int DETECTOR = 379;
    static final int DETECTOR_SHOP_MAP = 102;
    private static final int GOLD_PRICE = 100_000_000;
    private static final int GOLD_PAYMENT = 0;
    private static final int NORMAL_SHOP = 0;
    private static final int FUTURE_SHOP_TAB = 20;
    private static final byte BASIC_CLAN_FLAG = 0;
    static final int BASIC_CLAN_PRICE = 10_000;
    private static final int MAX_TRAINING_BATCH = 1000;
    private static final byte HP = 0;
    private static final byte KI = 1;
    private static final byte DAMAGE = 2;

    private WebQuestActions() {}

    static Training train(Player player, int taskId) {
        var tasks = TaskService.gI();
        if (tasks.getIdTask(player) != taskId) return Training.PROGRESSED;
        NPoint points = player.nPoint;
        if (taskId == TASK_3_0) return trainStarter(points);
        if (taskId != TASK_27_0) throw new IllegalArgumentException("Unexpected training quest");

        int goal = TaskService.CAPSULE_QUEST_BASE_DAMAGE;
        if (points.dameg >= goal) {
            tasks.checkDoneTaskNangCS(player);
            return Training.PROGRESSED;
        }
        if (points.getDameLimit() < goal) return Training.AT_LIMIT;
        if (points.tiemNang < damageCost(points.dameg, 1)) return Training.NEED_POTENTIAL;

        int low = 1;
        int high = Math.min(MAX_TRAINING_BATCH, goal - points.dameg);
        while (low < high) {
            int amount = low + (high - low + 1) / 2;
            if (damageCost(points.dameg, amount) <= points.tiemNang) low = amount;
            else high = amount - 1;
        }
        points.increasePoint(DAMAGE, (short) low);
        return Training.PROGRESSED;
    }

    private static Training trainStarter(NPoint points) {
        boolean canEarnPotential = points.power <= points.getPowerLimit();
        if (points.dameg > 0 && points.dameg < points.getDameLimit()) {
            if (points.tiemNang >= damageCost(points.dameg, 1)) {
                points.increasePoint(DAMAGE, (short) 1);
                return Training.PROGRESSED;
            }
            if (canEarnPotential) return Training.NEED_POTENTIAL;
        }
        long hpCost = points.hpg + 20L <= points.getHpMpLimit() ? points.hpg + 1000L : Long.MAX_VALUE;
        long kiCost = points.mpg + 20L <= points.getHpMpLimit() ? points.mpg + 1000L : Long.MAX_VALUE;
        long cost = Math.min(hpCost, kiCost);
        if (cost == Long.MAX_VALUE) return Training.AT_LIMIT;
        if (points.tiemNang < cost) return canEarnPotential ? Training.NEED_POTENTIAL : Training.NEED_POWER_LIMIT;
        points.increasePoint(cost == hpCost ? HP : KI, (short) 1);
        return Training.PROGRESSED;
    }

    private static long damageCost(int current, int amount) {
        return amount * (2L * current + amount - 1) / 2 * 100;
    }

    static boolean checkClan(Player player) {
        return TaskService.gI().checkDoneTaskJoinClan(player);
    }

    /** Called by the world thread after both sessions opt in to the same clan quest. */
    static boolean pairClans(Player first, Player second) throws Exception {
        if (first.id == second.id || !canPairClan(first) || !canPairClan(second)
                || (first.clan != null && second.clan != null)) return false;
        Player leader = first.clan != null || (second.clan == null && first.id < second.id) ? first : second;
        Player member = leader == first ? second : first;
        var clans = ClanService.gI();
        if (leader.clan == null) {
            var flag = FlagBagService.gI().getFlagBag(BASIC_CLAN_FLAG);
            if (flag == null || flag.gold != BASIC_CLAN_PRICE || flag.gem > 0) return false;
            if (leader.inventory.gold < BASIC_CLAN_PRICE) {
                if (member.inventory.gold < BASIC_CLAN_PRICE) return false;
                leader = member;
                member = leader == first ? second : first;
            }
            String name = "Bang Auto " + leader.id;
            try (var packet = GameWorld.packet(-46, out -> {
                out.writeByte(2); // Create clan.
                out.writeByte(BASIC_CLAN_FLAG);
                out.writeUTF(name);
            })) {
                clans.getClan(leader, packet.message());
            }
        }

        var clan = leader.clan;
        if (clan == null || !clan.isLeader(leader) || clan.getCurrMembers() >= clan.maxMember) return false;
        try (var packet = GameWorld.packet(-51, out -> {
            out.writeByte(2); // Request clan membership.
            out.writeInt(clan.id);
        })) {
            clans.clanMessage(member, packet.message());
        }
        for (var request : clan.getCurrClanMessages()) {
            if (request.type != 2 || request.role != -1 || request.playerId != member.id) continue;
            try (var packet = GameWorld.packet(-54, out -> {
                out.writeInt(request.id);
                out.writeByte(0); // Leader approves the membership request.
            })) {
                clans.joinClan(leader, packet.message());
            }
            return member.clan == clan;
        }
        return false;
    }

    private static boolean canPairClan(Player player) {
        return player.isPl() && !player.isDie() && !player.idMark.isHoldBlackBall()
                && Client.gI().getPlayer(player.id) == player && TaskService.gI().getIdTask(player) == TASK_13_0;
    }

    static boolean activateDetector(Player player) throws Exception {
        if (player.itemTime.isUseMayDo) return true;
        if (TaskService.gI().getIdTask(player) != TASK_27_1) return false;
        var detector = InventoryService.gI().findItemBag(player, DETECTOR);
        if (detector == null || detector.quantity < 1 || player.nPoint.power < detector.template.strRequire) return false;
        int slot = player.inventory.itemsBag.indexOf(detector);
        try (var packet = GameWorld.packet(-43, out -> {
            out.writeByte(0); // Use.
            out.writeByte(1); // Bag.
            out.writeByte(slot);
        })) {
            UseItem.gI().doItem(player, packet.message());
        }
        return player.itemTime.isUseMayDo;
    }

    static long detectorGoldCost() {
        return detectorShop() != null ? GOLD_PRICE : -1;
    }

    static boolean buyDetector(Player player) {
        var inventory = InventoryService.gI();
        if (inventory.findItemBag(player, DETECTOR) != null || player.itemTime.isUseMayDo) return true;
        if (TaskService.gI().getIdTask(player) != TASK_27_1 || player.zone == null
                || player.zone.map.mapId != DETECTOR_SHOP_MAP
                || player.zone.map.getNpc(player, ConstNpc.BUNMA_TL) == null
                || player.inventory.gold < GOLD_PRICE || inventory.getCountEmptyBag(player) < 1
                || inventory.findItemBox(player, DETECTOR) != null) return false;

        Shop shop = detectorShop();
        if (shop == null || player.nPoint.power < shop.getItemShop(DETECTOR).temp.strRequire) return false;
        Shop previous = player.idMark.getShopOpen();
        player.idMark.setShopOpen(shop);
        try {
            ShopService.gI().buyItem(player, DETECTOR);
        } finally {
            player.idMark.setShopOpen(previous);
        }
        Service.gI().sendMoney(player);
        return inventory.findItemBag(player, DETECTOR) != null;
    }

    private static Shop detectorShop() {
        for (Shop shop : Manager.SHOPS) {
            if (!"BUNMA_FUTURE".equals(shop.tagName) || shop.npcId != ConstNpc.BUNMA_TL
                    || shop.typeShop != NORMAL_SHOP) continue;
            var detector = shop.getItemShop(DETECTOR);
            if (detector != null && detector.typeSell == GOLD_PAYMENT && detector.cost == GOLD_PRICE
                    && detector.tabShop.id == FUTURE_SHOP_TAB) return shop;
        }
        return null;
    }
}
