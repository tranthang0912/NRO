package web;

import consts.ConstMob;
import consts.ConstPlayer;
import map.Zone;
import mob.Mob;
import org.junit.jupiter.api.Test;
import player.Player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebQuestDifficultyTest {
    @Test void questStillAttemptsItsOrdinaryMobWhenARoamingBossSharesTheOnlyQuestMap() {
        Zone zone = new Zone(null, 0, 5);
        Player player = new Player();
        player.zone = zone;
        player.nPoint.hp = player.nPoint.hpMax = 410;
        player.nPoint.dame = 70;
        player.nPoint.def = 2;
        Mob snail = mob(ConstMob.OC_MUON_HON, 1, 2_000);
        snail.zone = zone;
        Player boss = new Player();
        boss.zone = zone;
        boss.isBoss = true;
        boss.typePk = ConstPlayer.PK_ALL;
        boss.nPoint.hp = 1_000;
        boss.nPoint.dame = 500;
        zone.addPlayer(boss);

        assertFalse(WebQuestDifficulty.canFight(player, snail), "AFK farming keeps avoiding the hostile boss");
        assertTrue(WebQuestDifficulty.canAttemptQuest(player, snail), "A roaming boss must not indefinitely replace a viable quest with dinosaur farming");
    }

    @Test void questCanChallengeASnailAfterThePowerStepWithoutRequiringSafeAfkStats() {
        Player player = new Player();
        player.nPoint.hpMax = 230;
        player.nPoint.dame = 60;
        player.nPoint.def = 2;
        Mob snail = mob(ConstMob.OC_MUON_HON, 1, 2_000);
        assertFalse(WebQuestDifficulty.canFight(player, snail), "AFK farming still avoids a fight requiring resurrection");
        assertTrue(WebQuestDifficulty.canAttemptQuest(player, snail), "Quest mode can make useful progress and revive in place");
        assertFalse(WebQuestDifficulty.canAttemptQuest(player, mob(ConstMob.OC_MUON_HON, 1, 200_000)));
    }

    @Test void trainingHealthGoalCoversTheWholeFightInsteadOfOnlyOneCounterattack() {
        Player player = new Player();
        player.nPoint.hpMax = 230;
        player.nPoint.dame = 100;
        Mob snail = mob(ConstMob.OC_MUON_HON, 1, 2_000);
        assertFalse(WebQuestDifficulty.canFight(player, snail));
        player.nPoint.hpMax = (int) WebQuestDifficulty.minimumHealth(player, snail);
        assertTrue(WebQuestDifficulty.canFight(player, snail), "Training must prepare enough HP to actually unlock the quest target");
    }

    @Test void avoidsAGrindingMobWhenItsZoneHasAnOverpoweringHostileBoss() {
        Zone zone = new Zone(null, 0, 5);
        Player player = new Player();
        player.zone = zone;
        player.nPoint.hp = player.nPoint.hpMax = 230;
        player.nPoint.dame = 300;
        Mob dinosaur = mob(ConstMob.KHUNG_LONG, 2, 200);
        dinosaur.zone = zone;
        Player boss = new Player();
        boss.isBoss = true;
        boss.zone = zone;
        boss.nPoint.hp = 1_000;
        boss.nPoint.dame = 5_000;
        boss.typePk = ConstPlayer.PK_ALL;
        zone.addPlayer(boss);

        assertFalse(WebQuestDifficulty.canFight(player, dinosaur));
        boss.nPoint.hp = 0;
        assertTrue(WebQuestDifficulty.canFight(player, dinosaur));
    }

    @Test void comparesActualCombatStatsInsteadOfMobLevelAlone() {
        Player player = new Player();
        player.nPoint.hpMax = 230;
        player.nPoint.dame = 10;
        player.nPoint.def = 2;
        Mob dinosaur = mob(ConstMob.KHUNG_LONG, 2, 200);
        Mob snail = mob(ConstMob.OC_MUON_HON, 1, 2_000);

        assertTrue(WebQuestDifficulty.canFight(player, dinosaur));
        assertFalse(WebQuestDifficulty.canFight(player, snail));
        player.nPoint.dame = 300;
        player.nPoint.hpMax = 1_000;
        assertTrue(WebQuestDifficulty.canFight(player, snail));
    }

    private static Mob mob(int template, int level, int hp) {
        Mob mob = new Mob();
        mob.tempId = template;
        mob.level = (byte) level;
        mob.pDame = 5;
        mob.point.setHpFull(hp);
        return mob;
    }
}
