package combine;

import consts.ConstNpc;
import item.Item;
import item.Template;
import player.Player;
import services.ItemService;
import services.Service;
import player.Service.InventoryService;
import utils.Util;

public class NangCapDeBlackGoku {
    private static final int ITEM_ID_TRUNG_MABU = 568;
    private static final int REQUIRED_TRUNG_MABU = 15;
    private static final int ITEM_ID_DE_BLACK_GOKU = 1774;
    private static final int GOLD_TAO_DE = 50_000_000;
    private static final int SUCCESS_RATIO = 80;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item trungMabu = player.combineNew.itemsCombine.get(0);
            if (trungMabu.template.id == ITEM_ID_TRUNG_MABU && trungMabu.quantity >= REQUIRED_TRUNG_MABU) {
                player.combineNew.goldCombine = GOLD_TAO_DE;
                player.combineNew.ratioCombine = SUCCESS_RATIO;

                StringBuilder npcSay = new StringBuilder();
                npcSay.append("|2|Tạo Đệ Black từ Trứng Mabư\n");
                npcSay.append("|2|Cần ").append(REQUIRED_TRUNG_MABU).append(" Trứng Mabư\n");
                npcSay.append("|2|Tỉ lệ thành công: ").append(SUCCESS_RATIO).append("%\n");
                npcSay.append("|2|Cần: ").append(Util.numberToMoney(GOLD_TAO_DE)).append(" vàng\n");
                npcSay.append("|7|Thất bại -").append(REQUIRED_TRUNG_MABU).append(" Trứng Mabư\n");

                if (player.inventory.gold < GOLD_TAO_DE) {
                    npcSay.append("|7|Còn thiếu ")
                          .append(Util.powerToString(GOLD_TAO_DE - player.inventory.gold))
                          .append(" vàng\n");
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay.toString(), "Đóng");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(
                            player,
                            ConstNpc.MENU_START_COMBINE,
                            npcSay.toString(),
                            "Tạo Đệ Black Goku\n" + Util.numberToMoney(GOLD_TAO_DE) + " vàng\n",
                            "Từ chối");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 15 Trứng Mabư", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 15 Trứng Mabư", "Đóng");
        }
    }

    public static void nangCapDeBlackGoku(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item trungMabu = player.combineNew.itemsCombine.get(0);
            int gold = player.combineNew.goldCombine;

            if (trungMabu.template.id != ITEM_ID_TRUNG_MABU || trungMabu.quantity < REQUIRED_TRUNG_MABU) {
                Service.gI().sendThongBao(player, "Không đủ Trứng Mabư");
                return;
            }

            if (player.inventory.gold < gold) {
                Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện");
                return;
            }
            player.inventory.gold -= gold;
            InventoryService.gI().subQuantityItemsBag(player, trungMabu, REQUIRED_TRUNG_MABU);
            if (Util.isTrue(SUCCESS_RATIO, 100)) {
                Template.ItemTemplate deBlackTemplate = ItemService.gI().getTemplate(ITEM_ID_DE_BLACK_GOKU);
                Item deBlack = new Item();
                deBlack.template = deBlackTemplate;
                deBlack.quantity = 1;
                InventoryService.gI().addItemBag(player, deBlack);
                CombineService.gI().sendEffectSuccessCombine(player);
            } else {
                CombineService.gI().sendEffectFailCombine(player);
            }

            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            CombineService.gI().reOpenItemCombine(player);
        }
    }
}