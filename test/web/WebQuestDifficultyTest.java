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
