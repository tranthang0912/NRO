package combine;

import combine.CombineService;
import consts.ConstNpc;
import item.Item;
import static java.util.Collections.list;
import player.Player;
import server.Manager;
import services.ItemService;
import services.RewardService;
import services.Service;
import player.Service.InventoryService;
import utils.Util;

/**
 *
 * @author Admin
 */
public class NangCapKichHoat {

    public static boolean isDoThanLinh(Item item) {
        if (item.template.id >= 555 && item.template.id <= 567) {
            return true;
        }
        return false;
    }

    public static void showInfoCombine(Player player) {
        if (player.combineNew != null && player.combineNew.itemsCombine != null && player.combineNew.itemsCombine.size() == 1) {
            Item trangbiThanLinh = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (isDoThanLinh(item)) {
                    trangbiThanLinh= item;
                }
            }
            player.combineNew.goldCombine = 500_000_000;
            int goldCombie = player.combineNew.goldCombine;
            if (trangbiThanLinh != null) {
                String npcSay = "Sau khi cường hoá, sẽ được nâng cấp trang bị Thần linh thành trang bị Kích hoạt";
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                        "Cường hoá\n" + Util.numberToMoney(goldCombie) + " vàng", "Từ chối");
            } else {
                Service.gI().sendThongBaoOK(player, "Cần 1 trang bị thần linh");
            }
        } else {
            Service.gI().sendThongBaoOK(player, "Cần 1 trang bị Thần linh");
        }
    }

    public static void startCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            int gold = player.combineNew.goldCombine;
            if (player.inventory.gold < gold) {
                Service.gI().sendThongBao(player, "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(gold - player.inventory.gold) + " vàng nữa");
                Service.gI().sendMoney(player);
                return;
            }
            Item trangbiThanLinh = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (isDoThanLinh(item)) {
                    trangbiThanLinh = item;
                }
            }
            int gender = trangbiThanLinh.template.gender;
            int playerGender = player.gender;
            int[] maleOptions = {129, 141, 127, 139, 128, 140};
            int[] femaleOptions = {132, 144, 131, 143, 130, 142};
            int[] otherOptions = {135, 138, 133, 136, 134, 137};
            int[] selectedOptions;
            if (gender == 0 || gender == 3 && playerGender == 0) {
                selectedOptions = maleOptions;
            } else if (gender == 1 || gender == 3 && playerGender == 1) {
                selectedOptions = femaleOptions;
            } else {
                selectedOptions = otherOptions;
            }
            Item newItem = null;
            if (trangbiThanLinh.template.type == 4) {
                newItem = ItemService.gI().createNewItem((short) 12);
            } else {
                newItem = ItemService.gI().createNewItem(Manager.trangBiKichHoat[gender][trangbiThanLinh.template.type]);
            }
            RewardService.gI().initChiSoItem(newItem);
            if (Util.isTrue(15, 100)) {
                newItem.itemOptions.add(new Item.ItemOption(selectedOptions[0], 0));
                newItem.itemOptions.add(new Item.ItemOption(selectedOptions[1], 0));
            } else {
                if (Util.isTrue(75, 100)) {
                    newItem.itemOptions.add(new Item.ItemOption(selectedOptions[2], 0));
                    newItem.itemOptions.add(new Item.ItemOption(selectedOptions[3], 0));
                } else {
                    newItem.itemOptions.add(new Item.ItemOption(selectedOptions[4], 0));
                    newItem.itemOptions.add(new Item.ItemOption(selectedOptions[5], 0));
                }
            }
            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, trangbiThanLinh, 1);
            CombineService.gI().sendEffectSuccessCombine(player);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            CombineService.gI().reOpenItemCombine(player);
        }
    }
}
