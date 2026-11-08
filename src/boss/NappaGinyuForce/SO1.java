package boss.NappaGinyuForce;
import boss.Boss;
import boss.BossID;
import consts.BossStatus;
import boss.BossesData;
import java.util.Random;
import map.ItemMap;
import player.Player;
import services.Service;
import services.TaskService;
import utils.Util;

public class SO1 extends Boss {

    private long st;

    public SO1() throws Exception {
        super(BossID.SO_1, false, true, BossesData.SO_1);
    }

    @Override
    public void moveTo(int x, int y) {
        if (this.currentLevel == 1) {
            return;
        }
        super.moveTo(x, y);
    }
@Override
public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
    if (this.isDie()) return 0;

    if (plAtt != null && plAtt.isPl()) {
        int hour = java.time.LocalTime.now().getHour();

        boolean isLimitTime =
                (hour >= 12 && hour < 14) ||
                (hour >= 19 && hour < 22);

        if (isLimitTime) {
            boolean isTask20 = plAtt.playerTask != null
                    && plAtt.playerTask.taskMain != null
                    && plAtt.playerTask.taskMain.id == 20;

            if (!isTask20) {
                Service.gI().sendThongBao(
                        plAtt,
                        "Trong khung giờ này, chỉ người đang làm nhiệm vụ 20 mới gây sát thương được boss!"
                );
                return 0;
            }
        }
    }

    // Né đòn
    if (Util.isTrue(10, 1000)) {
        this.chat("Xí hụt");
        return 0;
    }

    // ✅ GIỮ NGUYÊN DAMAGE THẬT
    this.nPoint.subHP(damage);

    if (this.isDie()) {
        this.setDie(plAtt);
        this.die(plAtt);
    }
    return (int) damage;
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
            if (boss.id == BossID.SO_2 && !boss.isDie()) {
                return;
            }
        }
        this.parentBoss.changeStatus(BossStatus.ACTIVE);
    }

}
