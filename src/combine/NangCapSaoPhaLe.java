package combine;

import consts.ConstNpc;
import item.Item;
import item.Template.ItemTemplate;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;
import services.Service;
import utils.Util;

public class NangCapSaoPhaLe {

    private static final int GOLD_NANG_CAP = 200_000_000;  
    private static final int GEM_NANG_CAP = 10;
    private static final int RATIO_NANG_CAP = 50;
    private static final int[] SAO_PHA_LE_CAP_1_IDS = {441, 442, 443, 444, 445, 446, 447}; 
    private static final int[] SAO_PHA_LE_CAP_2_IDS = {1416, 1417, 1418, 1419, 1420, 1421, 1422}; 
    private static final int HEMATITE_ID = 1423;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item saoPhaLe = null;
            Item hematite = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (isSaoPhaLeCap1(item.template.id)) {
                    saoPhaLe = item; 
                } else if (item.template.id == HEMATITE_ID) {
                    hematite = item;
                }
            }
            if (saoPhaLe != null && hematite != null) {
                player.combineNew.goldCombine = GOLD_NANG_CAP;
                player.combineNew.gemCombine = GEM_NANG_CAP;
                player.combineNew.ratioCombine = RATIO_NANG_CAP;

                String npcSay = "|2|Nâng cấp Sao Pha Lê từ cấp 1 lên cấp 2\n";
                npcSay += "|2|Tỉ lệ thành công: " + player.combineNew.ratioCombine + "%\n";
                npcSay += "|2|Cần 1 đá Hematite\n";
                npcSay += "|2|Cần: " + Util.numberToMoney(player.combineNew.gemCombine) + " ngọc\n";
                npcSay += "|2|Cần: " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n";
                npcSay += "|7|Thất bại -1 đá Hematite\n";
                if (player.inventory.getGem() < player.combineNew.gemCombine) {
                    npcSay += "|7|Còn thiếu " + (player.combineNew.gemCombine - player.inventory.gem) + " ngọc xanh\n";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else if (player.inventory.gold < player.combineNew.goldCombine) {
                    npcSay += "|7|Còn thiếu " + Util.powerToString(player.combineNew.goldCombine - player.inventory.gold) + " vàng\n";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Nâng cấp\n" + Util.numberToMoney(player.combineNew.goldCombine) + " vàng\n"
                            + Util.numberToMoney(player.combineNew.gemCombine) + " ngọc\n", "Từ chối");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 Sao Pha Lê cấp 1 và 1 đá Hematite", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 Sao Pha Lê cấp 1 và 1 đá Hematite", "Đóng");
        }
    }

    public static void nangCapSaoPhaLe(Player player) {
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
            Item hematite = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (isSaoPhaLeCap1(item.template.id)) {
                    saoPhaLe = item; 
                } else if (item.template.id == HEMATITE_ID) {
                    hematite = item; 
                }
            }

            if (saoPhaLe != null && hematite != null) {
                player.inventory.gold -= gold;
                player.inventory.gem -= gem;
                if (Util.isTrue(player.combineNew.ratioCombine, 100)) {
                    int getSaoPhaLeCap2Id = getSaoPhaLeCap2Id(saoPhaLe.template.id);
                    ItemTemplate newTemplate = ItemService.gI().getTemplate(getSaoPhaLeCap2Id);
                    Item newItem = new Item();
                    newItem.template = newTemplate;
                    newItem.quantity = 1;
                    newItem.itemOptions.clear();
                    for (Item.ItemOption option : saoPhaLe.itemOptions) {
                        newItem.itemOptions.add(new Item.ItemOption(option.optionTemplate.id, option.param));
                    }

                    InventoryService.gI().addItemBag(player, newItem);
                    InventoryService.gI().subQuantityItemsBag(player, saoPhaLe, 1);
                    CombineService.gI().sendEffectSuccessCombine(player);
                } else {
                    CombineService.gI().sendEffectFailCombine(player);
                }
                InventoryService.gI().subQuantityItemsBag(player, hematite, 1);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                CombineService.gI().reOpenItemCombine(player);
            }
        }
    }

    private static boolean isSaoPhaLeCap1(int itemId) {
        for (int id : SAO_PHA_LE_CAP_1_IDS) {
            if (itemId == id) {
                return true;
            }
        }
        return false;
    }

    private static int getSaoPhaLeCap2Id(int saoPhaLeCap1Id) {
        for (int i = 0; i < SAO_PHA_LE_CAP_1_IDS.length; i++) {
            if (saoPhaLeCap1Id == SAO_PHA_LE_CAP_1_IDS[i]) {
                return SAO_PHA_LE_CAP_2_IDS[i];
            }
        }
        return -1;
    }
}