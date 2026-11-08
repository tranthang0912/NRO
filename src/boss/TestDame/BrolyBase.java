package boss.TestDame;

import consts.BossStatus;
import boss.Boss;
import boss.BossID;
import boss.BossesData;
import static consts.BossType.FINAL;
import map.Zone;
import player.Player;
import map.Service.ChangeMapService;
import utils.Util;

public class BrolyBase extends Boss {

    public BrolyBase() throws Exception {
        super(FINAL, Util.randomBossId(), BossesData.BROLY_BASE);
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
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
        if (this.zone == null) {
            this.zone = getMapJoin();
        }
        if (this.zone != null) {
            try {
                int[] spawnPos = getSpawnLocation(this.zone);
                ChangeMapService.gI().changeMap(this, this.zone, spawnPos[0], spawnPos[1]);
                this.notifyJoinMap();
                this.changeStatus(BossStatus.CHAT_S);
            } catch (Exception e) {
                this.changeStatus(BossStatus.REST);
            }
        } else {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    private int[] getSpawnLocation(Zone zone) {
        int x, y;
        int mapId = zone.map.mapId;

        switch (mapId) {
            case 42 -> {
                x = 1015;
                y = 408;
            }
            case 43 -> {
                x = 1190;
                y = 432;
            }
            case 44 -> {
                x = 1150;
                y = 432;
            }
            default -> {
                x = zone.map.mapWidth > 100 ? Util.nextInt(100, zone.map.mapWidth - 100) : Util.nextInt(100);
                y = zone.map.yPhysicInTop(x, 100);
            }
        }
        return new int[] { x, y };
    }
    
    @Override
    public void moveTo(int x, int y) {
    }

    @Override
    public void moveToPlayer(Player player) {
    }

    @Override
    public void attack() {
    }

    @Override
    public void reward(Player plKill) {
    }

    @Override
    public void die(Player plKill) {
        this.changeStatus(BossStatus.REST);
    }

    @Override
    public void autoLeaveMap() {
    }

    @Override
    protected void notifyJoinMap() {
    }
}
