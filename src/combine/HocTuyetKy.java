package combine;

import consts.ConstFont;
import consts.ConstNpc;
import item.Template;
import item.Item;
import player.Player;
import player.Service.InventoryService;
import services.Service;
import services.SkillService;
import skill.Skill;
import utils.SkillUtil;
import utils.Util;

public class HocTuyetKy {

    // Constants
    public static final int MAX_SKILL_LEVEL = 7; 
    public static final int SKILL_SUPER_KAME = Skill.SUPER_KAME;
    public static final int SKILL_MA_PHONG_BA = Skill.MA_PHONG_BA; 
    public static final int SKILL_LIEN_HOAN_CHUONG = Skill.LIEN_HOAN_CHUONG;
    public static final int REQUIRED_BIKIP_1 = 9999; 
    public static final int REQUIRED_BIKIP_2 = 999;
    public static final int REQUIRED_GEM_1 = 99; 
    public static final int REQUIRED_GOLD = 10_000_000;
    public static final int REQUIRED_BIKIP_TEMPLATE_ID = 1229; 

    public static void showInfoCombine(Player player) {
        Item biKipTuyetKy = InventoryService.gI().findItem(player.inventory.itemsBag, REQUIRED_BIKIP_TEMPLATE_ID);
        int quantityBiKipTuyetKy = (biKipTuyetKy != null) ? biKipTuyetKy.quantity : 0;
        int gem = player.inventory.getGem();
        long gold = player.inventory.gold;
        int skillId = getSkillIdByGender(player.gender);
        Skill curSkill = SkillUtil.getSkillbyId(player, skillId);
        int nextPoint = (curSkill != null && curSkill.point > 0) ? curSkill.point + 1 : 1;

        if (nextPoint > MAX_SKILL_LEVEL) {
            Service.gI().sendServerMessage(player, "Kỹ năng đã đạt tối đa!");
            return;
        }

        Template.SkillTemplate skillTemplate = SkillUtil.findSkillTemplate(skillId);
        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_GREEN)
            .append("Qua sẽ dạy ngươi tuyệt kỹ ")
            .append(skillTemplate.name)
            .append(" ")
            .append(nextPoint)
            .append("\n");

        int requiredBiKip = (nextPoint == 1) ? REQUIRED_BIKIP_1 : REQUIRED_BIKIP_2;
        int requiredGem = (nextPoint == 1) ? REQUIRED_GEM_1 : 0;
        int requiredGold = REQUIRED_GOLD;

        appendRequirement(text, "Bí kíp tuyệt kỹ", quantityBiKipTuyetKy, requiredBiKip);
        appendRequirement(text, "Giá vàng", gold, requiredGold);
        if (requiredGem > 0) {
            appendRequirement(text, "Giá ngọc", gem, requiredGem);
        }

        if (quantityBiKipTuyetKy < requiredBiKip || gold < requiredGold || gem < requiredGem) {
            CombineService.gI().whis.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Từ chối");
            return;
        }

        CombineService.gI().whis.createOtherMenu(player, ConstNpc.HOC_TUYET_KY, text.toString(), "Đồng ý", "Từ chối");
    }

    public static void hocTuyetKy(Player player) {
        Item biKipTuyetKy = InventoryService.gI().findItem(player.inventory.itemsBag, REQUIRED_BIKIP_TEMPLATE_ID);
        int quantityBiKipTuyetKy = (biKipTuyetKy != null) ? biKipTuyetKy.quantity : 0;
        int gem = player.inventory.getGem();
        long gold = player.inventory.gold;
        int skillId = getSkillIdByGender(player.gender);
        Skill curSkill = SkillUtil.getSkillbyId(player, skillId);
        int nextPoint = (curSkill != null && curSkill.point > 0) ? curSkill.point + 1 : 1;

        if (nextPoint > MAX_SKILL_LEVEL) {
            return;
        }

        int requiredBiKip = (nextPoint == 1) ? REQUIRED_BIKIP_1 : REQUIRED_BIKIP_2;
        int requiredGem = (nextPoint == 1) ? REQUIRED_GEM_1 : 0;
        int requiredGold = REQUIRED_GOLD;

        if (quantityBiKipTuyetKy < requiredBiKip || gold < requiredGold || gem < requiredGem) {
            return;
        }

        Template.SkillTemplate skillTemplate = SkillUtil.findSkillTemplate(skillId);
        Skill nextSkill = SkillUtil.createSkill(skillTemplate.id, nextPoint);
        SkillUtil.setSkill(player, nextSkill);

        player.inventory.subGem(requiredGem);
        player.inventory.gold -= requiredGold;
        InventoryService.gI().subQuantityItemsBag(player, biKipTuyetKy, requiredBiKip);

        CombineService.gI().whis.npcChat(player, "Búm ba la xì bùa");
        CombineService.gI().sendEffSuccessVip(player, skillTemplate.iconId);
        InventoryService.gI().sendItemBags(player);
    }

    private static int getSkillIdByGender(int gender) {
        switch (gender) {
            case 0:
                return SKILL_SUPER_KAME;
            case 1:
                return SKILL_MA_PHONG_BA;
            default:
                return SKILL_LIEN_HOAN_CHUONG;
        }
    }

    private static void appendRequirement(StringBuilder text, String label, long current, long required) {
        text.append((current < required) ? ConstFont.BOLD_RED : ConstFont.BOLD_BLUE)
            .append(label)
            .append(": ")
            .append(current)
            .append("/")
            .append(required)
            .append("\n");
    }
}