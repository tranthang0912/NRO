package combine;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import player.Player;
import services.Service;
import player.Service.InventoryService;
import utils.Util;

public class TaySach {
    private static final int OPTION_PARAM_USE_ID = 219;
    private static final int OPTION_TAY_SACH = 217;
    private static final int OPTION_KY_NANG = 21;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 1) {
            Service.gI().sendDialogMessage(player, "Cần Sách Tuyệt Kỹ để tẩy.");
            return;
        }

        Item sachTuyetKy = player.combineNew.itemsCombine.get(0);
        if (sachTuyetKy == null || (!sachTuyetKy.isSachTuyetKy() && !sachTuyetKy.isSachTuyetKy2())) {
            Service.gI().sendDialogMessage(player, "Cần Sách Tuyệt Kỹ để tẩy.");
            return;
        }

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                ConstFont.BOLD_BLUE + "Tẩy Sách Tuyệt Kỹ?", "Đồng ý", "Từ chối");
    }

    public static void taySach(Player player) {
        if (player.combineNew.itemsCombine.size() != 1) {
            return;
        }

        Item sachTuyetKy = player.combineNew.itemsCombine.get(0);
        if (sachTuyetKy == null || (!sachTuyetKy.isSachTuyetKy() && !sachTuyetKy.isSachTuyetKy2())) {
            return;
        }
        if (sachTuyetKy.getOptionParam(OPTION_PARAM_USE_ID) <= 0 || sachTuyetKy.isHaveOption(OPTION_TAY_SACH)) {
            Service.gI().sendServerMessage(player, "Không thể thực hiện");
            return;
        }
        for (int i = 0; i < sachTuyetKy.itemOptions.size(); i++) {
            Item.ItemOption io = sachTuyetKy.itemOptions.get(i);
            if (io.optionTemplate.id == OPTION_KY_NANG) {
                break;
            }
            sachTuyetKy.itemOptions.set(i, new Item.ItemOption(OPTION_TAY_SACH, 0));
        }

        sachTuyetKy.subOptionParam(OPTION_PARAM_USE_ID, 1);

        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBags(player);
        CombineService.gI().reOpenItemCombine(player);
    }
}