package combine;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import player.Player;
import services.ItemService;
import services.Service;
import player.Service.InventoryService;
import utils.Util;

public class DoiSachTuyetKy {
    private static final int REQUIRED_CUON_SACH_CU = 10;
    private static final int REQUIRED_KIM_BAM_GIAY = 1;
    private static final int SUCCESS_RATE_PERCENT = 20;
    private static final int[] SACH_TUYET_KY_IDS = {1044, 1211, 1212};
    private static final int CUON_SACH_CU_ID = 1283;
    private static final int KIM_BAM_GIAY_ID = 1285; 
    private static final String SUCCESS_MESSAGE = "Bạn nhận được %s";
    private static final String FAILURE_MESSAGE = "Chúc con may mắn lần sau, đừng buồn con nhé";

    public static void showCombine(Player player) {
        // Lấy số lượng các item cần thiết
        Item cuonSachCu = InventoryService.gI().findItemBag(player, CUON_SACH_CU_ID);
        Item kimBamGiay = InventoryService.gI().findItemBag(player, KIM_BAM_GIAY_ID);
        int quantityCuonSachCu = (cuonSachCu != null) ? cuonSachCu.quantity : 0;
        int quantityKimBamGiay = (kimBamGiay != null) ? kimBamGiay.quantity : 0;

        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_GREEN).append("Đổi sách Tuyệt Kỹ 1\n")
            .append(formatRequirement("Cuốn sách cũ", quantityCuonSachCu, REQUIRED_CUON_SACH_CU))
            .append(formatRequirement("Kìm bấm giấy", quantityKimBamGiay, REQUIRED_KIM_BAM_GIAY))
            .append((quantityCuonSachCu >= REQUIRED_CUON_SACH_CU && quantityKimBamGiay >= REQUIRED_KIM_BAM_GIAY)
                    ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
            .append("Tỉ lệ thành công: ").append(SUCCESS_RATE_PERCENT).append("%\n");
        if (quantityCuonSachCu < REQUIRED_CUON_SACH_CU || quantityKimBamGiay < REQUIRED_KIM_BAM_GIAY) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Từ chối");
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.DOI_SACH_TUYET_KY, text.toString(), "Đồng ý", "Từ chối");
        }
    }

    public static void doiSachTuyetKy(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Cần 1 ô trống trong hành trang.");
            return;
        }

        Item cuonSachCu = InventoryService.gI().findItemBag(player, CUON_SACH_CU_ID);
        Item kimBamGiay = InventoryService.gI().findItemBag(player, KIM_BAM_GIAY_ID);

        if (cuonSachCu == null || cuonSachCu.quantity < REQUIRED_CUON_SACH_CU ||
            kimBamGiay == null || kimBamGiay.quantity < REQUIRED_KIM_BAM_GIAY) {
            return;
        }

        CombineService.gI().sendAddItemCombine(player, ConstNpc.BA_HAT_MIT, cuonSachCu, kimBamGiay);

        if (Util.isTrue(SUCCESS_RATE_PERCENT, 100)) {
            processSuccess(player, cuonSachCu, kimBamGiay);
        } else {
            processFailure(player, cuonSachCu, kimBamGiay);
        }
    }

    private static String formatRequirement(String itemName, int currentQuantity, int requiredQuantity) {
        return (currentQuantity >= requiredQuantity ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED) +
               itemName + " " + currentQuantity + "/" + requiredQuantity + "\n";
    }

    private static void processSuccess(Player player, Item cuonSachCu, Item kimBamGiay) {
        InventoryService.gI().subQuantityItemsBag(player, cuonSachCu, REQUIRED_CUON_SACH_CU);
        InventoryService.gI().subQuantityItemsBag(player, kimBamGiay, REQUIRED_KIM_BAM_GIAY);

        Item sachTuyetKy = createRandomSachTuyetKy();
        InventoryService.gI().addItemBag(player, sachTuyetKy);
        CombineService.gI().sendEffSuccessVip(player, sachTuyetKy.template.iconID);

        Util.setTimeout(() -> {
            Service.gI().sendServerMessage(player, String.format(SUCCESS_MESSAGE, sachTuyetKy.template.name));
            CombineService.gI().baHatMit.npcChat(player, "Chúc mừng con nhé");
        }, 2000);
    }

    private static void processFailure(Player player, Item cuonSachCu, Item kimBamGiay) {
        InventoryService.gI().subQuantityItemsBag(player, cuonSachCu, REQUIRED_CUON_SACH_CU / 2);
        InventoryService.gI().subQuantityItemsBag(player, kimBamGiay, REQUIRED_KIM_BAM_GIAY);

        CombineService.gI().sendEffFailVip(player);

        Util.setTimeout(() -> {
            CombineService.gI().baHatMit.npcChat(player, FAILURE_MESSAGE);
        }, 2000);
    }

    private static Item createRandomSachTuyetKy() {
        int randomIndex = Util.nextInt(SACH_TUYET_KY_IDS.length);
        Item sachTuyetKy = ItemService.gI().createNewItem((short) SACH_TUYET_KY_IDS[randomIndex]);

        int optionCount = Util.isTrue(999, 1000) ? 1 : Util.nextInt(1, 3);
        for (int i = 0; i < optionCount; i++) {
            sachTuyetKy.itemOptions.add(new Item.ItemOption(217, 0));
        }
        sachTuyetKy.itemOptions.add(new Item.ItemOption(21, 40));
        sachTuyetKy.itemOptions.add(new Item.ItemOption(30, 0));
        sachTuyetKy.itemOptions.add(new Item.ItemOption(87, 0));
        sachTuyetKy.itemOptions.add(new Item.ItemOption(219, 5));
        sachTuyetKy.itemOptions.add(new Item.ItemOption(212, 1000));

        return sachTuyetKy;
    }
}