package combine;

import consts.ConstNpc;
import item.Item;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;
import services.Service;
import item.Template;
import utils.Util;

public class NangCapItemCap2 {

    // Constants
    private static final int GOLD_TAO_DA = 50_000_000;
    private static final int RATIO_TAO_DA = 80; 
    private static final int ITEM_ID_C1_MIN = 381;
    private static final int ITEM_ID_C1_MAX = 385;
    private static final int ITEM_C2_ID_MIN = 1150;
    private static final int ITEM_C2_ID_MAX = 1154;
    private static final int C2_ITEM_COUNT = 10; 
    private static final String ITEM_C1_NAME = "Item Cấp 1"; 
    private static final String ITEM_C2_NAME = "Item Cấp 2"; 

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item itemc1 = player.combineNew.itemsCombine.get(0);
            if (itemc1.template.id >= ITEM_ID_C1_MIN && itemc1.template.id <= ITEM_ID_C1_MAX && itemc1.quantity >= C2_ITEM_COUNT) {
                player.combineNew.goldCombine = GOLD_TAO_DA;
                player.combineNew.ratioCombine = RATIO_TAO_DA;

                String npcSay = "|2|Tạo " + ITEM_C2_NAME + " từ " + ITEM_C1_NAME + "\n";
                npcSay += "|2|Cần 10 " + ITEM_C1_NAME + " để lên " + ITEM_C2_NAME + "\n";
                npcSay += "|2|Tỉ lệ thành công: " + player.combineNew.ratioCombine + "%\n";
                npcSay += "|2|Cần: " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n";
                npcSay += "|7|Thất bại - 10 " + ITEM_C1_NAME + "\n";

                if (player.inventory.gold < player.combineNew.goldCombine) {
                    npcSay += "|7|Còn thiếu " + Util.powerToString(player.combineNew.goldCombine - player.inventory.gold) + " vàng\n";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Tạo " + ITEM_C2_NAME + "\n" + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n", "Từ chối");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 10 " + ITEM_C1_NAME, "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 10 " + ITEM_C1_NAME, "Đóng");
        }
    }

    public static void Itemc2(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            int gold = player.combineNew.goldCombine;

            if (player.inventory.gold < gold) {
                Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện");
                return;
            }

            Item itemc1 = player.combineNew.itemsCombine.get(0);

            if (itemc1.template.id >= ITEM_ID_C1_MIN && itemc1.template.id <= ITEM_ID_C1_MAX && itemc1.quantity >= C2_ITEM_COUNT) {
                player.inventory.gold -= gold;
                InventoryService.gI().subQuantityItemsBag(player, itemc1, C2_ITEM_COUNT);
                if (Util.isTrue(player.combineNew.ratioCombine, 100)) {
                    int randomId = Util.nextInt(ITEM_C2_ID_MIN, ITEM_C2_ID_MAX); 
                    Template.ItemTemplate Itemc2Template = ItemService.gI().getTemplate(randomId);
                    Item itemc2 = new Item();
                    itemc2.template = Itemc2Template;
                    itemc2.quantity = 1;
                    InventoryService.gI().addItemBag(player, itemc2);
                    CombineService.gI().sendEffectSuccessCombine(player);
                } else {
                    CombineService.gI().sendEffectFailCombine(player);
                }

                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                CombineService.gI().reOpenItemCombine(player);
            } else {
                Service.gI().sendThongBao(player, "Không đủ " + ITEM_C1_NAME + " để tạo " + ITEM_C2_NAME);
            }
        }
    }
}