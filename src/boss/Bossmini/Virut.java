package boss.Bossmini;

import boss.Boss;
import boss.BossData;
import boss.BossID;
import consts.BossStatus;
import consts.ConstPlayer;
import consts.ConstRatio;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import map.ItemMap;
import player.Player;
import server.Client;
import services.EffectSkillService;
import services.ItemTimeService;
import services.Service;
import services.SkillService;
import map.Service.ChangeMapService;
import skill.Skill;
import utils.Util;

public class Virut extends Boss {

    private final Map<Long, Long> globalEffectTimers = new ConcurrentHashMap<>();

    public Virut() throws Exception {
        super(BossID.Virut, new BossData(
                "Virut " + Util.nextInt(1, 49),
                ConstPlayer.TRAI_DAT,
                new short[]{651, 778, 779, -1, -1, -1},
                10,
                new int[]{100},
                new int[]{5, 7, 0, 14},
                new int[][]{{Skill.DRAGON, 7, 1000}},
                new String[]{}, // Text chat 1
                new String[]{}, // Text chat 2
                new String[]{},
                600));
    }

    private void applyEffect(Player player) {
        long effectEndTime = System.currentTimeMillis() + 300000;
        globalEffectTimers.put(player.id, effectEndTime);
        ItemTimeService.gI().sendItemTime(player, 7143, 10);
        this.chat("Khè Khè, " + player.name + " Đã bị nhiễm ");
    }

    private void checkGlobalEffects() {
        long currentTime = System.currentTimeMillis();

        globalEffectTimers.forEach((playerId, effectEndTime) -> {
            if (currentTime >= effectEndTime) {
                Player player = Client.gI().getPlayer(playerId);
                if (player != null) {
                    if (!player.isDie()) {
                        if (Util.isTrue(30, 100)) {
                            player.injured(null, player.nPoint.hp, true, false);
                        }
                    }
                }
                globalEffectTimers.remove(playerId);
            }
        });
    }

    private void updateOdo() {
        try {
            if (Util.isTrue(30, 100)) {
                List<Player> playersMap = this.zone.getNotBosses();
                for (Player pl : playersMap) {
                    if (pl != null && pl.nPoint != null && !this.equals(pl) && !pl.isBoss && !pl.isDie()
                            && Util.getDistance(this, pl) <= 200) {
                        applyEffect(pl);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 3000) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = this.getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }

                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));

                if (Util.getDistance(this, pl) <= 40) {
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                    if (!globalEffectTimers.containsKey(pl.id)
                            || System.currentTimeMillis() >= globalEffectTimers.get(pl.id)) {
                        this.updateOdo();
                    }
                } else {
                    this.moveToPlayer(pl);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void reward(Player plKill) {
        for (byte i = 0; i < 5; i++) {
            ItemMap it = new ItemMap(this.zone, 457, (int) 5, this.location.x + i * 3,
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, it);
        }
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
        this.checkGlobalEffects();
    }

    @Override
    public void joinMap() {
        super.joinMap();
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (damage >= 500) {
                damage = 500;
            }
            this.nPoint.subHP(damage);
            return (int) damage;
        } else {
            return 0;
        }
    }

}
