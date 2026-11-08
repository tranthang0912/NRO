package combine;

import consts.ConstNpc;
import item.Item;
import item.Template;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;
import services.Service;
import utils.Util;

public class CheTaoDuiDuc {
    private static final int GOLD_REQUIRED = 50_000_000;
    private static final int COMBINE_SUCCESS_RATE = 100;
    private static final int HEMATITE_ID = 1423;
    private static final int DUIDUC_ID = 1438;
    private static final int HEMATITE_REQUIRED_QUANTITY = 5;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item hematite = player.combineNew.itemsCombine.get(0);
            if (hematite.template.id == HEMATITE_ID && hematite.quantity >= HEMATITE_REQUIRED_QUANTITY) {
                player.combineNew.goldCombine = GOLD_REQUIRED;
                player.combineNew.ratioCombine = COMBINE_SUCCESS_RATE;

                String npcSay = "|2|Tạo Dùi Đục từ Đá Hematite\n"
                        + "|2|Cần " + HEMATITE_REQUIRED_QUANTITY + " viên Hematite\n"
                        + "|2|Tỉ lệ thành công: " + COMBINE_SUCCESS_RATE + "%\n"
                        + "|2|Cần: " + Util.numberToMoney(GOLD_REQUIRED) + " vàng\n"
                        + "|7|Thất bại -5 đá Hematite\n";

                if (player.inventory.gold < GOLD_REQUIRED) {
                    long goldShortage = GOLD_REQUIRED - player.inventory.gold;
                    npcSay += "|7|Còn thiếu " + Util.powerToString(goldShortage) + " vàng\n";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Tạo đá Dùi Đục\n" + Util.numberToMoney(GOLD_REQUIRED) + " vàng\n", "Từ chối");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 5 Viên Đá Hematite", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 5 Viên Đá Hematite", "Đóng");
        }
    }

    public static void CheTaoDuiDuc(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            if (player.inventory.gold < GOLD_REQUIRED) {
                Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện");
                return;
            }

            Item hematite = player.combineNew.itemsCombine.get(0);
            if (hematite.template.id == HEMATITE_ID && hematite.quantity >= HEMATITE_REQUIRED_QUANTITY) {
                player.inventory.gold -= GOLD_REQUIRED;
                InventoryService.gI().subQuantityItemsBag(player, hematite, HEMATITE_REQUIRED_QUANTITY);

                if (Util.isTrue(COMBINE_SUCCESS_RATE, 100)) {
                    Template.ItemTemplate duiducTemplate = ItemService.gI().getTemplate(DUIDUC_ID);
                    Item duiduc = new Item();
                    duiduc.template = duiducTemplate;
                    duiduc.quantity = 1;

                    InventoryService.gI().addItemBag(player, duiduc);
                    CombineService.gI().sendEffectSuccessCombine(player);
                } else {
                    CombineService.gI().sendEffectFailCombine(player);
                }

                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                CombineService.gI().reOpenItemCombine(player);
            } else {
                Service.gI().sendThongBao(player, "Không đủ đá Hematite để tạo Dùi Đục");
            }
        }
    }
}