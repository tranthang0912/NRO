package boss.Android;
import boss.Boss;
import boss.BossID;
import boss.BossesData;
import java.util.Random;
import map.ItemMap;
import player.Player;
import skill.Skill;
import player.Service.PlayerService;
import services.Service;
import services.TaskService;
import utils.Util;

public class DrKore extends Boss {

    public DrKore() throws Exception {
        super(BossID.DR_KORE, BossesData.DR_KORE);
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
    public void chatM() {
        if (Util.isTrue(60, 61)) {
            super.chatM();
            return;
        }
        if (this.bossAppearTogether == null || this.bossAppearTogether[this.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.bossAppearTogether[this.currentLevel]) {
            if (boss.id == BossID.ANDROID_19 && !boss.isDie()) {
                this.chat("Hút năng lượng của nó, mau lên");
                boss.chat("Tuân lệnh đại ca, hê hê hê");
                break;
            }
        }
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
    private long st;

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (plAtt != null) {
            switch (plAtt.playerSkill.skillSelect.template.id) {
                case Skill.KAMEJOKO:
                case Skill.MASENKO:
                case Skill.ANTOMIC:
                    PlayerService.gI().hoiPhuc(this, damage, 0);
                    if (Util.isTrue(1, 5)) {
                        this.chat("Hấp thụ.. các ngươi nghĩ sao vậy?");
                    }
                    return 0;
            }
        }
        return super.injured(plAtt, damage, piercing, isMobAttack);
    }

    @Override
    public void doneChatS() {
        for (Boss boss : this.bossAppearTogether[this.currentLevel]) {
            if (boss.id == BossID.ANDROID_19) {
                boss.changeToTypePK();
                break;
            }
        }
    }

    @Override
    public void changeToTypePK() {
        super.changeToTypePK();
        this.chat("Mau đền mạng cho thằng em trai ta");
    }
}
