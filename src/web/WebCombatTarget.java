package web;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;
import mob.Mob;
import player.Player;

/** Keeps a reachable target long enough to finish it and backs off stalled targets. */
final class WebCombatTarget {
    private static final long STALL_MS = 8_000;
    private static final long RETRY_MS = 15_000;
    private final Map<Mob, Long> blocked = new IdentityHashMap<>();
    private Mob current;
    private boolean engaged;
    private long lastProgress;
    private long previousHp;
    private double bestDistance;

    Mob current() { return current; }

    void release() { current = null; engaged = false; }

    void markEngaged(Mob mob) {
        if (current == mob && !mob.isDie()) engaged = true;
    }

    Mob keepEngaged(Player player, long now) {
        if (!engaged) return null;
        if (eligible(player, current, mob -> true) && continueTarget(player, now)) return current;
        release();
        return null;
    }

    void clear() {
        release();
        blocked.clear();
    }

    Mob select(Player player, Predicate<Mob> matches, Predicate<Mob> claimed, long now) {
        if (eligible(player, current, matches) && continueTarget(player, now)) return current;
        release();
        blocked.values().removeIf(until -> until <= now);
        boolean bestClaimed = true;
        bestDistance = Double.POSITIVE_INFINITY;
        for (Mob mob : player.zone.mobs) {
            if (!eligible(player, mob, matches) || blocked.containsKey(mob)) continue;
            boolean reserved = claimed.test(mob) || (!mob.temporaryEnemies.contains(player) && mob.temporaryEnemies.stream()
                    .anyMatch(other -> other != null && other.zone == player.zone && !other.isDie() && distance(other, mob) <= 120));
            double distance = distance(player, mob);
            if (reserved == bestClaimed ? distance < bestDistance : !reserved) {
                current = mob;
                bestClaimed = reserved;
                bestDistance = distance;
            }
        }
        if (current != null) {
            previousHp = current.point.hp;
            lastProgress = now;
        }
        return current;
    }

    private boolean continueTarget(Player player, long now) {
        double distance = distance(player, current);
        if (distance + 8 < bestDistance || current.point.hp < previousHp) {
            bestDistance = distance;
            lastProgress = now;
        }
        previousHp = current.point.hp;
        if (now - lastProgress < STALL_MS) return true;
        blocked.put(current, now + RETRY_MS);
        return false;
    }

    private static boolean eligible(Player player, Mob mob, Predicate<Mob> matches) {
        return mob != null && mob.zone == player.zone && !mob.isDie() && !mob.isBigBoss() && matches.test(mob);
    }

    private static double distance(Player player, Mob mob) {
        return GameWorld.distance(player.location.x, player.location.y, mob.location.x, mob.location.y);
    }
}
