package web;

import intrinsic.Intrinsic;
import org.junit.jupiter.api.Test;
import player.Player;
import static org.junit.jupiter.api.Assertions.*;

class WebQuestTrainingTest {
    @Test void evenOnePointGainsAccumulateTheHealthShare() {
        Player player = new Player();
        player.playerIntrinsic.intrinsic = new Intrinsic();
        player.playerTask.taskMain = new task.TaskMain();
        player.playerTask.taskMain.id = 10;
        var points = player.nPoint;
        points.hpg = 200;
        points.mpg = 100;
        points.dameg = 10;
        points.calPoint();
        var training = new WebQuestTraining();
        for (int i = 0; i < 6_000; i++) {
            points.tiemNang++;
            training.upgrade(points, 220, 0);
        }
        assertEquals(220, points.hpg);
        assertTrue(points.dameg > 10);
    }

    @Test void savesTheHealthShareAcrossSmallPotentialGainsInsteadOfSpendingItAllOnDamage() {
        Player player = new Player();
        player.playerIntrinsic.intrinsic = new Intrinsic();
        player.playerTask.taskMain = new task.TaskMain();
        player.playerTask.taskMain.id = 10;
        player.playerTask.taskMain.index = 1;
        var points = player.nPoint;
        points.power = 202_748;
        points.hpg = 200;
        points.mpg = 100;
        points.dameg = 10;
        points.calPoint();
        var training = new WebQuestTraining();
        for (int i = 0; i < 100; i++) {
            points.tiemNang += 500;
            training.upgrade(points, 300, 0);
        }
        assertTrue(points.hpg >= 300, "Small gains must eventually buy the HP needed to leave training");
        assertTrue(points.dameg > 10, "Damage remains the main investment");
        long hpSpent = 5L * (2 * (200 + 1_000) + 100 - 20) / 2;
        assertTrue(hpSpent <= 50_000 / 5);
    }
}
