package web;

import boss.Boss;
import boss.BossID;
import player.Player;

import static consts.ConstMob.*;
import static consts.ConstTask.*;

/** Maps the original task checks to targets the web player can find. */
final class WebQuestTargets {
    private WebQuestTargets() {}

    static int mob(int objective, int gender) {
        return switch (objective) {
            case TASK_1_0 -> MOC_NHAN;
            case TASK_2_0 -> KHUNG_LONG + gender;
            case TASK_4_0, TASK_5_1, TASK_6_1 -> KHUNG_LONG_ME;
            case TASK_4_1, TASK_5_0, TASK_6_2 -> LON_LOI_ME;
            case TASK_4_2, TASK_5_2, TASK_6_0 -> QUY_DAT_ME;
            case TASK_7_1 -> THAN_LAN_BAY + gender;
            case TASK_8_1 -> switch (gender) {
                case 0 -> PHI_LONG_ME;
                case 1 -> QUY_BAY_ME;
                default -> THAN_LAN_ME;
            };
            case TASK_10_1 -> OC_MUON_HON + gender;
            case TASK_14_0 -> HEO_RUNG;
            case TASK_14_1 -> HEO_DA_XANH;
            case TASK_14_2 -> HEO_XAYDA;
            case TASK_15_0 -> BULON;
            case TASK_15_1 -> UKULELE;
            case TASK_15_2 -> QUY_MAP;
            case TASK_16_0 -> TAMBOURINE;
            case TASK_16_1 -> DRUM;
            case TASK_16_2 -> AKKUMAN;
            case TASK_18_0 -> NAPPA;
            case TASK_18_1 -> SOLDIER;
            case TASK_18_2 -> APPULE;
            case TASK_18_3 -> RASPBERRY;
            case TASK_18_4 -> THAN_LAN_XANH;
            case TASK_22_4 -> XEN_CON_CAP_1;
            case TASK_23_3 -> XEN_CON_CAP_3;
            case TASK_25_4 -> XEN_CON_CAP_5;
            case TASK_26_4 -> XEN_CON_CAP_8;
            default -> -1;
        };
    }

    static boolean power(int objective) {
        return switch (objective) {
            case TASK_7_0, TASK_8_0, TASK_10_0, TASK_11_0, TASK_11_1, TASK_11_2, TASK_20_0, TASK_21_0 -> true;
            default -> false;
        };
    }

    static boolean dropsDetectorItem(int template) {
        return template >= XEN_CON_CAP_1 && template <= XEN_CON_CAP_8;
    }

    static int visit(int objective, int gender, int mapId) {
        return switch (objective) {
            case TASK_0_0, TASK_0_1, TASK_12_0 -> 21 + gender;
            case TASK_9_0 -> mapId;
            case TASK_17_0 -> 19;
            case TASK_23_0 -> 93;
            case TASK_24_0 -> 104;
            case TASK_25_0 -> 97;
            case TASK_26_0 -> 99;
            case TASK_27_2 -> 103;
            default -> -1;
        };
    }

    static int boss(int objective) {
        return switch (objective) {
            case TASK_19_0 -> BossID.KUKU;
            case TASK_19_1 -> BossID.MAP_DAU_DINH;
            case TASK_19_2 -> BossID.RAMBO;
            case TASK_20_1 -> BossID.SO_4;
            case TASK_20_2 -> BossID.SO_3;
            case TASK_20_3 -> BossID.SO_2;
            case TASK_20_4 -> BossID.SO_1;
            case TASK_20_5 -> BossID.TIEU_DOI_TRUONG;
            case TASK_21_1, TASK_21_2, TASK_21_3 -> BossID.FIDE;
            case TASK_23_1 -> BossID.ANDROID_19;
            case TASK_23_2 -> BossID.DR_KORE;
            case TASK_24_1 -> BossID.ANDROID_15;
            case TASK_24_2 -> BossID.ANDROID_14;
            case TASK_24_3 -> BossID.ANDROID_13;
            case TASK_25_1 -> BossID.POC;
            case TASK_25_2 -> BossID.PIC;
            case TASK_25_3 -> BossID.KING_KONG;
            case TASK_26_1, TASK_26_2, TASK_26_3 -> BossID.XEN_BO_HUNG;
            case TASK_27_3 -> BossID.XEN_CON_1;
            case TASK_27_4 -> BossID.SIEU_BO_HUNG;
            case TASK_28_1 -> BossID.DRABURA;
            case TASK_28_2 -> BossID.BUI_BUI;
            case TASK_28_3 -> BossID.BUI_BUI_2;
            case TASK_28_4 -> BossID.YA_CON;
            case TASK_28_5 -> BossID.DRABURA_2;
            case TASK_28_6 -> BossID.MABU;
            default -> 0;
        };
    }

    static boolean matchesBoss(Player target, int objective, int id) {
        if (!(target instanceof Boss boss) || boss.isDie()) return false;
        if (objective == TASK_27_3) return boss.id <= BossID.XEN_CON_1 && boss.id >= BossID.XEN_CON_7;
        if (boss.id != id) return false;
        return switch (id) {
            case BossID.FIDE -> boss.currentLevel == (objective - TASK_21_1) / 2;
            case BossID.XEN_BO_HUNG -> boss.currentLevel == (objective - TASK_26_1) / 2;
            default -> true;
        };
    }
}
