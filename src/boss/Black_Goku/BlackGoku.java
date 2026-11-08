package boss.Black_Goku;


import boss.*;
import consts.ConstPlayer;
import consts.ConstTask;
import item.Item;

import java.util.ArrayList;
import java.util.List;
import map.ItemMap;
import player.Player;
import services.EffectSkillService;
import services.ItemService;
import services.Service;
import utils.Util;

import services.SkillService;
import services.TaskService;

public class BlackGoku extends Boss {

    private long st;
    private int timeLeaveMap;

    public BlackGoku() throws Exception {
        super(BossID.BLACK_GOKU, false, true, BossesData.BLACK_GOKU, BossesData.SUPER_BLACK_GOKU);
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
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            if (this.currentLevel != 0) {
                damage /= 2;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage - Util.nextInt(100000));
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return (int) damage;
        } else {
            return 0;
        }
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, timeLeaveMap)) {
            if (Util.isTrue(1, 2)) {
                this.leaveMap();
            } else {
                this.leaveMapNew();
            }
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
            timeLeaveMap = Util.nextInt(300000, 900000);
        }
    }

    @Override
    public void joinMap() {
        this.name = this.data[this.currentLevel].getName() + " " + Util.nextInt(1, 100);
        super.joinMap();
        st = System.currentTimeMillis();
        timeLeaveMap = Util.nextInt(600000, 900000);
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                int dis = Util.getDistance(this, pl);
                if (dis > 450) {
                    move(pl.location.x - 24, pl.location.y);
                } else if (dis > 100) {
                    int dir = (this.location.x - pl.location.x < 0 ? 1 : -1);
                    int move = Util.nextInt(50, 100);
                    move(this.location.x + (dir == 1 ? move : -move), pl.location.y);
                } else {
                    if (Util.isTrue(30, 100)) {
                        int move = Util.nextInt(50);
                        move(pl.location.x + (Util.nextInt(0, 1) == 1 ? move : -move), this.location.y);
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
