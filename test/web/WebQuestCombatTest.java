package web;

import consts.ConstPlayer;
import intrinsic.Intrinsic;
import item.Template.SkillTemplate;
import java.util.List;
import map.Map;
import mob.Mob;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import player.Player;
import server.Manager;
import services.SkillService;
import skill.NClass;
import skill.Skill;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebQuestCombatTest {
    private final WebQuestCombat combat = new WebQuestCombat();
    private final Player player = player();

    @BeforeAll static void provideSkillClasses() {
        if (Manager.NCLASS.isEmpty()) Manager.NCLASS.addAll(List.of(new NClass(), new NClass(), new NClass()));
    }

    @Test void spendsKiOnStrongTargetsAndUsesBasicForWeakTargets() {
        Skill basic = learn(Skill.DRAGON, 1, 100, 32, 0, 1);
        Skill beam = learn(Skill.KAMEJOKO, 1, 200, 160, 0, 30);
        Mob mob = mob(1_000);
        assertSame(beam, combat.select(player, null, mob, basic, 70));
        mob.point.sethp(60);
        assertSame(basic, combat.select(player, null, mob, basic, 70));
        basic.lastTimeUseThisSkill = System.currentTimeMillis();
        assertSame(basic, combat.select(player, null, mob, basic, 70));
        assertSame(beam, combat.select(player, null, mob, basic, 150));
        assertSame(basic, combat.select(player, null, mob, basic, 201));
    }

    @Test void usesBasicWhenLearnedSkillIsCoolingDownOrKiIsInsufficient() {
        Skill basic = learn(Skill.GALICK, 1, 100, 32, 0, 1);
        Skill beam = learn(Skill.ANTOMIC, 1, 220, 160, 0, 30);
        Mob mob = mob(1_000);
        beam.lastTimeUseThisSkill = System.currentTimeMillis();
        player.playerSkill.skillSelect = basic;
        assertSame(basic, combat.select(player, null, mob, basic, 75));
        beam.lastTimeUseThisSkill = 0;
        player.nPoint.mp = 29;
        assertSame(basic, combat.select(player, null, mob, basic, 75));
        player.nPoint.mp = 0;
        assertSame(basic, combat.select(player, null, mob, basic, 75));
        player.nPoint.mp = 30;
        assertSame(beam, combat.select(player, null, mob, basic, 75));
        assertSame(basic, player.playerSkill.skillSelect);
    }

    @Test void avoidsMeleeBeyondNativeHitDistanceAndAvoidsKaiokenAtLowHealth() {
        Skill basic = learn(Skill.DRAGON, 1, 100, 32, 0, 1);
        Skill kaioken = learn(Skill.KAIOKEN, 1, 200, 32, 0, 50);
        Mob mob = mob(1_000);
        assertSame(kaioken, combat.select(player, null, mob, basic, 99));
        assertSame(basic, combat.select(player, null, mob, basic, 109));
        player.nPoint.hp = player.nPoint.hpMax / 4;
        assertSame(basic, combat.select(player, null, mob, basic, 75));
    }

    @Test void keepsNativeChargeSelectedAndInterruptsItWhenAnEnemyClosesIn() {
        Skill basic = learn(Skill.GALICK, 1, 100, 32, 0, 1);
        Skill charge = learn(Skill.TAI_TAO_NANG_LUONG, 3, 4, 0, 1, 0);
        Mob mob = mob(1_000);
        player.nPoint.mp = 0;
        assertSame(basic, combat.select(player, null, mob, basic, 75));
        assertSame(charge, combat.select(player, null, mob, basic, 240));
        player.playerSkill.skillSelect = charge;
        assertTrue(SkillService.gI().useSkill(player, null, null, -1, null));
        assertTrue(player.effectSkill.isCharging);
        player.nPoint.update();
        assertTrue(player.nPoint.mp > 0);
        assertNull(combat.select(player, null, mob, basic, 240));
        assertSame(charge, player.playerSkill.skillSelect);
        assertSame(basic, combat.select(player, null, mob, basic, 75));
        assertFalse(player.effectSkill.isCharging);
    }

    @Test void keepsNativeLaserWindupBoundToTheTargetAndWaitsBeforeFiring() {
        Skill basic = learn(Skill.DEMON, 1, 100, 32, 0, 1);
        Skill laser = learn(Skill.MAKANKOSAPPO, 1, 70, 20_000, 2, 0);
        Mob mob = mob(3_000);
        assertSame(laser, combat.select(player, null, mob, basic, 75));
        player.playerSkill.skillSelect = laser;
        assertTrue(SkillService.gI().useSkill(player, null, mob, -1, null));
        assertTrue(player.playerSkill.prepareLaze);
        assertNull(combat.select(player, null, mob, basic, 75));
        player.playerSkill.lastTimePrepareLaze -= 3_100;
        assertSame(laser, combat.select(player, null, mob, basic, 75));
        assertSame(basic, combat.select(player, null, mob(50), basic, 75));
        assertFalse(player.playerSkill.prepareLaze);
    }

    @Test void cancelsNativeSpiritBombIfAnotherPlayerArrivesDuringWindup() {
        Skill basic = learn(Skill.DRAGON, 1, 100, 32, 0, 1);
        Skill bomb = learn(Skill.QUA_CAU_KENH_KHI, 1, 500, 300, 1, 50);
        Mob mob = mob(3_000);
        assertSame(bomb, combat.select(player, null, mob, basic, 75));
        player.playerSkill.skillSelect = bomb;
        assertTrue(SkillService.gI().useSkill(player, null, mob, -1, null));
        assertTrue(player.playerSkill.prepareQCKK);
        assertNull(combat.select(player, null, mob, basic, 75));
        player.zone.addPlayer(player());
        assertSame(basic, combat.select(player, null, mob, basic, 75));
        assertFalse(player.playerSkill.prepareQCKK);
    }

    @Test void releasesPreparedLaserThroughNativeDamageAndKiRules() {
        Skill basic = learn(Skill.DEMON, 1, 100, 32, 0, 1);
        Skill laser = learn(Skill.MAKANKOSAPPO, 1, 70, 20_000, 2, 0);
        Mob mob = mob(3_000);
        assertSame(laser, combat.select(player, null, mob, basic, 75));
        player.playerSkill.skillSelect = laser;
        assertTrue(SkillService.gI().useSkill(player, null, mob, -1, null));
        player.playerSkill.lastTimePrepareLaze -= 3_100;
        assertSame(laser, combat.select(player, null, mob, basic, 75));
        assertTrue(SkillService.gI().useSkill(player, null, mob, -1, null));
        assertFalse(player.playerSkill.prepareLaze);
        assertTrue(mob.point.hp < 3_000);
        assertTrue(player.nPoint.mp == 0);
    }

    @Test void avoidsAreaStunWhenNativeSkillCanAffectAnotherPlayer() {
        Skill basic = learn(Skill.DRAGON, 1, 100, 32, 0, 1);
        Skill stun = learn(Skill.THAI_DUONG_HA_SAN, 3, 3_000, 150, 1, 45);
        Player boss = player();
        boss.isBoss = true;
        Player other = player();
        other.typePk = ConstPlayer.PK_ALL;
        other.location.x = 140;
        player.zone.addPlayer(other);
        assertSame(basic, combat.select(player, boss, null, basic, 75));
        other.location.x = 200;
        assertSame(stun, combat.select(player, boss, null, basic, 75));
    }

    @Test void selfHealCannotUseItsNativeAreaEffectOnAnotherCharacter() {
        Skill basic = learn(Skill.DEMON, 1, 100, 32, 0, 1);
        Skill heal = learn(Skill.TRI_THUONG, 2, 50, 100, 1, 40);
        heal.point = 2;
        player.nPoint.hp = player.nPoint.hpMax / 4;
        Player other = player();
        other.location.x = 250;
        player.zone.addPlayer(other);
        Mob mob = mob(1_000);
        assertSame(basic, combat.select(player, null, mob, basic, 75));
        other.location.x = 310;
        assertSame(heal, combat.select(player, null, mob, basic, 75));
        assertTrue(WebQuestCombat.selfHeal(heal));
    }

    private Skill learn(int id, int type, int damage, int range, int manaType, int mana) {
        Skill skill = new Skill();
        skill.template = new SkillTemplate();
        skill.template.id = (byte) id;
        skill.template.type = (byte) type;
        skill.template.manaUseType = (byte) manaType;
        skill.point = 1;
        skill.damage = (short) damage;
        skill.dx = range;
        skill.manaUse = mana;
        skill.coolDown = 500;
        player.playerSkill.skills.add(skill);
        return skill;
    }

    private Mob mob(int hp) {
        Mob mob = new Mob();
        mob.zone = player.zone;
        mob.point.setHpFull(hp);
        mob.point.sethp(hp);
        player.zone.mobs.add(mob);
        return mob;
    }

    private static Player player() {
        Player player = new Player();
        var map = new Map(0, "Test", (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                new int[1][1], null, null, null, 1, 5, List.of());
        player.zone = map.zones.get(0);
        player.zone.addPlayer(player);
        player.nPoint.dame = 100;
        player.nPoint.hp = player.nPoint.hpMax = 1_000;
        player.nPoint.mp = player.nPoint.mpMax = 1_000;
        player.nPoint.stamina = 100;
        player.playerIntrinsic.intrinsic = new Intrinsic();
        return player;
    }
}
