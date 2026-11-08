package npc.list;

import consts.ConstNpc;
import item.Item;
import item.Item.ItemOption;
import npc.Npc;
import player.Player;
import services.ItemService;
import services.Service;
import shop.ShopService;
import utils.Util;
import player.Service.InventoryService;

import java.util.ArrayList;

public class DaiThienSu extends Npc {

    public DaiThienSu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) return;

        createOtherMenu(player, ConstNpc.BASE_MENU,
                "Này nhóc\r\n" + //
                                        "Biết tính đạo hàm tích phân rồi\r\n" + //
                                        "Thì biết tính cho hàm ẩn, hàm logalepe, hàm lượng giác chưa\r\n" + //                      
                                        "Biết đếm vân sáng , vân tối\r\n" + //
                                        "Trong giao Thoa ánh sáng k ?\r\n" + //
                                        "Cho mấy chục viên bi xanh vàng đoả\r\n" + //
                                        "Bốc bừa 3 viên thì có biết tính xác suất k ?\r\n" + //
                                        "Bt khi nào thì công suất trên quận dây L\r\n" + //
                                        "Cực đại trong mạch RLC k ?\r\n" + //
                                        "Cho đậu xanh , thân ngắn hạt nhăn \r\n" + //
                                        "Lai vs đậu vàng ,thân dài , hạt trơn \r\n" + //
                                        "Thì có bt tính sát suất đời con k",
                "Shop\nSự Kiện",
                "Đổi\nCải Trang",
                "Đổi\nĐồ Vip");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;

        // ===== MENU CHÍNH =====
        if (player.idMark.isBaseMenu()) {
            switch (select) {
                case 0 ->
                        ShopService.gI().opendShop(player, "DOI_SKILL_DE", false);

                case 1 ->
                        createOtherMenu(player, 1,
                                "Dưới đây là những cải trang cực VIP\nvui lòng đổi lấy mà xài\nLưu ý quan trọng: Do đây là cải trang Vip\nNên phải vất ra đất mới có thể xài\nKhông tin thì thử là biết.",
                                "Thỏ\nĐầu Bạc\n10 Carot",
                                "Bulma Sexy\n99 Carot",
                                "Đóng");

                case 2 ->
                        createOtherMenu(player, 2,
                                "Chọn hành tinh đi mày\n20 củ Carot sẽ đổi được 1 đồ\nNếu may mắn, ngươi có thể ...xem thêm",
                                "Xayda",
                                "Namek",
                                "Trái đất",
                                "Đóng");
            }
            return;
        }

        // ===== ĐỔI CẢI TRANG =====
        if (player.idMark.getIndexMenu() == 1) {
            if (select == 2) return;
            if (select == 0) doiCaiTrang(player, 463, 10);
            if (select == 1) doiCaiTrang(player, 464, 99);
            return;
        }

        // ===== CHỌN HÀNH TINH =====
        if (player.idMark.getIndexMenu() == 2) {
            if (select == 3) return;

            switch (select) {
                case 0 ->
                        createOtherMenu(player, 21,
                                "Xayda phải chơi sức đánh\nĐó mới là đẳng cấp",
                                "Quần Si",
                                "Găng Líp",
                                "Đóng");

                case 1 ->
                        createOtherMenu(player, 22,
                                "Namek có thể chơi HP\nNhưng bạn làm đíu gì có đẳng cấp đó",
                                "Giày Rách",
                                "Găng Vip",
                                "Đóng");

                case 2 ->
                        createOtherMenu(player, 23,
                                "Trái Đất phải chơi Gay\nVì trên Trái Đất có nhiều Gay Nhất",
                                "Quần Lọt",
                                "Găng Khe",
                                "Đóng");
            }
            return;
        }

        // ===== XAYDA =====
        if (player.idMark.getIndexMenu() == 21) {
            if (select == 2) return;
            if (select == 0) doiDo(player, 253); // Quần
            if (select == 1) doiDo(player, 265); // Găng
            return;
        }

        // ===== NAMEK =====
        if (player.idMark.getIndexMenu() == 22) {
            if (select == 2) return;
            if (select == 0) doiDo(player, 261); // Giày
            if (select == 1) doiDo(player, 273); // Găng
            return;
        }

        // ===== TRÁI ĐẤT =====
        if (player.idMark.getIndexMenu() == 23) {
            if (select == 2) return;
            if (select == 0) doiDo(player, 245); // Quần
            if (select == 1) doiDo(player, 257); // Găng
        }
    }

    // ================== XỬ LÝ ==================

  private void doiDo(Player player, int itemId) {

    if (getItemQuantity(player, 462) < 20) {
        Service.gI().sendThongBao(player, "Không đủ Carot!");
        return;
    }

    if (InventoryService.gI().getCountEmptyBag(player) == 0) {
        Service.gI().sendThongBao(player, "Hành trang đã đầy!");
        return;
    }

    subItem(player, 462, 20);

    Item item = ItemService.gI().createNewItem((short) itemId);
    item.itemOptions = new ArrayList<>();

    // ===== OPTION CHÍNH THEO ITEM ID =====

    // QUẦN
    if (itemId == 253 || itemId == 245) {
        item.itemOptions.add(
                new ItemOption(6, Util.nextInt(23000, 26000))
        );
    }

    // GĂNG / ĐỒ ĐÁNH
    else if (itemId == 261 || itemId == 257 || itemId == 265) {
        item.itemOptions.add(
                new ItemOption(0, Util.nextInt(2100, 2500))
        );
    }

    // GIÀY NAMEK
    else if (itemId == 273) {
        item.itemOptions.add(
                new ItemOption(7, Util.nextInt(25000, 28000))
        );
    }

    InventoryService.gI().addItemBag(player, item);
    InventoryService.gI().sendItemBags(player);
    Service.gI().player(player);
    Service.gI().sendThongBao(player, "Bạn nhận được " + item.template.name);
}

    private void doiCaiTrang(Player player, int itemId, int carotNeed) {

        if (getItemQuantity(player, 462) < carotNeed) {
            Service.gI().sendThongBao(player, "Không đủ Carot!");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy!");
            return;
        }

        subItem(player, 462, carotNeed);

        Item item = ItemService.gI().createNewItem((short) itemId);
        item.itemOptions = new ArrayList<>();

        if (itemId == 463) {
            item.itemOptions.add(new ItemOption(50, 10));
            item.itemOptions.add(new ItemOption(77, 10));
            item.itemOptions.add(new ItemOption(103, 10));
            item.itemOptions.add(new ItemOption(101, 20));
            item.itemOptions.add(new ItemOption(116, 1));
        } else {
            item.itemOptions.add(new ItemOption(50, Util.nextInt(1, 24)));
            item.itemOptions.add(new ItemOption(77, Util.nextInt(1, 24)));
            item.itemOptions.add(new ItemOption(103, Util.nextInt(1, 24)));
            item.itemOptions.add(new ItemOption(117, Util.nextInt(1, 15)));
        }

        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBags(player);
        Service.gI().player(player);
        Service.gI().sendThongBao(player, "Đổi cải trang thành công!");
    }

    // ================== INVENTORY ==================

    private int getItemQuantity(Player player, int itemId) {
        int count = 0;
        for (Item item : player.inventory.itemsBag) {
            if (item != null && item.template != null && item.template.id == itemId) {
                count += item.quantity;
            }
        }
        return count;
    }

    private void subItem(Player player, int itemId, int quantity) {
        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
            Item item = player.inventory.itemsBag.get(i);
            if (item == null || item.template == null) continue;

            if (item.template.id == itemId) {
                int sub = Math.min(item.quantity, quantity);
                item.quantity -= sub;
                quantity -= sub;

                if (item.quantity <= 0) {
                    player.inventory.itemsBag.set(i, null);
                }
                if (quantity <= 0) break;
            }
        }
        InventoryService.gI().sendItemBags(player);
    }
}
