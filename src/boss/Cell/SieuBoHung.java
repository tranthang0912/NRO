package boss.Cell;

import boss.*;
import consts.BossStatus;
import consts.ConstPlayer;
import item.Item;
import item.Item.ItemOption;
import map.ItemMap;
import mob.Mob;
import player.Player;
import player.Service.PlayerService;
import services.*;
import utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SieuBoHung extends Boss {

    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private long st;
    public boolean callCellCon;
    private long lastTimeChat;
    private long lastTimeMove;
    private int indexChat = 0;
    private final String[] text = {
            "Thưa quý vị và các bạn, đây đúng là trận đấu trời long đất lở",
            "Vượt xa mọi dự đoán của chúng tôi",
            "Eo ơi toàn thân lão Xên bốc cháy kìa"
    };

    public SieuBoHung() throws Exception {
        super(BossID.SIEU_BO_HUNG, BossesData.SIEU_BO_HUNG_1, BossesData.SIEU_BO_HUNG_2);
    }

    @Override
    protected void resetBase() {
        super.resetBase();
        this.callCellCon = false;
    }

    public void callCellCon() {
        executor.submit(() -> {
            try {
                this.changeStatus(BossStatus.AFK);
                this.changeToTypeNonPK();
                this.recoverHP();
                this.callCellCon = true;
                this.chat("Hãy đấu với 7 đứa con của ta, chúng đều là siêu cao thủ");
                Thread.sleep(2000);
                this.chat("Cứ chưởng tiếp đi haha");
                Thread.sleep(2000);
                this.chat("Liệu mà giữ mạng đấy");
                Thread.sleep(2000);
                for (Boss boss : this.bossAppearTogether[this.currentLevel]) {
                    switch ((int) boss.id) {
                        case BossID.XEN_CON_1, BossID.XEN_CON_2, BossID.XEN_CON_3,
                             BossID.XEN_CON_4, BossID.XEN_CON_5, BossID.XEN_CON_6,
                             BossID.XEN_CON_7 -> boss.changeStatus(BossStatus.RESPAWN);
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public void recoverHP() {
        PlayerService.gI().hoiPhuc(this, this.nPoint.hpMax, 0);
    }
 @Override
public void reward(Player plKill) {
    if (this.zone == null || plKill == null) return;

    int x = this.location.x;
    int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);

    int chance = Util.nextInt(100);

    // ================== 50% RƠI ĐỒ ==================
    if (chance < 20) {

        int[] items = {
            233, 241, 237,
            245, 253, 249,
            265, 261, 257,
            277, 273, 269,
            281
        };

        int itemId = items[Util.nextInt(items.length)];
        ItemMap it = new ItemMap(this.zone, itemId, 1, x, y, plKill.id);

        if (it.options == null) {
            it.options = new ArrayList<>();
        }

        // ===== OPTION CHÍNH =====
        if (itemId == 233 || itemId == 241 || itemId == 237) {
            it.options.add(new Item.ItemOption(47, Util.nextInt(500, 701)));
        } else if (itemId == 265 || itemId == 261 || itemId == 257) {
            it.options.add(new Item.ItemOption(0, Util.nextInt(2100, 2501)));
        } else if (itemId == 245 || itemId == 253 || itemId == 249) {
            it.options.add(new Item.ItemOption(6, Util.nextInt(23000, 26001)));
        } else if (itemId == 277 || itemId == 273 || itemId == 269) {
            it.options.add(new Item.ItemOption(7, Util.nextInt(25000, 28001)));
        } else if (itemId == 281) {
            it.options.add(new Item.ItemOption(14, Util.nextInt(11, 14)));
        }

        // ===== OPTION PHỤ =====
        int[] extraOps = {50, 77, 103};
        int opId = extraOps[Util.nextInt(extraOps.length)];
        it.options.add(new Item.ItemOption(opId, Util.nextInt(1, 6)));

        // ===== RANDOM SAO =====
        int rand = Util.nextInt(100);
        int star = rand < 50 ? 1 : rand < 90 ? 2 : 3;
        it.options.add(new Item.ItemOption(107, star));

        Service.gI().dropItemMap(this.zone, it);

    }
    // ================== 50% RƠI ITEM 16 / 17 ==================
    else {
        int[] ids = {16, 17};
        int itemId = ids[Util.nextInt(ids.length)];
        ItemMap it = new ItemMap(this.zone, itemId, 1, x, y, plKill.id);
        Service.gI().dropItemMap(this.zone, it);
    }

    // CHECK TASK
    TaskService.gI().checkDoneTaskKillBoss(plKill, this);
}


    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (prepareBom) return 0;

        if (!callCellCon && damage >= this.nPoint.hp) {
            callCellCon();
            return 0;
        }

        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }

            damage = this.nPoint.subDameInjureWithDeff(damage / 3);

            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage /= 4;
            }

            this.nPoint.subHP(damage);

            if (isDie()) {
                setBom(plAtt);
                return 0;
            }

            return (int) damage;
        }
        return 0;
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void autoLeaveMap() {
        this.mc();
        if (this.currentLevel > 0 && this.bossStatus == BossStatus.AFK) {
            this.changeStatus(BossStatus.ACTIVE);
        }
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    public void mc() {
        Player mc = zone.getNpc();
        if (mc != null) {
            if (Util.canDoWithTime(lastTimeChat, 3000)) {
                Service.gI().chat(mc, text[indexChat]);
                indexChat = (indexChat + 1) % text.length;
                lastTimeChat = System.currentTimeMillis() + (indexChat == 0 ? 7000 : 0);
            }

            if (Util.canDoWithTime(lastTimeMove, 15000) && Util.isTrue(2, 3)) {
                int x = this.location.x + Util.nextInt(-100, 100);
                int y = (x > 156 && x < 611) ? 288 : 312;
                PlayerService.gI().playerMove(mc, x, y);
                lastTimeMove = System.currentTimeMillis();
            }
        }
    }
}
