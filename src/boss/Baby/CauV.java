package boss.Baby;

import boss.Boss;
import boss.BossID;
import boss.BossesData;
import item.Item;
import map.ItemMap;
import player.Player;
import services.ItemService;
import services.Service;
import services.TaskService;
import player.Service.PlayerService;
import utils.Util;

public class CauV extends Boss {

    private long lastTimeDrain;

    public CauV() throws Exception {
        super(BossID.CAUV, BossesData.CAUV);
    }

    // =====================================================
    // PEM BOSS – DAME TỐI ĐA 1
    // =====================================================
  @Override
public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
    if (this.isDie()) return 0;

    // Né đòn
    if (Util.isTrue(10, 1000)) {
        this.chat("Xí hụt");
        return 0;
    }

    // GIỚI HẠN DAME TỐI ĐA 1 TRIỆU
    if (damage > 1_000_000) {
        damage = 1_000_000;
    }

    this.nPoint.subHP(damage);

    if (this.isDie()) {
        this.setDie(plAtt);
        this.die(plAtt);
    }
  return (int) damage;
}


    // =====================================================
    // GIẢM HP NGƯỜI CHƠI GẦN (GIỐNG Ở DƠ)
    // =====================================================
    private void drainHpNearbyPlayers() {
        int percent = 10;
        int radius = 200;

        if (!Util.canDoWithTime(lastTimeDrain, Util.nextInt(10000, 20000))) return;

        for (Player pl : this.zone.getNotBosses()) {
            if (pl == null || pl.isDie() || pl.nPoint == null) continue;

            if (Util.getDistance(this, pl) <= radius) {
                int subHp = (int) ((long) pl.nPoint.hpMax * percent / 100);
                if (subHp >= pl.nPoint.hp) subHp = pl.nPoint.hp - 1;

                if (subHp > 0) {
                    this.chat("Hehehe 😈");
                    pl.injured(null, subHp, true, false);
                    PlayerService.gI().sendInfoHpMpMoney(pl);
                }
            }
        }
        lastTimeDrain = System.currentTimeMillis();
    }

    @Override
    public void attack() {
        super.attack();
        drainHpNearbyPlayers();
    }

    // =====================================================
    // RƠI ĐỒ – FIX HIỂN THỊ
    // =====================================================
 @Override
public void reward(Player plKill) {
    TaskService.gI().checkDoneTaskKillBoss(plKill, this);

    int x = this.location.x;
    int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);

    // 80% rơi item 568
    if (Util.isTrue(80, 100)) {
        ItemMap item568 = new ItemMap(
                this.zone,
                (short) 568,
                1,
                x,
                y,
                plKill.id
        );
        Service.gI().dropItemMap(this.zone, item568);
    }
    // 20% rơi item 578 có option
    else {
        ItemMap item578 = new ItemMap(
                this.zone,
                (short) 460,
                1,
                x,
                y,
                plKill.id
        );

        // ADD OPTION TRỰC TIẾP
        item578.options.add(new Item.ItemOption(50, Util.nextInt(1, 21)));
        item578.options.add(new Item.ItemOption(77, Util.nextInt(10, 31)));
        item578.options.add(new Item.ItemOption(103, Util.nextInt(10, 31)));

        Service.gI().dropItemMap(this.zone, item578);
    }
}


}

