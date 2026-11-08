package combine;

import consts.ConstNpc;
import item.Item;
import item.Item.ItemOption;
import java.util.ArrayList;
import java.util.Arrays;
import player.Player;
import services.ItemService;
import services.Service;
import player.Service.InventoryService;
import utils.Util;

public class CheTaoTrangBiThienSu {
    private static final int REQUIRED_ITEMS_COUNT = 4;
    private static final int REQUIRED_GOLD_CHE_TAO = 10_000_000;
    private static final int REQUIRED_GOLD_CHE_TAO_TS = 500_000_000;
    private static final int REQUIRED_MANH_TS_QUANTITY = 999;
    private static final int MIN_EMPTY_BAG_SLOTS = 1;
    private static final int BASE_SUCCESS_RATE = 90;
    private static final int BASE_LUCKY_RATE = 5;

    private static final short[][] ITEM_IDS = {
        {1048, 1051, 1054, 1057, 1060},
        {1049, 1052, 1055, 1058, 1061},
        {1050, 1053, 1056, 1059, 1062}
    };

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != REQUIRED_ITEMS_COUNT) {
            Service.gI().sendThongBao(player, "Thiếu vật phẩm, vui lòng thêm vào");
            return;
        }

        if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isCongThucVip()).count() != 1) {
            Service.gI().sendThongBao(player, "Thiếu Công Thức Vip");
            return;
        }

        if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isManhTS() && item.quantity >= REQUIRED_MANH_TS_QUANTITY).count() != 1) {
            Service.gI().sendThongBao(player, "Thiếu Mảnh Thiên Sứ");
            return;
        }

        if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isDaNangCap1()).count() != 1) {
            Service.gI().sendThongBao(player, "Thiếu Đá Nâng Cấp");
            return;
        }

        if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isDaMayMan()).count() != 1) {
            Service.gI().sendThongBao(player, "Thiếu Đá May Mắn");
            return;
        }

        Item mTS = null, daNC = null, daMM = null, CtVip = null;
        for (Item item : player.combineNew.itemsCombine) {
            if (item.isNotNullItem()) {
                if (item.isManhTS()) mTS = item;
                else if (item.isDaNangCap1()) daNC = item;
                else if (item.isDaMayMan()) daMM = item;
                else if (item.isCongThucVip()) CtVip = item;
            }
        }

        if (InventoryService.gI().getCountEmptyBag(player) < MIN_EMPTY_BAG_SLOTS) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }

        if (player.inventory.gold < REQUIRED_GOLD_CHE_TAO) {
            Service.gI().sendThongBao(player, "Không đủ vàng để thực hiện");
            return;
        }

        player.inventory.gold -= REQUIRED_GOLD_CHE_TAO;

        int tilemacdinh = BASE_SUCCESS_RATE;
        int tileLucky = BASE_LUCKY_RATE;

        if (daNC != null) tilemacdinh += (daNC.template.id - 1073);
        if (daMM != null) tileLucky += tileLucky * (daMM.template.id - 1078);

        if (Util.nextInt(0, 100) < tilemacdinh) {
            Item itemTS = ItemService.gI().DoThienSu(
                ITEM_IDS[CtVip.template.gender > 2 ? player.gender : CtVip.template.gender][mTS.typeIdManh()],
                CtVip.template.gender
            );

            for (ItemOption option : itemTS.itemOptions) {
                if (option.optionTemplate.id != 0 && option.optionTemplate.id != 20) {
                    option.param += (option.param * tilemacdinh / 100);
                }
            }

            int roll = Util.nextInt(0, 50);
            if (roll <= tileLucky) {
                tileLucky = roll >= (tileLucky - 3) ? 3 :
                            (roll <= (tileLucky - 4) && roll >= (tileLucky - 10)) ? 2 : 1;

                itemTS.itemOptions.add(new ItemOption(15, tileLucky));

                ArrayList<Integer> bonusOptions = new ArrayList<>(Arrays.asList(50, 77, 103, 94, 5));
                for (int j = 0; j < tileLucky; j++) {
                    int index = Util.nextInt(0, bonusOptions.size() - 1);
                    itemTS.itemOptions.add(new ItemOption(bonusOptions.get(index), Util.nextInt(1, 3)));
                    bonusOptions.remove(index);
                }
            }

            InventoryService.gI().addItemBag(player, itemTS);
            CombineService.gI().sendEffectSuccessCombine(player);
        } else {
            CombineService.gI().sendEffectFailCombine(player);
        }

        InventoryService.gI().subQuantityItemsBag(player, CtVip, 1);
        InventoryService.gI().subQuantityItemsBag(player, daNC, 1);
        InventoryService.gI().subQuantityItemsBag(player, mTS, REQUIRED_MANH_TS_QUANTITY);
        InventoryService.gI().subQuantityItemsBag(player, daMM, 1);

        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

    public static void CheTaoTS(Player player) {
        if (player.combineNew.itemsCombine.size() != REQUIRED_ITEMS_COUNT) {
            Service.gI().sendThongBao(player, "Thiếu đồ");
            return;
        }

        if (player.inventory.gold < REQUIRED_GOLD_CHE_TAO_TS) {
            Service.gI().sendThongBao(player, "Ảo ít thôi con...");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < MIN_EMPTY_BAG_SLOTS) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }

        Item itemTL = player.combineNew.itemsCombine.stream()
                .filter(item -> item.isNotNullItem() && item.isDHD())
                .findFirst().get();

        Item itemManh = player.combineNew.itemsCombine.stream()
                .filter(item -> item.isNotNullItem() && item.isManhTS() && item.quantity >= 5)
                .findFirst().get();

        player.inventory.gold -= REQUIRED_GOLD_CHE_TAO_TS;
        CombineService.gI().sendEffectSuccessCombine(player);

        Item itemTS = ItemService.gI().DoThienSu(
                ITEM_IDS[itemTL.template.gender > 2 ? player.gender : itemTL.template.gender][itemManh.typeIdManh()],
                itemTL.template.gender
        );

        InventoryService.gI().addItemBag(player, itemTS);
        InventoryService.gI().subQuantityItemsBag(player, itemTL, 1);
        InventoryService.gI().subQuantityItemsBag(player, itemManh, 99);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player, "Bạn đã nhận được " + itemTS.template.name);
        player.combineNew.itemsCombine.clear();
        CombineService.gI().reOpenItemCombine(player);
    }
}