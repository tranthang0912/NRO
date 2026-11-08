package combine;

import consts.ConstNpc;
import item.Item;
import item.Item.ItemOption;
import player.Player;
import player.Service.InventoryService;
import services.Service;
import utils.Util;

public class CuongHoaLoSaoPhaLe {

    // Hằng số
    private static final int COST = 500_000_000;
    private static final int HEMATITE_ID = 1423;
    private static final int DUIDUC_ID = 1438;
    private static final int OPTION_ID_STAR = 107;
    private static final int OPTION_ID_PHA_LE = 228;
    private static final int OPTION_ID_EFFECT = 218;
    private static final int TARGET_STAR = 9;
    private static final int CURRENT_STAR = 8;
    private static final int SUCCESS_RATE = 50;

    public static void showInfoCombine(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            if (player.combineNew.itemsCombine.size() == 3) {
                Item item = null, hematite = null, duiDuc = null;

                for (Item i : player.combineNew.itemsCombine) {
                    if (CombineSystem.isTrangBiPhaLeHoa(i)) {
                        item = i;
                    } else if (i.template.id == HEMATITE_ID) {
                        hematite = i;
                    } else if (i.template.id == DUIDUC_ID) {
                        duiDuc = i;
                    }
                }

                if (item != null && hematite != null && duiDuc != null
                        && hematite.quantity >= 1 && duiDuc.quantity >= 1) {

                    String npcSay = item.template.name + "\n|2|";
                    for (ItemOption io : hematite.itemOptions) {
                        npcSay += io.getOptionString() + "\n";
                    }
                    npcSay += "Cường hóa\nÔ sao pha lê thứ " + CURRENT_STAR + "\n" + item.template.name
                            + "\nTỉ lệ thành công: " + SUCCESS_RATE + "%\n"
                            + "|7| Cần 1 " + hematite.template.name
                            + "\n|7| Cần 1 " + duiDuc.template.name
                            + "\nCần " + Util.numberToMoney(COST) + " vàng";

                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay, "Cường Hóa", "Từ chối");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Bạn chưa bỏ đủ vật phẩm !!!", "Đóng");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cần bỏ đủ vật phẩm yêu cầu", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Hành trang cần ít nhất 1 chỗ trống", "Đóng");
        }
    }

    public static void cuongHoaLoSaoPhaLe(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) return;

        if (player.inventory.gold < COST) {
            Service.gI().sendThongBao(player, "Con cần thêm vàng để cường hóa...");
            return;
        }

        if (!player.combineNew.itemsCombine.isEmpty()) {
            Item item = null, hematite = null, duiDuc = null;

            for (Item i : player.combineNew.itemsCombine) {
                if (CombineSystem.isTrangBiPhaLeHoa(i)) {
                    item = i;
                } else if (i.template.id == HEMATITE_ID) {
                    hematite = i;
                } else if (i.template.id == DUIDUC_ID) {
                    duiDuc = i;
                }
            }

            if (item != null && hematite != null && duiDuc != null
                    && hematite.quantity >= 1 && duiDuc.quantity >= 1) {

                int star = 0;
                ItemOption optionPhaLe = null;

                for (ItemOption io : item.itemOptions) {
                    if (io.optionTemplate.id == OPTION_ID_STAR) {
                        star = io.param;
                    }
                    if (io.optionTemplate.id == OPTION_ID_PHA_LE) {
                        optionPhaLe = io;
                    }
                }

                player.inventory.gold -= COST;

                if (star == CURRENT_STAR && optionPhaLe == null) {
                    item.itemOptions.add(new ItemOption(OPTION_ID_EFFECT, 0));
                    item.itemOptions.add(new ItemOption(OPTION_ID_PHA_LE, CURRENT_STAR));
                    if (Util.isTrue(SUCCESS_RATE, 100)) {
                        CombineService.gI().sendEffectSuccessCombine(player);
                    } else {
                        CombineService.gI().sendEffectFailCombine(player);
                    }
                } else if (star == TARGET_STAR && optionPhaLe != null && optionPhaLe.param == CURRENT_STAR) {
                    if (Util.isTrue(SUCCESS_RATE, 100)) {
                        optionPhaLe.param++;
                        CombineService.gI().sendEffectSuccessCombine(player);
                        Service.gI().sendThongBao(player, "Trang bị của bạn đã cường hóa thành công lên sao thứ " + TARGET_STAR + "!");
                    } else {
                        CombineService.gI().sendEffectFailCombine(player);
                    }
                } else if (optionPhaLe != null && optionPhaLe.param >= TARGET_STAR) {
                    Service.gI().sendThongBao(player, "Trang bị của bạn đã đạt tối đa, không thể cường hóa thêm.");
                    return;
                } else {
                    Service.gI().sendThongBao(player, "Cường hóa không hợp lệ, vui lòng nâng cấp trang bị lên " + TARGET_STAR + " sao.");
                    return;
                }

                InventoryService.gI().subQuantityItemsBag(player, hematite, 1);
                InventoryService.gI().subQuantityItemsBag(player, duiDuc, 1);
                Service.gI().sendMoney(player);
                InventoryService.gI().sendItemBags(player);
                CombineService.gI().reOpenItemCombine(player);
                CombineService.gI().sendEffectCombineDB(player, item.template.iconID);

            } else {
                Service.gI().sendThongBao(player, "Vật phẩm không hợp lệ hoặc không đủ số lượng.");
            }
        }
    }
}