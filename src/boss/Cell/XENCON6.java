package boss.Cell;


import boss.Boss;
import boss.BossID;
import consts.BossStatus;
import boss.BossesData;
import java.util.Random;
import map.ItemMap;
import player.Player;
import services.Service;
import services.TaskService;
import map.Service.ChangeMapService;
import utils.Util;

public class XENCON6 extends Boss {

    private long st;

    public XENCON6() throws Exception {
        super(BossID.XEN_CON_6, BossesData.XEN_CON_6);
    }

    @Override
public void reward(Player plKill) {
    TaskService.gI().checkDoneTaskKillBoss(plKill, this);

    // Thả vàng mặc định
    Service.gI().dropItemMap(
        this.zone,
        new ItemMap(
            this.zone,
            190,
            Util.nextInt(20000, 30001),
            this.location.x,
            this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24),
            -1 // ai cũng nhặt được
        )
    );

    int rand = Util.nextInt(100); // 0 -> 99

    // ===== 50% rơi tất cả 381-384 (ai cũng nhặt, rải đều) =====
    if (rand < 50) {
        int[] itemsAll = {381, 382, 383, 384};
        int offset = -30;

        for (int itemId : itemsAll) {
            Service.gI().dropItemMap(
                this.zone,
                new ItemMap(
                    this.zone,
                    itemId,
                    1,
                    this.location.x + offset,
                    this.zone.map.yPhysicInTop(this.location.x + offset, this.location.y - 24),
                    -1 // ai cũng nhặt được
                )
            );
            offset += 20; // rải đều xung quanh
        }
    }
    // ===== 25% rơi item 19 (chỉ người hạ boss) =====
    else if (rand < 75) {
        Service.gI().dropItemMap(
            this.zone,
            new ItemMap(
                this.zone,
                19,
                1,
                this.location.x,
                this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24),
                plKill.id
            )
        );
    }
    // ===== 25% rơi item 20 (chỉ người hạ boss) =====
    else {
        Service.gI().dropItemMap(
            this.zone,
            new ItemMap(
                this.zone,
                20,
                1,
                this.location.x,
                this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24),
                plKill.id
            )
        );
    }
}
    @Override
    public void joinMap() {
        st = System.currentTimeMillis();
        this.zone = this.parentBoss.zone;
        ChangeMapService.gI().changeMap(this, this.zone,
                this.parentBoss.location.x + Util.nextInt(-100, 100), this.parentBoss.location.y);
        Service.gI().sendFlagBag(this);
        this.notifyJoinMap();
        this.changeStatus(BossStatus.CHAT_S);
    }

    @Override
    public void doneChatE() {
        if (this.parentBoss == null || this.parentBoss.bossAppearTogether == null
                || this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel]) {
            if ((boss.id == BossID.XEN_CON_2 || boss.id == BossID.XEN_CON_3 || boss.id == BossID.XEN_CON_4 || boss.id == BossID.XEN_CON_5 || boss.id == BossID.XEN_CON_1 || boss.id == BossID.XEN_CON_7) && !boss.isDie()) {
                return;
            }
        }
        this.parentBoss.changeStatus(BossStatus.ACTIVE);
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
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

}
