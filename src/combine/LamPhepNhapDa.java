package combine;

import consts.ConstNpc;
import item.Item;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;
import services.Service;
import utils.Util;

public class LamPhepNhapDa {
    private static final int GOLD_TAO_DA = 10_000_000;
    private static final int RATIO_TAO_DA = 80;
    private static final int ITEM_ID_NGOC_TRANG = 225;
    private static final int ITEM_ID_DA_TRUNG = 226; 
    private static final int REQUIRED_QUANTITY_ITEM1 = 10;
    private static final int REQUIRED_QUANTITY_ITEM2 = 1;
    private static final int RANDOM_ITEM_ID_START = 220; 
    private static final int RANDOM_ITEM_ID_END = 224;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cần đặt đúng 2 vật phẩm!", "Đóng");
            return;
        }

        Item item1 = player.combineNew.itemsCombine.get(0);
        Item item2 = player.combineNew.itemsCombine.get(1);

        if (!isValidCombination(item1, item2)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Nguyên liệu không hợp lệ!", "Đóng");
            return;
        }

        player.combineNew.goldCombine = GOLD_TAO_DA;
        player.combineNew.ratioCombine = RATIO_TAO_DA;

        String npcSay = "|2|Tỉ lệ thành CONG: " + RATIO_TAO_DA + "%\n"
                      + "|2|Cần: " + Util.numberToMoney(GOLD_TAO_DA) + " vàng\n";

        if (player.inventory.gold < GOLD_TAO_DA) {
            npcSay += "|7|Còn thiếu " + Util.powerToString(GOLD_TAO_DA - player.inventory.gold) + " vàng\n";
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    "Nâng cấp\n" + Util.numberToMoney(GOLD_TAO_DA) + " vàng\n", "Từ chối");
        }
    }

    public static void lamphepnhapda(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            Service.gI().sendThongBao(player, "Cần đặt đúng 2 vật phẩm!");
            return;
        }

        Item item1 = player.combineNew.itemsCombine.get(0);
        Item item2 = player.combineNew.itemsCombine.get(1);

        if (!isValidCombination(item1, item2)) {
            Service.gI().sendThongBao(player, "Nguyên liệu không hợp lệ!");
            return;
        }

        if (player.inventory.gold < GOLD_TAO_DA) {
            Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện!");
            return;
        }

        if (item1.quantity < REQUIRED_QUANTITY_ITEM1 || item2.quantity < REQUIRED_QUANTITY_ITEM2) {
            Service.gI().sendThongBao(player, "Không đủ nguyên liệu để thực hiện!");
            return;
        }

        player.inventory.gold -= GOLD_TAO_DA;
        InventoryService.gI().subQuantityItemsBag(player, item1, REQUIRED_QUANTITY_ITEM1);
        InventoryService.gI().subQuantityItemsBag(player, item2, REQUIRED_QUANTITY_ITEM2);

        if (Util.isTrue(RATIO_TAO_DA, 100)) {
            int randomId = Util.nextInt(RANDOM_ITEM_ID_START, RANDOM_ITEM_ID_END + 1);
            Item newItem = new Item();
            newItem.template = ItemService.gI().getTemplate(randomId);
            newItem.quantity = 1;
            InventoryService.gI().addItemBag(player, newItem);
            CombineService.gI().sendEffectSuccessCombine(player);
            Service.gI().sendThongBao(player, "Chúc mừng! Bạn đã chế tạo thành công.");
        } else {
            CombineService.gI().sendEffectFailCombine(player);
            Service.gI().sendThongBao(player, "Thất bại! Nguyên liệu đã bị mất.");
        }

        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    private static boolean isValidCombination(Item item1, Item item2) {
        return (item1.template.id == ITEM_ID_NGOC_TRANG && item2.template.id == ITEM_ID_DA_TRUNG) ||
               (item1.template.id == ITEM_ID_DA_TRUNG && item2.template.id == ITEM_ID_NGOC_TRANG);
    }
}