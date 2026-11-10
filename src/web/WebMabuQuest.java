package web;

import boss.BossID;
import consts.ConstNpc;
import player.Player;
import utils.TimeUtil;

import static consts.ConstTask.*;

/** The original Ôsin event and its route through the ship's floors. */
final class WebMabuQuest {
    static final int ENTRY_MAP = 52;
    private static final int FIRST_FLOOR = 114;

    private WebMabuQuest() {}

    static boolean applies(int objective) {
        return objective >= TASK_28_0 && objective <= TASK_28_6;
    }

    static boolean entryOpen() {
        return TimeUtil.isMabuOpen() && !TimeUtil.isMabu14HOpen();
    }

    static WebQuestTransit.Trip entry() {
        return new WebQuestTransit.Trip(FIRST_FLOOR, ConstNpc.OSIN, 0, ConstNpc.MENU_OPEN_MMB);
    }

    static int floor(int objective) {
        return switch (objective) {
            case TASK_28_1 -> FIRST_FLOOR;
            case TASK_28_2 -> 115;
            case TASK_28_3 -> 117;
            case TASK_28_4 -> 118;
            case TASK_28_5 -> 119;
            case TASK_28_6 -> 120;
            default -> -1;
        };
    }

    static int floorBoss(int mapId) {
        return switch (mapId) {
            case FIRST_FLOOR -> BossID.DRABURA;
            case 115 -> BossID.BUI_BUI;
            case 117 -> BossID.BUI_BUI_2;
            case 118 -> BossID.YA_CON;
            case 119 -> BossID.DRABURA_2;
            case 120 -> BossID.MABU_12H;
            default -> 0;
        };
    }

    static WebQuestTransit.Trip descent(Player player) {
        if (player.fightMabu.pointMabu < player.fightMabu.POINT_MAX) return null;
        var map = player.zone.map;
        int destination = map.mapIdNextMabu((short) map.mapId);
        if (destination < 0) return null;
        int npcId = switch (player.cFlag) {
            case 9 -> ConstNpc.OSIN;
            case 10 -> ConstNpc.BABIDAY;
            default -> -1;
        };
        if (npcId < 0 || map.npcs.stream().noneMatch(npc -> npc.tempId == npcId)) return null;
        int option = player.itemTime.isUseGTPT ? 1 : 2;
        return new WebQuestTransit.Trip(destination, npcId, option, ConstNpc.GO_UPSTAIRS_MENU);
    }
}
