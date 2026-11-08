package managers;
import matches.PVP;
import player.Player;
import server.ServerManager;
import utils.Functions;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class PVPManager implements Runnable {

    private static PVPManager i;

    public static PVPManager gI() {
        if (i == null) {
            i = new PVPManager();
        }
        return i;
    }

    private ArrayList<PVP> pvps;

    public PVPManager() {
        this.pvps = new ArrayList<>();
        Executors.newSingleThreadExecutor().submit(this, "Update pvp");
    }

    public void removePVP(PVP pvp) {
        this.pvps.remove(pvp);
    }

    public void addPVP(PVP pvp) {
        this.pvps.add(pvp);
    }

    public PVP getPVP(Player player) {
        for (PVP pvp : this.pvps) {
            if (pvp.p1.equals(player) || pvp.p2.equals(player)) {
                return pvp;
            }
        }
        return null;
    }

    @Override
    public void run() {
        this.update();
    }

    private void update() {
        while (ServerManager.isRunning) {
            try {
                long st = System.currentTimeMillis();
                for (PVP pvp : pvps) {
                    pvp.update();
                }
                Functions.sleep(Math.max(1000 - (System.currentTimeMillis() - st), 10));
            } catch (Exception e) {
            }
        }
    }

}
