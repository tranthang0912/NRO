package boss.Cell;
import consts.ConstPlayer;
import boss.Boss;
import boss.BossesData;
import boss.BossID;
import item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import map.ItemMap;
import player.Player;
import services.EffectSkillService;
import services.ItemService;
import player.Service.PlayerService;
import services.Service;
import services.TaskService;
import utils.Util;
import map.Service.ChangeMapService;

public class XenBoHung extends Boss {

    private long lastTimeHapThu;
    private int timeHapThu;

    public XenBoHung() throws Exception {
        super(BossID.XEN_BO_HUNG, BossesData.XEN_BO_HUNG_1, BossesData.XEN_BO_HUNG_2, BossesData.XEN_BO_HUNG_3);
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
        this.hapThu();
        this.attack();
    }

    private void hapThu() {
        if (!Util.canDoWithTime(this.lastTimeHapThu, this.timeHapThu) || !Util.isTrue(1, 100)) {
            return;
        }

        Player pl = this.zone.getRandomPlayerInMap();
        if (pl == null || pl.isDie()) {
            return;
        }
        ChangeMapService.gI().changeMapYardrat(this, this.zone, pl.location.x, pl.location.y);
        this.nPoint.dameg += (pl.nPoint.dame * 5 / 100);
        this.nPoint.hpg += (pl.nPoint.hp * 2 / 100);
        this.nPoint.critg++;
        this.nPoint.calPoint();
        PlayerService.gI().hoiPhuc(this, pl.nPoint.hp, 0);
        pl.injured(null, pl.nPoint.hpMax, true, false);
        Service.gI().sendThongBao(pl, "Bạn vừa bị " + this.name + " hấp thu!");
        this.chat(2, "Ui cha cha, kinh dị quá. " + pl.name + " vừa bị tên " + this.name + " nuốt chửng kìa!!!");
        this.chat("Haha, ngọt lắm đấy " + pl.name + "..");
        this.lastTimeHapThu = System.currentTimeMillis();
        this.timeHapThu = Util.nextInt(10000, 20000);
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage / 2);
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage / 4;
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

}
