package boss.Earth;


import boss.Boss;
import boss.BossID;
import consts.BossStatus;
import boss.BossesData;
import item.Item;
import java.util.List;
import map.ItemMap;
import player.Player;
import services.ItemService;
import services.Service;
import utils.Util;

public class BOJACK extends Boss {

    private long st;

    public BOJACK() throws Exception {
        super(BossID.BOJACK, false, true, BossesData.BOJACK, BossesData.SUPER_BOJACK);
    }

   @Override
public void reward(Player plKill) {
    if (this.zone == null || plKill == null) return;

    int baseX = this.location.x;
    int baseY = this.zone.map.yPhysicInTop(baseX, this.location.y - 24);

    // RƠI 10 ITEM ID 77 RẢI ĐỀU XUNG QUANH
    int soLuong = 10;
    int khoangCach = 20; // khoảng cách giữa các item

    for (int i = 0; i < soLuong; i++) {
        int x = baseX - (soLuong / 2 * khoangCach) + i * khoangCach;
        ItemMap it77 = new ItemMap(
                zone,
                77,
                1,
                x,
                baseY,
                plKill.id
        );
        Service.gI().dropItemMap(this.zone, it77);
    }

    // RƠI 1 ITEM ID 188 GIÁ TRỊ 20.000
    ItemMap it188 = new ItemMap(
            zone,
            188,
            20000,
            baseX + Util.nextInt(-30, 30),
            baseY,
            plKill.id
    );
    Service.gI().dropItemMap(this.zone, it188);
}

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 90000000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    @Override
    public void doneChatS() {
        if (this.currentLevel == 1) {
            return;
        }
        this.changeStatus(BossStatus.AFK);
    }
}
