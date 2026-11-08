package combine;

import consts.ConstNpc;
import item.Item;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;
import services.Service;
import item.Template;
import utils.Util;

public class TaoDaHematite {

    private static final int GOLD_TAO_DA = 50_000_000;
    private static final int RATIO_TAO_DA = 100;  
    private static final int REQUIRED_ITEM_QUANTITY = 5;
    private static final short HEMATITE_ITEM_ID = 1423;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item saoPhaLe = player.combineNew.itemsCombine.get(0);
            if (isValidSaoPhaLe(saoPhaLe)) {
                player.combineNew.goldCombine = GOLD_TAO_DA;
                player.combineNew.ratioCombine = RATIO_TAO_DA;

                String npcSay = buildCombineInfoMessage(player);

                if (player.inventory.gold < player.combineNew.goldCombine) {
                    npcSay += "|7|Còn thiếu " + Util.powerToString(player.combineNew.goldCombine - player.inventory.gold) + " vàng\n";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Tạo đá Hematite\n" + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n", "Từ chối");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 5 sao pha lê Cấp 2", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 5 sao pha lê Cấp 2", "Đóng");
        }
    }

    public static void taoDaHematite(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            int gold = player.combineNew.goldCombine;
            if (player.inventory.gold < gold) {
                Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện");
                return;
            }

            Item saoPhaLe = player.combineNew.itemsCombine.get(0);
            if (isValidSaoPhaLe(saoPhaLe)) {
                player.inventory.gold -= gold;
                InventoryService.gI().subQuantityItemsBag(player, saoPhaLe, REQUIRED_ITEM_QUANTITY);
                if (Util.isTrue(player.combineNew.ratioCombine, 100)) {
                    createHematiteItem(player);
                    CombineService.gI().sendEffectSuccessCombine(player);
                } else {
                    CombineService.gI().sendEffectFailCombine(player);
                }
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                CombineService.gI().reOpenItemCombine(player);
            } else {
                Service.gI().sendThongBao(player, "Không đủ sao pha lê để tạo đá Hematite");
            }
        }
    }
    private static boolean isValidSaoPhaLe(Item saoPhaLe) {
        return saoPhaLe.template.id >= 441 && saoPhaLe.template.id <= 447 && saoPhaLe.quantity >= REQUIRED_ITEM_QUANTITY;
    }
    private static String buildCombineInfoMessage(Player player) {
        StringBuilder npcSay = new StringBuilder();
        npcSay.append("|2|Tạo đá Hematite từ sao pha lê\n")
              .append("|2|Cần 5 sao pha lê Cấp 2\n")
              .append("|2|Tỉ lệ thành công: ").append(player.combineNew.ratioCombine).append("%\n")
              .append("|2|Cần: ").append(Util.numberToMoney(player.combineNew.goldCombine)).append(" vàng\n")
              .append("|7|Thất bại -5 sao pha lê\n");
        return npcSay.toString();
    }
    private static void createHematiteItem(Player player) {
        Template.ItemTemplate hematiteTemplate = ItemService.gI().getTemplate(HEMATITE_ITEM_ID);
        Item hematite = new Item();
        hematite.template = hematiteTemplate;
        hematite.quantity = 1;
        InventoryService.gI().addItemBag(player, hematite);
    }
}