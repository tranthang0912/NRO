package npc.list;

import consts.ConstNpc;
import consts.ConstPlayer;
import java.util.ArrayList;
import npc.Npc;
import player.Player;
import map.Service.NpcService;
import services.Service;
import shop.ShopService;
import services.TaskService;
import player.Service.PlayerService;
import skill.Skill;
import utils.Logger;
import utils.SkillUtil;
import utils.TimeUtil;

public class VuaVegeta extends Npc {

    public VuaVegeta(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player) || TaskService.gI().checkDoneTaskTalkNpc(player, this)) return;

        if (player.gender != ConstPlayer.XAYDA) {
            NpcService.gI().createTutorial(
                    player, tempId, avartar,
                    "Con hãy về hành tinh của mình mà thể hiện"
            );
            return;
        }

        ArrayList<String> menu = new ArrayList<>();
        menu.add("Nhiệm vụ");
        menu.add("Học\nKỹ năng");

        createOtherMenu(player, ConstNpc.BASE_MENU,
                "Chào con, ta rất vui khi gặp được con\nCon muốn làm gì nào ?",
                menu.toArray(new String[0]));
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;

        switch (player.idMark.getIndexMenu()) {

            // ================= BASE MENU =================
            case ConstNpc.BASE_MENU:
                switch (select) {

                    // ===== NHIỆM VỤ =====
                    case 0 -> {
                        // ===== HOÀN THÀNH HỌC THƯỜNG =====
                        if (player.LearnSkill.Time != -1
                                && player.LearnSkill.Time <= System.currentTimeMillis()) {

                            player.LearnSkill.Time = -1;

                            try {
                                int itemId = player.LearnSkill.ItemTemplateSkillId;
                                byte level = SkillUtil.getSkillLevelByItemID(itemId);

                                Skill curSkill = SkillUtil.createSkill(
                                        SkillUtil.getTempSkillSkillByItemID(itemId),
                                        level
                                );

                                player.BoughtSkill.add(itemId);
                                SkillUtil.setSkill(player, curSkill);

                                var msg = Service.gI().messageSubCommand((byte) 62);
                                msg.writer().writeShort(curSkill.skillId);
                                player.sendMessage(msg);
                                msg.cleanup();

                                PlayerService.gI().sendInfoHpMpMoney(player);
                            } catch (Exception e) {
                                Logger.log(e.toString());
                            }
                        }

                        NpcService.gI().createTutorial(
                                player,
                                tempId,
                                avartar,
                                player.playerTask.taskMain.subTasks
                                        .get(player.playerTask.taskMain.index).name
                        );
                    }

                    // ===== HỌC KỸ NĂNG =====
                    case 1 -> {
                        if (player.LearnSkill.Time != -1) {
                            long time = player.LearnSkill.Time - System.currentTimeMillis();
                            int itemId = player.LearnSkill.ItemTemplateSkillId;
                            byte level = SkillUtil.getSkillLevelByItemID(itemId);
                            int ngoc = level;

                            createOtherMenu(player, 12,
                                  "NẾU HỌC SKILL 1\nBẠN CẦN THOÁT RA VÀO LẠI KHI HỌC XONG\nĐỂ CẬP NHẬT SKILL 1\nCon đang học kỹ năng\n"
                                            + SkillUtil.findSkillTemplate(
                                                    SkillUtil.getTempSkillSkillByItemID(itemId)).name
                                            + " cấp " + level
                                            + "\nThời gian còn lại " + TimeUtil.getTime(time),
                                    "Học cấp tốc\n" + ngoc + " ngọc",
                                    "Huỷ", "Đóng");
                        } else {
                            ShopService.gI().opendShop(player, "QUY_LAO", false);
                        }
                    }
                }
                break;

            // ================= HỌC CẤP TỐC =================
            case 12:
                if (select == 0) {
                    try {
                        int itemId = player.LearnSkill.ItemTemplateSkillId;
                        byte level = SkillUtil.getSkillLevelByItemID(itemId);
                        int ngoc = level;

                        if (player.inventory.gem < ngoc) {
                            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc");
                            return;
                        }

                        player.inventory.subGem(ngoc);
                        player.LearnSkill.Time = -1;

                        Skill curSkill = SkillUtil.createSkill(
                                SkillUtil.getTempSkillSkillByItemID(itemId),
                                level
                        );

                        player.BoughtSkill.add(itemId);
                        SkillUtil.setSkill(player, curSkill);

                        var msg = Service.gI().messageSubCommand((byte) 62);
                        msg.writer().writeShort(curSkill.skillId);
                        player.sendMessage(msg);
                        msg.cleanup();

                        PlayerService.gI().sendInfoHpMpMoney(player);
                    } catch (Exception e) {
                        Logger.log(e.toString());
                    }
                }
                break;
        }
    }
}
