package npc.list;

import consts.ConstNpc;
import npc.Npc;
import player.Player;
import map.Service.ChangeMapService;

public class Jaco extends Npc {

    public Jaco(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) return;

        switch (this.mapId) {
            case 24:
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Gô Tên, Calích và Monaka đang gặp chuyện ở hành tinh\nPotaufeu\nHãy đến đó ngay", 
                        "Đến\nPotaufeu", "Từ chối");
                break;
            case 139:
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Tàu Vũ Trụ của ta có thể đưa cậu đến hành tinh khác chỉ trong 3 giây.\nCậu muốn đi đâu?", 
                        "Đến\nTrái Đất", "Đến\nNamếc", "Đến\nXayda", "Từ chối");
                break;
            default:
                break;
        }
    }
    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;

        if (player.idMark.isBaseMenu()) {
            switch (this.mapId) {
                case 24:
                    if (select == 0) {
                        ChangeMapService.gI().goToPotaufeu(player);
                    }
                    break;
                case 139:
                    handleSpaceShipMenu(player, select);
                    break;
                default:
                    break;
            }
        }
    }
    private void handleSpaceShipMenu(Player player, int select) {
        switch (select) {
            case 0:
                ChangeMapService.gI().changeMapBySpaceShip(player, 24, -1, -1);
                break;
            case 1:
                ChangeMapService.gI().changeMapBySpaceShip(player, 25, -1, -1);
                break;
            case 2:
                ChangeMapService.gI().changeMapBySpaceShip(player, 26, -1, -1);
                break;
            default:
                break;
        }
    }
}