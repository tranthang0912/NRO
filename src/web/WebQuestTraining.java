package web;

import player.NPoint;

/** Keeps the small HP budget available while prioritizing damage. */
final class WebQuestTraining {
    private long healthSavings;
    private long previousPotential = -1;
    private int healthRemainder;

    void clear() { healthSavings = 0; previousPotential = -1; healthRemainder = 0; }

    void restore(WebQuestTraining old) {
        healthSavings = old.healthSavings;
        previousPotential = old.previousPotential;
        healthRemainder = old.healthRemainder;
    }

    void upgrade(NPoint points, long healthGoal, long skillReserve) {
        if (healthGoal > points.hpMax) {
            long earned = previousPotential < 0 ? points.tiemNang : Math.max(0, points.tiemNang - previousPotential);
            int fraction = (int) (earned % 5) + healthRemainder;
            healthSavings = Math.min(points.tiemNang, healthSavings + earned / 5 + fraction / 5);
            healthRemainder = fraction % 5;
            long before = points.tiemNang;
            WebQuestActions.trainRequiredHealth(points, healthGoal, healthSavings);
            healthSavings -= before - points.tiemNang;
        } else { healthSavings = 0; healthRemainder = 0; }
        WebQuestActions.trainDamage(points, points.getDameLimit(), Math.min(points.tiemNang, skillReserve + healthSavings));
        previousPotential = points.tiemNang;
    }
}
