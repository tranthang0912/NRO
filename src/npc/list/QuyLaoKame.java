package npc.list;

import consts.ConstNpc;
import item.Item;
import java.util.ArrayList;
import npc.Npc;
import player.Player;
import player.Service.InventoryService;
import map.Service.NpcService;
import services.RewardService;
import services.Service;
import shop.ShopService;
import services.TaskService;
import services.func.Input;
import player.Service.PlayerService;
import skill.Skill;
import utils.Logger;
import utils.SkillUtil;
import utils.TimeUtil;

public class QuyLaoKame extends Npc {

    public QuyLaoKame(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        Item ruacon = InventoryService.gI().findItemBag(player, 874);
        if (!canOpenNpc(player)) return;

        ArrayList<String> menu = new ArrayList<>();
        if (!player.canReward) {
            menu.add("Nói\nchuyện");
            if (ruacon != null && ruacon.quantity > 0) {
                menu.add("Giao\nRùa con");
            }
        } else {
            menu.add("Giao\nLân con");
        }

        if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Con muốn hỏi gì nào?",
                    menu.toArray(new String[0]));
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;

        if (player.canReward) {
            RewardService.gI().rewardLancon(player);
            return;
        }

        switch (player.idMark.getIndexMenu()) {

            // ================= BASE MENU =================
            case ConstNpc.BASE_MENU:
                if (select == 0) {

                    // ===== HOÀN THÀNH HỌC THƯỜNG (KHÔNG TRỪ TN Ở ĐÂY) =====
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

                    ArrayList<String> menu = new ArrayList<>();
                    menu.add("Nhiệm vụ");
                    menu.add("Học\nKỹ năng");
                    if (player.clan != null && player.clan.isLeader(player)) {
                        menu.add("Giải tán\nBang hội");
                    }

                    createOtherMenu(player, 0,
                            "Chào con, ta rất vui khi gặp con\nCon muốn làm gì nào ?",
                            menu.toArray(new String[0]));
                }

                if (select == 1) {
                    Item ruacon = InventoryService.gI().findItemBag(player, 874);
                    if (ruacon != null && ruacon.quantity > 0) {
                        createOtherMenu(player, 1,
                                "Cảm ơn cậu đã cứu con rùa của ta\nTa sẽ tặng con món quà.",
                                "Nhận quà", "Đóng");
                    }
                }
                break;

            // ================= MENU CHÍNH =================
            case 0:
                switch (select) {
                    case 0:
                        NpcService.gI().createTutorial(
                                player,
                                tempId,
                                avartar,
                                player.playerTask.taskMain.subTasks
                                        .get(player.playerTask.taskMain.index).name
                        );
                        break;

                    case 1:
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
                        break;

                    case 2:
                        if (player.clan != null && player.clan.isLeader(player)) {
                            createOtherMenu(player, 4,
                                    "Con có chắc muốn giải tán bang hội không?",
                                    "Đồng ý", "Từ chối");
                        }
                        break;
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

                        // CHỈ TRỪ NGỌC
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

            // ================= GIẢI TÁN BANG =================
            case 4:
                if (select == 0 && player.clan != null && player.clan.isLeader(player)) {
                    Input.gI().createFormGiaiTanBangHoi(player);
                }
                break;
        }
    }
}
