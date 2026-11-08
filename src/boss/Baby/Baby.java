package boss.Baby;

import boss.Boss;
import boss.BossID;
import boss.BossesData;
import map.ItemMap;
import player.Player;
import services.Service;
import player.Service.PlayerService;
import utils.Util;

public class Baby extends Boss {

    private long lastTimeDrain;

    public Baby() throws Exception {
        super(BossID.BABY, BossesData.BABY_VEGETA);
    }

    // =====================================================
    // PEM BOSS – DAME TỐI ĐA 1
    // =====================================================
    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (this.isDie()) return 0;

        if (Util.isTrue(10, 1000)) {
            this.chat("Xí hụt");
            return 0;
        }

        damage = 1;
        this.nPoint.subHP(damage);

        if (isDie()) {
            this.setDie(plAtt);
            die(plAtt);
        }
         return (int) damage;
    }

    // =====================================================
    // GIẢM HP NGƯỜI CHƠI GẦN (GIỐNG Ở DƠ)
    // =====================================================
    private void drainHpNearbyPlayers() {
        int percent = 10;
        int radius = 200;

        if (!Util.canDoWithTime(lastTimeDrain, Util.nextInt(5000, 10000))) return;

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
        if (this.zone == null) return;

        int playerCount = this.zone.getNotBosses().size();
        int totalDrop = (playerCount < 5) ? 2 : Util.nextInt(5, 7);
        int radius = 120;

        int centerX = this.location.x;
        int centerY = this.zone.map.yPhysicInTop(centerX, this.location.y - 24);

        for (int i = 0; i < totalDrop; i++) {

            double angle = 2 * Math.PI * i / totalDrop;
            int x = centerX + (int) (Math.cos(angle) * radius);
            int y = this.zone.map.yPhysicInTop(x, centerY);

            ItemMap itemMap = new ItemMap(
                    this.zone,
                    462,
                    1,
                    x,
                    y,
                    -1 // PUBLIC
            );

            // 🔥 DÒNG QUAN TRỌNG NHẤT
            this.zone.addItem(itemMap);
            Service.gI().dropItemMap(this.zone, itemMap);
        }
    }
}
