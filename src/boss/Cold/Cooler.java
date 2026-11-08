package boss.Cold;


import boss.Boss;
import boss.BossID;
import boss.BossesData;
import item.Item;

import java.util.ArrayList;
import java.util.List;
import map.ItemMap;
import player.Player;
import services.EffectSkillService;
import services.Service;
import utils.Util;

import java.util.Random;
import services.ItemService;
import services.TaskService;

public class Cooler extends Boss {

    private long st;

    public Cooler() throws Exception {
        super(BossID.COOLER, BossesData.COOLER, BossesData.COOLER_2);
    }
@Override
public void reward(Player plKill) {
    if (this.zone == null || plKill == null) return;

    int x = this.location.x;
    int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);

    int chance = Util.nextInt(100);

    // ================== 50% RƠI ĐỒ ==================
    if (chance < 30) {

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
            if (Util.isTrue(10, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
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

}
