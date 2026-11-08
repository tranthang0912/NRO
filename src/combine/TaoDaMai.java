package combine;

import consts.ConstNpc;
import item.Item;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;
import services.Service;
import item.Template;
import utils.Util;

public class TaoDaMai {

    private static final int GOLD_TAO_DA = 50_000_000;
    private static final int RATIO_TAO_DA = 100;
    private static final int REQUIRED_DUIDUC_QUANTITY = 5;
    private static final short ITEM_ID_DUIDUC = 1438;
    private static final short ITEM_ID_DAMAI = 1439;      

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item duiDuc = player.combineNew.itemsCombine.get(0);
            if (isValidDuiDuc(duiDuc)) {
                player.combineNew.goldCombine = GOLD_TAO_DA;
                player.combineNew.ratioCombine = RATIO_TAO_DA;

                String npcSay = buildCombineInfoMessage(player);

                if (player.inventory.gold < GOLD_TAO_DA) {
                    npcSay += "|7|Còn thiếu " + Util.powerToString(GOLD_TAO_DA - player.inventory.gold) + " vàng\n";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Tạo đá Mài\n" + Util.numberToMoney(GOLD_TAO_DA) + " vàng\n", "Từ chối");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cần 5 Dùi Đục", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cần 5 Dùi Đục", "Đóng");
        }
    }

    public static void cheTaoDaMai(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            int gold = GOLD_TAO_DA;

            if (player.inventory.gold < gold) {
                Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện");
                return;
            }

            Item duiDuc = player.combineNew.itemsCombine.get(0);
            if (isValidDuiDuc(duiDuc)) {
                player.inventory.gold -= gold;
                InventoryService.gI().subQuantityItemsBag(player, duiDuc, REQUIRED_DUIDUC_QUANTITY);

                if (Util.isTrue(RATIO_TAO_DA, 100)) {
                    createDaMai(player);
                    CombineService.gI().sendEffectSuccessCombine(player);
                } else {
                    CombineService.gI().sendEffectFailCombine(player);
                }

                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                CombineService.gI().reOpenItemCombine(player);
            } else {
                Service.gI().sendThongBao(player, "Không đủ Dùi Đục tạo đá Mài");
            }
        }
    }
    private static boolean isValidDuiDuc(Item item) {
        return item.template.id == ITEM_ID_DUIDUC && item.quantity >= REQUIRED_DUIDUC_QUANTITY;
    }
    private static void createDaMai(Player player) {
        Template.ItemTemplate daMaiTemplate = ItemService.gI().getTemplate(ITEM_ID_DAMAI);
        Item daMai = new Item();
        daMai.template = daMaiTemplate;
        daMai.quantity = 1;
        InventoryService.gI().addItemBag(player, daMai);
    }

    private static String buildCombineInfoMessage(Player player) {
        return "|2|Tạo Đá Mài từ Dùi Đục\n"
                + "|2|Cần 5 Dùi Đục\n"
                + "|2|Tỉ lệ thành công: " + RATIO_TAO_DA + "%\n"
                + "|2|Cần: " + Util.numberToMoney(GOLD_TAO_DA) + " vàng\n"
                + "|7|Thất bại -5 Dùi Đục\n";
    }
}