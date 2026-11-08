package map.Service;

import consts.ConstNpc;
import consts.ConstTask;
import npc.Npc;
import player.Player;
import server.Manager;
import services.TaskService;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NpcManager {

    public static Npc getByIdAndMap(int id, int mapId) {
        for (Npc npc : Manager.NPCS) {
            if (npc.tempId == id && npc.mapId == mapId) {
                return npc;
            }
        }
        return null;
    }

    public static Npc getNpc(byte tempId) {
        for (Npc npc : Manager.NPCS) {
            if (npc.tempId == tempId) {
                return npc;
            }
        }
        return null;
    }

    public static List<Npc> getNpcsByMapPlayer(Player player) {
    List<Npc> list = new ArrayList<>();
    if (player.zone != null) {
        for (Npc npc : player.zone.map.npcs) {
            if (npc.tempId == ConstNpc.QUA_TRUNG && player.mabuEgg == null && player.zone.map.mapId == (21 + player.gender)) {
                continue;
            } 
            else if (npc.tempId == ConstNpc.CALICK && TaskService.gI().getIdTask(player) < ConstTask.TASK_21_0) {
                continue;
            } 
            else if (npc.tempId == ConstNpc.QUOC_VUONG && player.nPoint.power < 500L) {
                continue;
            }
            list.add(npc);
        }
    }
    return list;
}

}
