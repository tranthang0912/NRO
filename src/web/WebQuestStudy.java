package web;

import consts.ConstNpc;
import npc.Npc;
import player.Player;
import server.Manager;
import server.MenuController;
import services.func.UseItem;
import shop.Shop;
import shop.ShopService;
import utils.SkillUtil;

/** Uses the same catalog, confirmation and timer as learning with a mentor by hand. */
final class WebQuestStudy {
    private static final int CONFIRM_STUDY = 671;
    record Mentor(int map, int npc) {}
    record Lesson(int itemId, long potential) {}

    static Mentor mentor(Player player) {
        return switch (player.gender) {
            case 0 -> new Mentor(5, ConstNpc.QUY_LAO_KAME);
            case 1 -> new Mentor(13, ConstNpc.TRUONG_LAO_GURU);
            default -> new Mentor(20, ConstNpc.VUA_VEGETA);
        };
    }

    static Lesson nextLesson(Player player) {
        Shop shop = school();
        if (shop == null) return null;
        Lesson cheapest = null;
        for (var tab : shop.tabShops) {
            for (var entry : tab.itemShops) {
                var item = entry.temp;
                if (item.type != 7 || (item.gender != player.gender && item.gender != 3)
                        || item.strRequire > player.nPoint.power || player.BoughtSkill.contains((int) item.id)
                        || SkillUtil.getSkillLevelByItemID(item.id) != 1) continue;
                int skillId = SkillUtil.getTempSkillSkillByItemID(item.id);
                if (skillId < 0 || learned(player, skillId)) continue;
                var skill = SkillUtil.createSkill(skillId, 1);
                if (skill != null && (cheapest == null || skill.powRequire < cheapest.potential())) {
                    cheapest = new Lesson(item.id, skill.powRequire);
                }
            }
        }
        return cheapest;
    }

    static boolean start(WebSession session, Lesson lesson) throws Exception {
        Player player = session.player;
        if (player.LearnSkill.Time != -1 || player.nPoint.tiemNang < lesson.potential()) return false;
        Shop school = school();
        if (school == null) return false;
        Shop shop = new Shop(school, player);
        var entry = shop.getItemShop(lesson.itemId());
        if (entry == null || entry.temp.strRequire > player.nPoint.power
                || learned(player, SkillUtil.getTempSkillSkillByItemID(lesson.itemId()))) return false;
        var previous = player.idMark.getShopOpen();
        int menu = player.idMark.getIndexMenu(), menuNpc = session.menuNpc, choices = session.menuChoices;
        session.autoMenu = true;
        try {
            player.idMark.setShopOpen(shop);
            ShopService.gI().buyItem(player, lesson.itemId());
            if (player.idMark.getIndexMenu() != CONFIRM_STUDY || player.LearnSkill.ItemTemplateSkillId != lesson.itemId()) return false;
            MenuController.gI().doSelectMenu(player, ConstNpc.CON_MEO, 0);
            return player.LearnSkill.Time > 0;
        } finally {
            player.idMark.setShopOpen(previous);
            player.idMark.setIndexMenu(menu);
            session.menuNpc = menuNpc;
            session.menuChoices = choices;
            session.autoMenu = false;
        }
    }

    static boolean finish(WebSession session, Npc mentor, long now) {
        Player player = session.player;
        if (player.LearnSkill.Time < 0 || player.LearnSkill.Time > now
                || (mentor.tempId == ConstNpc.QUY_LAO_KAME && player.canReward)) return false;
        int menu = player.idMark.getIndexMenu(), menuNpc = session.menuNpc, choices = session.menuChoices;
        session.autoMenu = true;
        try {
            player.idMark.setIndexMenu(ConstNpc.BASE_MENU);
            mentor.confirmMenu(player, 0);
            return player.LearnSkill.Time == -1;
        } finally {
            player.idMark.setIndexMenu(menu);
            session.menuNpc = menuNpc;
            session.menuChoices = choices;
            session.autoMenu = false;
        }
    }

    static boolean learnFromBag(WebSession session) throws Exception {
        Player player = session.player;
        for (int index = 0; index < player.inventory.itemsBag.size(); index++) {
            var book = player.inventory.itemsBag.get(index);
            if (!book.isNotNullItem() || book.template.type != 7 || book.template.strRequire > player.nPoint.power
                    || (book.template.gender != player.gender && book.template.gender != 3)
                    || SkillUtil.getSkillLevelByItemID(book.template.id) != 1) continue;
            int skillId = SkillUtil.getTempSkillSkillByItemID(book.template.id);
            if (skillId < 0 || learned(player, skillId)) continue;
            int slot = index;
            try (var packet = GameWorld.packet(-43, out -> {
                out.writeByte(3); // Confirm use of the eligible skill book.
                out.writeByte(1); // Bag.
                out.writeByte(slot);
            })) {
                UseItem.gI().doItem(player, packet.message());
            }
            return learned(player, skillId);
        }
        return false;
    }

    private static boolean learned(Player player, int skillId) {
        var known = player.playerSkill.getSkillbyId(skillId);
        return known != null && known.point > 0;
    }

    private static Shop school() {
        for (Shop shop : Manager.SHOPS) if ("QUY_LAO".equals(shop.tagName)) return shop;
        return null;
    }
}
