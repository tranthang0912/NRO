package combine;

import consts.ConstNpc;
import item.Item;
import player.Player;
import services.Service;
import player.Service.InventoryService;
import utils.Util;

public class HoiPhucSach {
    public static final int MAX_DURABILITY = 1000; 
    public static final int OPTION_PARAM_ID = 212;
    public static final int MIN_GEM = 5;
    public static final int GEM_MULTIPLIER = 50;

    private static int getGem(int param) {
        int gem = (MAX_DURABILITY - param) * GEM_MULTIPLIER / MAX_DURABILITY;
        if (gem < MIN_GEM) {
            gem = MIN_GEM;
        }
        return gem;
    }

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 1) {
            Service.gI().sendDialogMessage(player, "Cần Sách Tuyệt Kỹ hỏng để phục hồi.");
            return;
        }
        Item sachTuyetKy = null;
        for (Item item : player.combineNew.itemsCombine) {
            if (item.isSachTuyetKy() || item.isSachTuyetKy2()) {
                sachTuyetKy = item;
            }
        }
        if (sachTuyetKy == null || sachTuyetKy.getOptionParam(OPTION_PARAM_ID) >= MAX_DURABILITY) {
            Service.gI().sendDialogMessage(player, "Cần Sách Tuyệt Kỹ hỏng để phục hồi.");
            return;
        }
        int doBen = sachTuyetKy.getOptionParam(OPTION_PARAM_ID);
        StringBuilder text = new StringBuilder();
        text.append("Phục hồi Sách Tuyệt Kỹ ?\n");
        text.append("1 cuốn\n");
        text.append(MAX_DURABILITY - doBen).append(" điểm độ bền cần hồi phục\n");
        text.append("Cần ").append(getGem(doBen)).append(" ngọc");
        if (player.inventory.getGem() < getGem(doBen)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(),
                    "Còn thiếu\n" + Util.numberToMoney(getGem(doBen) - player.inventory.getGem()) + " ngọc");
            return;
        }
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(),
                "Đồng ý", "Từ chối");
    }

    public static void hoiPhucSach(Player player) {
        if (player.combineNew.itemsCombine.size() != 1) {
            return;
        }
        Item sachTuyetKy = null;
        for (Item item : player.combineNew.itemsCombine) {
            if (item.isSachTuyetKy() || item.isSachTuyetKy2()) {
                sachTuyetKy = item;
            }
        }
        if (sachTuyetKy == null || sachTuyetKy.getOptionParam(OPTION_PARAM_ID) >= MAX_DURABILITY) {
            return;
        }
        int doBen = sachTuyetKy.getOptionParam(OPTION_PARAM_ID);
        if (player.inventory.getGem() < getGem(doBen)) {
            return;
        }
        player.inventory.subGem(getGem(doBen));
        for (Item.ItemOption io : sachTuyetKy.itemOptions) {
            if (io.optionTemplate.id == OPTION_PARAM_ID) {
                io.param = MAX_DURABILITY;
                break;
            }
        }
        CombineService.gI().sendEffectSuccessCombine(player);
        Service.gI().sendMoney(player);
        InventoryService.gI().sendItemBags(player);
        CombineService.gI().reOpenItemCombine(player);
    }
}