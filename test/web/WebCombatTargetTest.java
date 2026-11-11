package web;

import map.Zone;
import mob.Mob;
import org.junit.jupiter.api.Test;
import player.Player;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test void keepsAHitTargetUntilItDiesOrStopsProgressing() {
        var zone = new Zone(null, 0, 5);
        var player = new Player();
        player.zone = zone;
        Mob first = mob(zone, 30);
        Mob other = mob(zone, 60);
        var targets = new WebCombatTarget();

        assertSame(first, targets.select(player, id -> true, mob -> false, 1_000));
        assertNull(targets.keepEngaged(player, 1_001));
        first.point.sethp(60);
        targets.markEngaged(first);
        assertSame(first, targets.keepEngaged(player, 2_000));
        first.point.sethp(0);
        assertNull(targets.keepEngaged(player, 2_100));
        assertSame(other, targets.select(player, id -> true, mob -> false, 2_101));
        targets.markEngaged(other);
        assertNull(targets.keepEngaged(player, 10_101));
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
