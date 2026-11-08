package combine;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import player.Player;
import services.ItemService;
import services.Service;
import player.Service.InventoryService;
import utils.Util;

public class NangCapSachTuyetKy {

    private static final int KIM_BAM_GIAY_ID = 1285;
    private static final int REQUIRED_KIM_BAM_GIAY_QUANTITY = 10; 
    private static final int SUCCESS_RATE_PERCENT = 10;

    public static void showInfoCombine(Player player) {
        if (!hasRequiredItems(player)) {
            Service.gI().sendDialogMessage(player, "Cần Sách Tuyệt Kỹ 1 và 10 Kìm bấm giấy.");
            return;
        }

        Item sachTuyetKy = null;
        Item kimBamGiay = null;

        for (Item item : player.combineNew.itemsCombine) {
            if (item.isSachTuyetKy()) {
                sachTuyetKy = item;
            } else if (item.template.id == KIM_BAM_GIAY_ID) {
                kimBamGiay = item;
            }
        }

        if (sachTuyetKy == null || kimBamGiay == null) {
            Service.gI().sendDialogMessage(player, "Cần Sách Tuyệt Kỹ 1 và 10 Kìm bấm giấy.");
            return;
        }

        String statusColor = kimBamGiay.quantity >= REQUIRED_KIM_BAM_GIAY_QUANTITY ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED;
        String message = String.format(
            "%sNâng cấp sách tuyệt kỹ\n%sCần 10 Kìm bấm giấy\n%sTỉ lệ thành công: %d%%\n%sNâng cấp thất bại sẽ mất 10 Kìm bấm giấy",
            ConstFont.BOLD_BLUE, statusColor, ConstFont.BOLD_BLUE, SUCCESS_RATE_PERCENT, ConstFont.BOLD_BLUE
        );

        if (kimBamGiay.quantity < REQUIRED_KIM_BAM_GIAY_QUANTITY) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, message,
                    String.format("Còn thiếu\n%d Kìm bấm giấy", REQUIRED_KIM_BAM_GIAY_QUANTITY - kimBamGiay.quantity));
            return;
        }

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, message,
                "Nâng cấp", "Từ chối");
    }

    public static void nangCapSachTuyetKy(Player player) {
        if (!hasRequiredItems(player)) {
            return;
        }

        Item sachTuyetKy = null;
        Item kimBamGiay = null;

        for (Item item : player.combineNew.itemsCombine) {
            if (item.isSachTuyetKy()) {
                sachTuyetKy = item;
            } else if (item.template.id == KIM_BAM_GIAY_ID) {
                kimBamGiay = item;
            }
        }

        if (sachTuyetKy == null || kimBamGiay == null) {
            return;
        }
        if (Util.isTrue(SUCCESS_RATE_PERCENT, 100)) {
            switch (sachTuyetKy.template.id) {
                case 1044 -> sachTuyetKy.template = ItemService.gI().getTemplate(1278);
                case 1211 -> sachTuyetKy.template = ItemService.gI().getTemplate(1279);
                case 1212 -> sachTuyetKy.template = ItemService.gI().getTemplate(1280);
            }
            CombineService.gI().sendEffectSuccessCombine(player);
        } else {
            CombineService.gI().sendEffectFailCombine(player);
        }
        InventoryService.gI().subQuantityItemsBag(player, kimBamGiay, REQUIRED_KIM_BAM_GIAY_QUANTITY);
        InventoryService.gI().sendItemBags(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    private static boolean hasRequiredItems(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            return false;
        }

        boolean hasSachTuyetKy = false;
        boolean hasKimBamGiay = false;

        for (Item item : player.combineNew.itemsCombine) {
            if (item.isSachTuyetKy()) {
                hasSachTuyetKy = true;
            } else if (item.template.id == KIM_BAM_GIAY_ID && item.quantity >= REQUIRED_KIM_BAM_GIAY_QUANTITY) {
                hasKimBamGiay = true;
            }
        }

        return hasSachTuyetKy && hasKimBamGiay;
    }
}