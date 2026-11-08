package combine;

import consts.ConstNpc;
import item.Item;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;
import services.Service;
import item.Template;
import utils.Util;

public class DanhBongSaoPhaLe {

    private static final int GOLD_NANG_CAP = 100_000_000;
    private static final int RATIO_NANG_CAP = 100;

    private static final int ID_SAO_PHA_LE_START = 1416;
    private static final int ID_SAO_PHA_LE_END = 1422;
    private static final int ID_DA_MAI = 1439;
    private static final int ID_SAO_PHA_LE_LAP_LANH_START = 1426;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item saoPhaLe = null;
            Item daMai = null;

            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id >= ID_SAO_PHA_LE_START && item.template.id <= ID_SAO_PHA_LE_END) {
                    saoPhaLe = item;
                } else if (item.template.id == ID_DA_MAI) {
                    daMai = item;
                }
            }

            if (saoPhaLe != null && daMai != null && saoPhaLe.quantity >= 2) {
                player.combineNew.goldCombine = GOLD_NANG_CAP;
                player.combineNew.ratioCombine = RATIO_NANG_CAP;

                String npcSay = "|2|Nâng cấp Sao Pha Lê lên Sao Pha Lê Lấp Lánh\n";
                npcSay += "|2|Tỉ lệ thành công: " + player.combineNew.ratioCombine + "%\n";
                npcSay += "|2|Cần 1 đá mài\n";
                npcSay += "|2|Cần: " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n";
                npcSay += "|7|Thất bại -1 đá mài\n";

                if (player.inventory.gold < player.combineNew.goldCombine) {
                    npcSay += "|7|Còn thiếu " + Util.powerToString(player.combineNew.goldCombine - player.inventory.gold) + " vàng\n";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Nâng cấp\n" + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n"
                                    + Util.numberToMoney(player.combineNew.gemCombine) + " ngọc\n",
                            "Từ chối");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần x2 Sao Pha Lê và 1 đá mài", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần x2 Sao Pha Lê và 1 đá mài", "Đóng");
        }
    }

    public static void danhBongSaoPhaLe(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            int gold = player.combineNew.goldCombine;
            int gem = player.combineNew.gemCombine;

            if (player.inventory.gold < gold) {
                Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện");
                return;
            }
            if (player.inventory.gem < gem) {
                Service.gI().sendThongBao(player, "Không đủ ngọc để thực hiện");
                return;
            }

            Item saoPhaLe = null;
            Item daMai = null;

            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.id >= ID_SAO_PHA_LE_START && item.template.id <= ID_SAO_PHA_LE_END) {
                    saoPhaLe = item;
                } else if (item.template.id == ID_DA_MAI) {
                    daMai = item;
                }
            }

            if (saoPhaLe != null && daMai != null && saoPhaLe.quantity >= 2) {
                player.inventory.gold -= gold;
                player.inventory.gem -= gem;

                if (Util.isTrue(player.combineNew.ratioCombine, 100)) {
                    int saoPhaLeLapLanhId = ID_SAO_PHA_LE_LAP_LANH_START + (saoPhaLe.template.id - ID_SAO_PHA_LE_START);
                    Template.ItemTemplate newTemplate = ItemService.gI().getTemplate(saoPhaLeLapLanhId);
                    Item newItem = new Item();
                    newItem.template = newTemplate;
                    newItem.quantity = 1;

                    for (Item.ItemOption option : saoPhaLe.itemOptions) {
                        Item.ItemOption newOption = new Item.ItemOption(option.optionTemplate.id, option.param + 1);
                        newItem.itemOptions.add(newOption);
                    }

                    InventoryService.gI().addItemBag(player, newItem);
                    InventoryService.gI().subQuantityItemsBag(player, saoPhaLe, 2);
                    CombineService.gI().sendEffectSuccessCombine(player);
                } else {
                    CombineService.gI().sendEffectFailCombine(player);
                }

                InventoryService.gI().subQuantityItemsBag(player, daMai, 1);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                CombineService.gI().reOpenItemCombine(player);
            }
        }
    }
}