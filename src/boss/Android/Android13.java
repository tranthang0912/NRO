package boss.Android;
import boss.Boss;
import boss.BossID;
import boss.BossesData;
import item.Item;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import map.ItemMap;
import player.Player;
import services.ItemService;
import services.Service;
import services.TaskService;
import utils.Util;

public class Android13 extends Boss {

    public Android13() throws Exception {
        super(BossID.ANDROID_13, BossesData.ANDROID_13);
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
    public void doneChatS() {
        if (this.parentBoss == null) {
            return;
        }
        if (this.parentBoss.bossAppearTogether == null
                || this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel]) {
            if (boss.id == BossID.ANDROID_15 && !boss.isDie()) {
                boss.changeToTypePK();
                break;
            }
        }
        this.parentBoss.changeToTypePK();
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (damage >= this.nPoint.hp) {
            boolean flag = true;
            if (this.parentBoss != null) {
                if (this.parentBoss.bossAppearTogether != null && this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] != null) {
                    for (Boss boss : this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel]) {
                        if (boss.id == BossID.ANDROID_15 && !boss.isDie()) {
                            flag = false;
                            break;
                        }
                    }
                }
                if (flag && !this.parentBoss.isDie()) {
                    flag = false;
                }
            }
            if (!flag) {
                return 0;
            }
        }
        return super.injured(plAtt, damage, piercing, isMobAttack);
    }
}
