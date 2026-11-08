package npc.list;

import boss.BossManager.BossManager;
import consts.ConstNpc;
import npc.Npc;
import player.Player;
import map.Service.NpcService;
import services.Service;
import utils.Util;

public class Potage extends Npc {

    public Potage(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player) && this.mapId == 140) {
            Player bossClone = BossManager.gI().findBossClone(player);
            if (bossClone != null) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Đang có 1 nhân bản của " + bossClone.name + " hãy chờ kết quả trận đấu", "OK");
            } else {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Hãy giúp ta đánh bại bản sao\nNgươi chỉ có 5 phút để hạ hắn\nPhần thưởng cho ngươi là 1 bình Commeson",
                        "Hướng\ndẫn\nthêm", "OK", "Từ chối");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player) || this.mapId != 140 || !player.idMark.isBaseMenu()) return;

        if (BossManager.gI().findBossClone(player) != null) return;

        switch (select) {
            case 0:
                NpcService.gI().createTutorial(player, tempId, this.avartar,
                        "Thứ bị phong ấn tại đây là vũ khí có tên Commeson\n" +
                        "được tạo ra nhằm bảo vệ cho hành tinh Potaufeu\n" +
                        "Tuy nhiên nó đã tàn phá mọi thứ trong quá khứ\n" +
                        "Khiến cư dân Potaufeu niêm phong nó với cái giá\n" +
                        "phải trả là mạng sống của họ\n" +
                        "Ta, Potage là người duy nhất sống sót\n" +
                        "và ta đã bảo vệ phong ấn hơn một trăm năm.\n" +
                        "Tuy nhiên bọn xâm lược Gryll đã đến và giải thoát Commeson\n" +
                        "Hãy giúp ta tiêu diệt bản sao do Commeson tạo ra\n" +
                        "và niêm phong Commeson một lần và mãi mãi");
                break;
            case 1:
                if (!Util.isAfterMidnight(player.lastPkCommesonTime) && !player.isAdmin()) {
                    Service.gI().sendThongBao(player, "Hãy chờ đến ngày mai");
                } else {
                    Service.gI().callNhanBan(player);
                }
                break;
        }
    }
}