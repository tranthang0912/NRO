package npc.list;

import consts.ConstNpc;
import npc.Npc;
import player.Player;
import map.Service.ChangeMapService;

public class GokuSSJ extends Npc {

    public GokuSSJ(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        switch (mapId) {
            case 80 -> createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Ta mới hạ Fide, nhưng nó đã kịp đào 1 cái lỗ\nHành tinh này sắp nổ tung rồi\nMau lượn thôi",
                    "Chuẩn");
            case 131 -> createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Đây là đâu? Xong cmnr",
                    "Bó tay", "Về chỗ cũ");
            default -> super.openBaseMenu(player);
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (player.idMark.getIndexMenu() != ConstNpc.BASE_MENU) {
            return;
        }
        if (mapId == 80 && select == 0) {
            ChangeMapService.gI().changeMapBySpaceShip(player, 131, -1, 870);
            return;
        }
        if (mapId == 131 && select == 1) {
            ChangeMapService.gI().changeMapBySpaceShip(player, 80, -1, 870);
        }
    }
}