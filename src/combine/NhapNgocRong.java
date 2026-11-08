package combine;

import consts.ConstNpc;
import item.Item;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;

public class NhapNgocRong {
    private static final int MIN_ITEM_ID = 15; 
    private static final int MAX_ITEM_ID = 20; 
    private static final int REQUIRED_QUANTITY = 7;

    public static void showInfoCombine(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            if (player.combineNew.itemsCombine.size() == 1) {
                Item item = player.combineNew.itemsCombine.get(0);
                if (isValidItemForCombine(item)) {
                    String npcSay = "|2|Con có muốn biến 7 " + item.template.name + " thành\n"
                            + "1 viên " + ItemService.gI().getTemplate((short) (item.template.id - 1)).name + "\n"
                            + "|7|Cần 7 " + item.template.name;
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay, "Làm phép", "Từ chối");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cần 7 viên ngọc rồng 2 sao trở lên", "Đóng");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cần 7 viên ngọc rồng 2 sao trở lên", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Hành trang cần ít nhất 1 chỗ trống", "Đóng");
        }
    }

    public static void nhapNgocRong(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0 && !player.combineNew.itemsCombine.isEmpty()) {
            Item item = player.combineNew.itemsCombine.get(0);
            if (isValidItemForCombine(item)) {
                CombineService.gI().sendEffectCombineDB(player, item.template.iconID);
                Item nr = ItemService.gI().createNewItem((short) (item.template.id - 1));
                InventoryService.gI().addItemBag(player, nr);
                InventoryService.gI().subQuantityItemsBag(player, item, REQUIRED_QUANTITY);
                InventoryService.gI().sendItemBags(player);
                CombineService.gI().reOpenItemCombine(player);
            }
        }
    }
    private static boolean isValidItemForCombine(Item item) {
        return item != null && item.isNotNullItem() && item.template.id >= MIN_ITEM_ID && item.template.id <= MAX_ITEM_ID && item.quantity >= REQUIRED_QUANTITY;
    }
}