package combine;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;
import services.Service;
import utils.Util;

public class PhanRaSach {

    private static final int COST_GOLD = 10_000_000;
    private static final short SACH_TUYET_KY_ITEM_ID = 1283;
    private static final short SACH_TUYET_KY2_ITEM_ID = 1284; 

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 1) {
            Service.gI().sendDialogMessage(player, "Không tìm thấy vật phẩm");
            return;
        }
        Item sachTuyetKy = getSachTuyetKy(player);
        if (sachTuyetKy == null) {
            Service.gI().sendDialogMessage(player, "Không tìm thấy vật phẩm");
            return;
        }
        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append("Phân rã sách\n");
        text.append(ConstFont.BOLD_BLUE).append("Nhận lại 5 cuốn sách cũ\n");
        text.append(player.inventory.gold >= COST_GOLD ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
            .append("Phí rã 10 triệu vàng");
        if (player.inventory.gold < COST_GOLD) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(),
                    "Còn thiếu\n" + Util.numberToMoney(COST_GOLD - player.inventory.gold) + " vàng");
            return;
        }
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(),
                "Đồng ý", "Từ chối");
    }

    public static void phanRaSach(Player player) {
        if (player.combineNew.itemsCombine.size() != 1) {
            return;
        }
        Item sachTuyetKy = getSachTuyetKy(player);
        if (sachTuyetKy == null || player.inventory.gold < COST_GOLD) {
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, sachTuyetKy, 1);
        player.inventory.gold -= COST_GOLD;
        Item cuonSachCu = ItemService.gI().createNewItem(SACH_TUYET_KY_ITEM_ID, 5);
        cuonSachCu.itemOptions.add(new Item.ItemOption(30, 0));
        InventoryService.gI().addItemBag(player, cuonSachCu);
        CombineService.gI().sendEffectSuccessCombine(player);
        Service.gI().sendMoney(player); 
        InventoryService.gI().sendItemBags(player);
        CombineService.gI().reOpenItemCombine(player);
    }
    private static Item getSachTuyetKy(Player player) {
        for (Item item : player.combineNew.itemsCombine) {
            if (item.template.id == SACH_TUYET_KY_ITEM_ID || item.template.id == SACH_TUYET_KY2_ITEM_ID) {
                return item;
            }
        }
        return null;
    }
}