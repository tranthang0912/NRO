package boss.Broly;

import boss.Boss;
import boss.BossData;
import boss.BossID;
import consts.BossStatus;
import static consts.BossType.BROLY;
import consts.ConstPlayer;
import map.Zone;
import player.Player;
import services.SkillService;
import map.Service.ChangeMapService;
import skill.Skill;
import utils.SkillUtil;
import utils.Util;

public class Broly extends Boss {

    private int maxHp;

    public Broly() throws Exception {
        super(BROLY,BossID.BROLY, new BossData(
                "Broly",
                ConstPlayer.XAYDA,
                new short[]{291, 292, 293, -1, -1, -1},
                5,
                new int[]{1000},
                new int[]{5, 13, 20, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38},
                generateSkills(Skill.TAI_TAO_NANG_LUONG, Skill.MASENKO, Skill.ANTOMIC),
                new String[]{},
                new String[]{"|-1|Haha! ta sẽ giết hết các ngươi",
                             "|-1|Sức mạnh của ta là tuyệt đối",
                             "|-1|Vào hết đây!!!"},
                new String[]{"|-1|Các ngươi giỏi lắm. Ta sẽ quay lại."},
                10000
        ));
    }

    private static int[][] generateSkills(int... skillIds) {
        int[][] skills = new int[skillIds.length * 7][3];
        int index = 0;
        for (int skillId : skillIds) {
            for (int level = 1; level <= 7; level++) {
                skills[index++] = new int[]{skillId, level, 1000};
            }
        }
        return skills;
    }

    @Override
    public void active() {
        super.active();
    }

    @Override
    public void joinMap() {
        this.name = "Broly " + Util.nextInt(10, 100);
        this.nPoint.hpMax = 10000;
        this.nPoint.hp = this.nPoint.hpMax;
        this.maxHp = this.nPoint.hpMax;
        this.nPoint.dame = this.nPoint.hpMax / 100;
        this.nPoint.crit = Util.nextInt(50);
        this.joinMap2();
        st = System.currentTimeMillis();
    }

    public void joinMap2() {
        if (this.zone == null) {
            this.zone = (this.parentBoss != null) ? this.parentBoss.zone : 
                         (this.lastZone != null) ? this.lastZone : getMapJoin();
        }

        if (this.zone != null) {
            try {
                int zoneid = Util.nextInt(2, this.zone.map.zones.size());
                while (zoneid < this.zone.map.zones.size() && !this.zone.map.zones.get(zoneid).getBosses().isEmpty()) {
                    zoneid++;
                }

                if (zoneid < this.zone.map.zones.size()) {
                    this.zone = this.zone.map.zones.get(zoneid);
                } else if (this.id == BossID.BROLY) {
                    this.changeStatus(BossStatus.DIE);
                    return;
                } else {
                    this.zone = this.zone.map.zones.get(Util.nextInt(2, this.zone.map.zones.size()));
                }

                if (this.zone.zoneId < 2) this.leaveMap();

                ChangeMapService.gI().changeMap(this, this.zone, -1, -1);
                this.changeStatus(BossStatus.CHAT_S);
            } catch (Exception e) {
                this.changeStatus(BossStatus.REST);
            }
        } else {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    private long st;

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            if (Util.isTrue(1, 30)) {
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, 6));
                this.updateStats();
                SkillService.gI().useSkill(this, null, null, -1, null);
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            if (!piercing && plAtt.playerSkill.skillSelect.template.id != Skill.TU_SAT && damage > this.nPoint.hpMax / 100) {
                damage = this.nPoint.hpMax / 100;
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

    private long lastTimeAttack;

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(7, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(5, 20)) {
                        if (SkillUtil.isUseSkillChuong(this)) {
                            this.moveTo(pl.location.x + Util.nextInt(-200, 200), 
                                        Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                        } else {
                            this.moveTo(pl.location.x + Util.nextInt(-40, 40),
                                        Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50));
                        }
                    }
                    if (Util.isTrue(1, 100)) {
                        this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, 6));
                        this.updateStats();
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void die(Player plKill) {
        this.changeStatus(BossStatus.DIE);
    }

    private void updateStats() {
        int hpMax = this.nPoint.hpMax;
        int rand = Util.nextInt(10, 15);
        this.nPoint.hpMax = Math.min(hpMax + hpMax / rand, 16_070_777);
        this.nPoint.dame = this.nPoint.hpMax / 10;
    }

   @Override
public void leaveMap() {
    Zone zone = this.zone;
    int x = this.location.x;
    int y = this.location.y;
    ChangeMapService.gI().exitMap(this);

    try {
        
        if (this.nPoint.hpMax >= 1_000_000) {
            new SuperBroly(zone, x, y);
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    }

    this.lastZone = null;
    this.lastTimeRest = System.currentTimeMillis();
    this.changeStatus(BossStatus.REST);
}

}
