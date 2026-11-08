package boss.NamekGinyuForce;


import boss.Boss;
import boss.BossID;
import consts.BossStatus;
import java.util.List;
import boss.BossesData;
import item.Item;
import map.ItemMap;
import player.Player;
import services.ItemService;
import services.Service;
import utils.Util;

public class SO2_NM extends Boss {

    private long st;

    public SO2_NM() throws Exception {
        super(BossID.SO_2_NM, false, true, BossesData.SO_2_NM);
    }

    @Override
    public void moveTo(int x, int y) {
        if (this.currentLevel == 1) {
            return;
        }
        super.moveTo(x, y);
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
    protected void notifyJoinMap() {
        if (this.currentLevel == 1) {
            return;
        }
        super.notifyJoinMap();
    }

    @Override
    public void doneChatS() {
        this.changeStatus(BossStatus.AFK);
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
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void doneChatE() {
        if (this.parentBoss == null || this.parentBoss.bossAppearTogether == null
                || this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel]) {
            if (boss.id == BossID.SO_1_NM && !boss.isDie()) {
                return;
            }
        }
        this.parentBoss.changeStatus(BossStatus.ACTIVE);
    }

}
