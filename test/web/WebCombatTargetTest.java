package web;

import map.Zone;
import mob.Mob;
import org.junit.jupiter.api.Test;
import player.Player;

import static org.junit.jupiter.api.Assertions.assertSame;

class WebCombatTargetTest {
    @Test void holdsProgressingTargetAndSkipsOneThatStalls() {
        var zone = new Zone(null, 0, 5);
        var player = new Player();
        player.zone = zone;
        Mob near = mob(zone, 30), far = mob(zone, 60);
        var targets = new WebCombatTarget();

        assertSame(near, targets.select(player, id -> true, mob -> false, 1_000));
        near.point.sethp(90);
        assertSame(near, targets.select(player, id -> true, mob -> false, 8_000));
        assertSame(far, targets.select(player, id -> true, mob -> false, 16_000));
    }

    @Test void prefersUnclaimedMobAndCanUseSharedTargetWhenItIsTheOnlyChoice() {
        var zone = new Zone(null, 0, 5);
        var player = new Player();
        player.zone = zone;
        Mob near = mob(zone, 30), far = mob(zone, 60);
        var targets = new WebCombatTarget();

        assertSame(far, targets.select(player, id -> true, mob -> mob == near, 1_000));
        far.point.sethp(0);
        assertSame(near, targets.select(player, id -> true, mob -> mob == near, 2_000));
    }

    private static Mob mob(Zone zone, int x) {
        Mob mob = new Mob();
        mob.zone = zone;
        mob.location.x = x;
        mob.point.sethp(100);
        zone.mobs.add(mob);
        return mob;
    }
}
