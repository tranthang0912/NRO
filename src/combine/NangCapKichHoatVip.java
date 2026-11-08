package combine;

import combine.CombineService;
import consts.ConstNpc;
import item.Item;
import java.util.List;
import java.util.stream.Collectors;
import player.Player;
import server.Manager;
import services.ItemService;
import services.RewardService;
import services.Service;
import player.Service.InventoryService;
import utils.Util;

public class NangCapKichHoatVip {

    private static final int COST = 500_000_000;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 0) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Hãy đưa ta 1 món Hủy Diệt, 2 món thần linh ngẫu nhiên", "Đóng");
            return;
        }
        if (player.combineNew.itemsCombine.size() == 3) {
            if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isDHD()).count() < 1) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Thiếu đồ Hủy Diệt", "Đóng");
                return;
            }
            if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isDTL()).count() < 2) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Thiếu đồ kích hoạt ", "Đóng");
                return;
            }
            String npcSay = "|2|Con có muốn đổi các món nguyên liệu ?\n|7|"
                    + "Và nhận được " + player.combineNew.itemsCombine.stream().filter(Item::isDHD).findFirst().get().typeName()
                    + " kích hoạt VIP tương ứng\n"
                    + "|1|Cần " + Util.numberToMoney(COST) + " vàng";

            if (player.inventory.gold < COST) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Hết tiền rồi\nẢo ít thôi con", "Đóng");
                return;
            }
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    npcSay, "Nâng cấp\n" + Util.numberToMoney(COST) + " vàng", "Từ chối");
        } else {
            if (player.combineNew.itemsCombine.size() > 3) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Nguyên liệu không phù hợp", "Đóng");
                return;
            }
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Còn thiếu nguyên liệu để nâng cấp hãy quay lại sau", "Đóng");
        }
    }

    public static void startCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 3) {
            Service.gI().sendThongBao(player, "Thiếu nguyên liệu");
            return;
        }
        if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isDHD()).count() != 1) {
            Service.gI().sendThongBao(player, "Thiếu đồ Hủy Diệt");
            return;
        }
        if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isDTL()).count() != 2) {
            Service.gI().sendThongBao(player, "Thiếu đồ Thần Linh");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }
        if (player.inventory.gold < COST) {
            Service.gI().sendThongBao(player, "Con cần thêm vàng để đổi...");
            return;
        }

        player.inventory.gold -= COST;
        Item itemHD = player.combineNew.itemsCombine.stream().filter(Item::isDHD).findFirst().get();
        List<Item> itemDTL = player.combineNew.itemsCombine.stream()
                .filter(item -> item.isNotNullItem() && item.isDTL())
                .collect(Collectors.toList());
        
        CombineService.gI().sendEffectOpenItem(player, itemHD.template.iconID, itemHD.template.iconID);

        short itemId;
        if (itemHD.template.gender == 3 || itemHD.template.type == 4) {
            itemId = Manager.radaSKHVip[Util.nextInt(0, 5)];
            if (player.getSession().bdPlayer > 0 && Util.isTrue(1, (int) (100 / player.getSession().bdPlayer))) {
                itemId = Manager.radaSKHVip[6];
            }
        } else {
            itemId = Manager.doSKHVip[itemHD.template.gender][itemHD.template.type][Util.nextInt(0, 5)];
            if (player.getSession().bdPlayer > 0 && Util.isTrue(1, (int) (100 / player.getSession().bdPlayer))) {
                itemId = Manager.doSKHVip[itemHD.template.gender][itemHD.template.type][6];
            }
        }

        int skhId = ItemService.gI().randomSKHId(player.gender);
        Item item;
        if (new Item(itemId).isDTL()) {
            item = Util.ratiItemTL(itemId);
            item.itemOptions.add(new Item.ItemOption(skhId, 1));
            item.itemOptions.add(new Item.ItemOption(ItemService.gI().optionIdSKH(skhId), 1));
            item.itemOptions.remove(item.itemOptions.stream()
                    .filter(itemOption -> itemOption.optionTemplate.id == 21)
                    .findFirst().get());
            item.itemOptions.add(new Item.ItemOption(21, 15));
            item.itemOptions.add(new Item.ItemOption(30, 1));
        } else {
            item = ItemService.gI().itemSKH(itemId, skhId);
        }

        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().subQuantityItemsBag(player, itemHD, 1);
        itemDTL.forEach(i -> InventoryService.gI().subQuantityItemsBag(player, i, 2));
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        player.combineNew.itemsCombine.clear();
        CombineService.gI().reOpenItemCombine(player);
    }
}