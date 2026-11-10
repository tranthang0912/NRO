package web;

import consts.ConstNpc;
import consts.ConstTask;
import java.util.List;
import map.Map;
import map.Service.NpcManager;
import player.Player;

final class WebQuestTransit {
    record Trip(int destination, int npcId, int option, int menu) {}

    static List<Trip> from(Player player, Map map, int objective) {
        if (objective < 0) return List.of();
        return switch (map.mapId) {
            case 24 -> station(player, map, ConstNpc.DR_DRIEF, 25, 26);
            case 25 -> station(player, map, ConstNpc.CARGO, 24, 26);
            case 26 -> station(player, map, ConstNpc.CUI, 24, 25);
            case 84 -> hasNpc(map, ConstNpc.DR_DRIEF)
                    ? List.of(new Trip(24 + player.gender, ConstNpc.DR_DRIEF, 0, ConstNpc.BASE_MENU)) : List.of();
            case 19 -> {
                if (player.playerTask.taskMain.id == 7 || objective == ConstTask.TASK_17_1
                        || !hasNpc(map, ConstNpc.CUI)) yield List.of();
                int menu = switch (objective) {
                    case ConstTask.TASK_19_0 -> ConstNpc.MENU_FIND_KUKU;
                    case ConstTask.TASK_19_1 -> ConstNpc.MENU_FIND_MAP_DAU_DINH;
                    case ConstTask.TASK_19_2 -> ConstNpc.MENU_FIND_RAMBO;
                    default -> ConstNpc.BASE_MENU;
                };
                yield List.of(new Trip(68, ConstNpc.CUI, menu == ConstNpc.BASE_MENU ? 1 : 2, menu));
            }
            case 68 -> player.playerTask.taskMain.id != 7 && hasNpc(map, ConstNpc.CUI)
                    ? List.of(new Trip(19, ConstNpc.CUI, 0, ConstNpc.BASE_MENU)) : List.of();
            case 27, 28, 29, 102 -> calick(map, objective);
            default -> List.of();
        };
    }

    static boolean pending(Player player) {
        return player.idMark != null && player.idMark.isGotoFuture();
    }

    private static List<Trip> station(Player player, Map map, int npcId, int first, int second) {
        if (player.playerTask.taskMain.id == 7 || !hasNpc(map, npcId)) return List.of();
        return List.of(new Trip(first, npcId, 0, ConstNpc.BASE_MENU),
                new Trip(second, npcId, 1, ConstNpc.BASE_MENU),
                new Trip(84, npcId, 2, ConstNpc.BASE_MENU));
    }

    private static List<Trip> calick(Map map, int objective) {
        if (objective < ConstTask.TASK_20_0 || objective == ConstTask.TASK_22_1
                || !hasNpc(map, ConstNpc.CALICK)) return List.of();
        if (map.mapId == 102) return List.of(new Trip(24, ConstNpc.CALICK, 1, ConstNpc.BASE_MENU));
        // The original menu controller resolves the roaming Calick by his global NPC entry.
        var roamingCalick = NpcManager.getNpc(ConstNpc.CALICK);
        return roamingCalick != null && roamingCalick.mapId == map.mapId
                ? List.of(new Trip(102, ConstNpc.CALICK, 1, ConstNpc.BASE_MENU)) : List.of();
    }

    private static boolean hasNpc(Map map, int npcId) {
        return map.npcs.stream().anyMatch(npc -> npc.tempId == npcId && npc.mapId == map.mapId);
    }
}
