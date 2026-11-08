package combine;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import player.Player;
import player.Service.InventoryService;
import services.Service;
import utils.Util;

public class GiamDinhSach {
    public static final int SACH_TUYET_KY_OPTION_ID = 217;
    public static final int BUA_GIAM_DINH_ID = 1284;
    public static final int[] RANDOM_OPTIONS = {77, 103, 50, 108, 94, 14, 80, 81, 175, 5, 214, 216}; 
    public static final int MAX_RANDOM_VALUE = 10;
    public static final int MIN_RANDOM_VALUE = 1;
    public static final int MAX_DIVIDER = 3;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            Service.gI().sendDialogMessage(player, "Cần Sách Tuyệt Kỹ và bùa giám định.");
            return;
        }

        Item sachTuyetKy = null;
        Item buaGiamDinh = null;

        for (Item item : player.combineNew.itemsCombine) {
            if (item.isSachTuyetKy() || item.isSachTuyetKy2()) {
                sachTuyetKy = item;
            } else if (item.template.id == BUA_GIAM_DINH_ID) {
                buaGiamDinh = item;
            }
        }

        if (sachTuyetKy == null || buaGiamDinh == null) {
            Service.gI().sendDialogMessage(player, "Cần Sách Tuyệt Kỹ và bùa giám định.");
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_GREEN)
            .append("Giám định ")
            .append(sachTuyetKy.template.name)
            .append(" ?\n")
            .append(ConstFont.BOLD_BLUE)
            .append("Bùa giám định ")
            .append(buaGiamDinh.quantity)
            .append("/1");

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(), "Giám định", "Từ chối");
    }

    public static void giamDinhSach(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            return;
        }

        Item sachTuyetKy = null;
        Item buaGiamDinh = null;

        for (Item item : player.combineNew.itemsCombine) {
            if (item.isSachTuyetKy() || item.isSachTuyetKy2()) {
                sachTuyetKy = item;
            } else if (item.template.id == BUA_GIAM_DINH_ID) {
                buaGiamDinh = item;
            }
        }

        if (sachTuyetKy == null || buaGiamDinh == null || !sachTuyetKy.isHaveOption(SACH_TUYET_KY_OPTION_ID)) {
            Service.gI().sendServerMessage(player, "Còn cái nịt mà giám");
            return;
        }

        for (int i = 0; i < sachTuyetKy.itemOptions.size(); i++) {
            Item.ItemOption io = sachTuyetKy.itemOptions.get(i);
            if (io.optionTemplate.id == SACH_TUYET_KY_OPTION_ID) {
                int randomOption = RANDOM_OPTIONS[Util.nextInt(RANDOM_OPTIONS.length)];
                int randomValue = Util.nextInt(MIN_RANDOM_VALUE, MAX_RANDOM_VALUE / Util.nextInt(1, MAX_DIVIDER));
                sachTuyetKy.itemOptions.set(i, new Item.ItemOption(randomOption, randomValue));
            }
        }

        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().subQuantityItemsBag(player, buaGiamDinh, 1);
        InventoryService.gI().sendItemBags(player);
        CombineService.gI().reOpenItemCombine(player);
    }
}