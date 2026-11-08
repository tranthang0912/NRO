package npc.list;

import consts.ConstNpc;
import consts.ConstTask;
import services.AchievementService;
import npc.Npc;
import player.Player;
import services.TaskService;

public class BoMong extends Npc {

    public BoMong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            if (canOpenNpc(player)) {
                if (this.mapId == 47 || this.mapId == 84) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "xin chào", "Nhiệm vụ\nhàng ngày", "Nhiệm vụ\nthành tích", "Từ chối");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 47 || this.mapId == 84) {
                if (player.idMark.isBaseMenu()) {
                    switch (select) {
                        case 0 -> {
                            if (player.playerTask.sideTask.template != null) {
                                String npcSay = "Nhiệm vụ hiện tại: " + player.playerTask.sideTask.getName() + " ("
                                        + player.playerTask.sideTask.getLevel() + ")"
                                        + "\nHiện tại đã hoàn thành: " + player.playerTask.sideTask.count + "/"
                                        + player.playerTask.sideTask.maxCount + " ("
                                        + player.playerTask.sideTask.getPercentProcess() + "%)\nSố nhiệm vụ còn lại trong ngày: "
                                        + player.playerTask.sideTask.leftTask + "/" + ConstTask.MAX_SIDE_TASK;
                                this.createOtherMenu(player, ConstNpc.MENU_OPTION_PAY_SIDE_TASK,
                                        npcSay, "Trả nhiệm\nvụ", "Hủy nhiệm\nvụ");
                            } else {
                                this.createOtherMenu(player, ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK,
                                        "Tôi có vài nhiệm vụ theo cấp bậc, "
                                        + "sức cậu có thể làm được cái nào?",
                                        "10 Ngọc\nDễ", "20 Ngọc\nBình thường", "30 Ngọc\nKhó", "Từ chối");
                            }
                        }
                        case 1 -> {
                            AchievementService.gI().openAchievementUI(player);
                        }

                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK) {
                    switch (select) {
                        case 0, 1, 2->
                            TaskService.gI().changeSideTask(player, (byte) select);
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_OPTION_PAY_SIDE_TASK) {
                    switch (select) {
                        case 0 ->
                            TaskService.gI().paySideTask(player);
                        case 1 ->
                            TaskService.gI().removeSideTask(player);
                    }
                }
            }
        }
    }
}
