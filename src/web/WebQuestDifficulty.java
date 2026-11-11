package web;

import consts.ConstMob;
import mob.Mob;
import player.Player;
import services.SkillService;

/** Estimates whether the character can sustain a fight with its current equipment. */
final class WebQuestDifficulty {
    private WebQuestDifficulty() {}

    static boolean canFight(Player player, Mob mob) {
        if (mob.isBigBoss()) return false;
        if (mob.zone != null && mob.zone.getBosses().stream().anyMatch(boss -> !boss.isDie()
                && SkillService.gI().canAttackPlayer(boss, player)
                && boss.nPoint.dame - (long) player.nPoint.def > player.nPoint.hpMax / 2L)) return false;
        if (mob.tempId == ConstMob.MOC_NHAN || mob.tempId == ConstMob.BU_NHIN_MA_QUAI) return true;
        return estimatedDamageTaken(player, mob) <= Math.max(1, player.nPoint.hpMax * 3L / 4);
    }

    static long minimumHealth(Player player, Mob mob) {
        return Math.max(1, counterAttack(player, mob) * 4 / 3 + 1);
    }

    private static long estimatedDamageTaken(Player player, Mob mob) {
        long damage = Math.max(1, player.nPoint.dame);
        long mobHp = mob.point.maxHp;
        long hits = Math.max(2, (mobHp + damage - 1) / damage);
        // A provoked ordinary mob can respond about once per second. Basic hits take roughly half a second.
        return counterAttack(player, mob) * ((hits + 1) / 2);
    }

    private static long counterAttack(Player player, Mob mob) {
        long mobHp = mob.point.maxHp;
        long counter = mob.point.dame > 0 ? mob.point.dame : mobHp * Math.max(0, mob.pDame) / 100;
        return Math.max(1, counter + mob.level * 3L - player.nPoint.def);
    }
}
