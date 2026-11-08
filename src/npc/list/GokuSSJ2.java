package npc.list;

import consts.ConstNpc;
import item.Item;
import npc.Npc;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;
import services.Service;

public class GokuSSJ2 extends Npc {

    public GokuSSJ2(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.BASE_MENU, 
                    "Hãy cố gắng luyện tập\nThu thập 9.999 bí kiếp để đổi trang phục Yardrat nhé!",
                    "Nhận\nthưởng", "OK");
        }
    }
    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (select == 0) {
            int soluong = InventoryService.gI().getParam(player, 31, 590);
            if (soluong >= 9999) {
                InventoryService.gI().subParamItemsBag(player, 590, 31, 9999);
                Item yardrat = ItemService.gI().createNewItem((short) (player.gender + 592));
                yardrat.itemOptions.add(new Item.ItemOption(47, 400));
                yardrat.itemOptions.add(new Item.ItemOption(97, 10));
                yardrat.itemOptions.add(new Item.ItemOption(14, 15));
                yardrat.itemOptions.add(new Item.ItemOption(147, 30)); 
                yardrat.itemOptions.add(new Item.ItemOption(108, 10));
                InventoryService.gI().addItemBag(player, yardrat);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "Bạn nhận được võ phục của người Yardrat");
            } else {
                Service.gI().sendThongBao(player, "Bạn không đủ bí kiếp!");
            }
        }
    }
}