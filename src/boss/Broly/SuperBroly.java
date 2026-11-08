package boss.Broly;

/*
 * @Author: NgocRongWhis
 * @Description: Ngọc Rồng Whis - Máy Chủ Chuẩn Teamobi 2024
 * @Group Zalo: https://zalo.me/g/qabzvn331
 */

import boss.Boss;
import boss.BossManager.BrolyManager;
import boss.BossData;
import boss.BossID;
import consts.BossStatus;
import static consts.BossType.BROLY;

import consts.ConstPlayer;
import map.Zone;
import player.Player;
import services.PetService;
import services.SkillService;
import map.Service.ChangeMapService;
import skill.Skill;
import utils.SkillUtil;
import utils.Util;

public class SuperBroly extends Boss {

    private long st;
    private long lastTimeAttack;

    public SuperBroly(Zone zone, int x, int y) throws Exception {
        super(BROLY, BossID.SUPER_BROLY, false, false, new BossData(
                "Super Broly",
                ConstPlayer.XAYDA,
                new short[]{294, 295, 296, -1, -1, -1},
                5,
                new int[]{1000},
                new int[]{5, 13, 20, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38},
                new int[][]{
                    {Skill.TAI_TAO_NANG_LUONG, 1, 50000},
                    {Skill.DEMON, 1, 500},
                    {Skill.KAMEJOKO, 1, 3000},
                    {Skill.MASENKO, 1, 1000},
                    {Skill.ANTOMIC, 1, 2000}
                },
                new String[]{},
                new String[]{
                    "|-1|Haha! ta sẽ giết hết các ngươi",
                    "|-1|Sức mạnh của ta là tuyệt đối",
                    "|-1|Vào hết đây!!!"
                },
                new String[]{"|-1|Các ngươi giỏi lắm. Ta sẽ quay lại."},
                600
        ));
        this.zone = zone;
        this.location.x = x;
        this.location.y = y;
    }

    @Override
    public void reward(Player plKill) {
        if (plKill != null && plKill.pet == null) {
            PetService.gI().createNormalPet(plKill);
        }
    }

    @Override
    public void joinMap() {
        this.name = "Super Broly " + Util.nextInt(10, 100);
        this.typePk = ConstPlayer.PK_ALL;

        this.nPoint.hpMax = Util.nextInt(1_000_000, 16_070_777);
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dame = this.nPoint.hpMax / 100;
        this.nPoint.crit = Util.nextInt(50);

        if (this.zone != null) {
            ChangeMapService.gI().changeMap(this, this.zone, this.location.x, this.location.y);
            this.changeStatus(BossStatus.CHAT_S);
            this.notifyJoinMap();
        } else {
            super.joinMap();
        }

        PetService.gI().createNormalPet(this);
        st = System.currentTimeMillis();
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            leaveMap();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (this.isDie()) return 0;

        if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
            this.chat("Xí hụt");
            return 0;
        }

        if (Util.isTrue(1, 30) && this.playerSkill.skills.size() > 0) {
            int max = Math.min(6, this.playerSkill.skills.size() - 1);
            this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, max));
            tangChiSo();
            SkillService.gI().useSkill(this, null, null, -1, null);
        }

        damage = this.nPoint.subDameInjureWithDeff(damage);

        if (!piercing
                && plAtt != null
                && plAtt.playerSkill != null
                && plAtt.playerSkill.skillSelect != null
                && plAtt.playerSkill.skillSelect.template.id != Skill.TU_SAT
                && damage > this.nPoint.hpMax / 100) {
            damage = this.nPoint.hpMax / 100;
        }

        this.nPoint.subHP(damage);

        if (isDie()) {
            setDie(plAtt);
            die(plAtt);
        }
        return (int) damage;
    }

    @Override
    public void attack() {
        if (!Util.canDoWithTime(this.lastTimeAttack, 100) || this.typePk != ConstPlayer.PK_ALL) {
            return;
        }
        this.lastTimeAttack = System.currentTimeMillis();

        try {
            Player pl = getPlayerAttack();
            if (pl == null || pl.isDie() || this.playerSkill.skills.isEmpty()) return;

            int start = Math.min(7, this.playerSkill.skills.size() - 1);
            int end = this.playerSkill.skills.size() - 1;
            this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(start, end));

            if (Util.getDistance(this, pl) <= getRangeCanAttackWithSkillSelect()) {
                if (Util.isTrue(5, 20)) {
                    if (SkillUtil.isUseSkillChuong(this)) {
                        moveTo(pl.location.x + Util.getOne(-1, 1) * Util.nextInt(20, 200),
                                Util.isTrue(1, 2) ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                    } else {
                        moveTo(pl.location.x + Util.getOne(-1, 1) * Util.nextInt(10, 40),
                                Util.isTrue(1, 2) ? pl.location.y : pl.location.y - Util.nextInt(0, 50));
                    }
                }

                if (Util.isTrue(1, 100)) {
                    int max = Math.min(6, this.playerSkill.skills.size() - 1);
                    this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, max));
                    tangChiSo();
                }

                SkillService.gI().useSkill(this, pl, null, -1, null);
                checkPlayerDie(pl);
            } else if (Util.isTrue(1, 2)) {
                moveToPlayer(pl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tangChiSo() {
        int hpMax = this.nPoint.hpMax;
        int rand = Util.nextInt(80, 100);
        hpMax = Math.min(hpMax + hpMax / rand, 16_070_777);
        this.nPoint.hpMax = hpMax;
        this.nPoint.dame = hpMax / 10;
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        if (this.pet != null) {
            ChangeMapService.gI().exitMap(this.pet);
        }
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
        BrolyManager.gI().removeBoss(this);
        this.dispose();
    }
}
