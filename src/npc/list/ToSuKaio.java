package npc.list;

/*
 * @Author Coder: Nguyễn Tấn Tài
 * @Description: Ngọc Rồng Kiwi - Máy Chủ Chuẩn Teamobi 2025
 * @Group Zalo: https://zalo.me/g/toiyeuvietnam2025
 */

import boss.BossID;
import consts.ConstNpc;
import Deputyhead.Service.TrainingService;
import npc.Npc;
import player.Player;
import map.Service.NpcService;
import utils.Util;

public class ToSuKaio extends Npc {

    public ToSuKaio(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            String message = String.format("Tập luyện với Tổ sư Kaio sẽ tăng %s sức mạnh mỗi phút, có thể tăng giảm tùy vào khả năng đánh quái của con",
                    Util.formatNumber(TrainingService.gI().getTnsmMoiPhut(player)));
            String autoTrainingOption = player.dangKyTapTuDong ? "Hủy đăng ký tập tự động" : "Đăng ký tập tự động";
            String autoTrainingMessage = player.dangKyTapTuDong ? "Hủy đăng\nký tập\ntự động" : "Đăng ký\ntập\ntự động";

            this.createOtherMenu(player, ConstNpc.BASE_MENU, message,
                    autoTrainingMessage, "Đồng ý\nluyện tập", "Không\nđồng ý"
                    // , "Nâng\nGiới hạn\nSức mạnh"
                );
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;

        if (player.idMark.isBaseMenu()) {
            switch (select) {
                case 0:
                    handleAutoTrainingMenu(player);
                    break;
                case 1:
                    TrainingService.gI().callBoss(player, BossID.TO_SU_KAIO, false);
                    break;
                default:
                    break;
            }
        } else if (player.idMark.getIndexMenu() == 2001) {
            handleAutoTrainingRegistration(player, select);
        }
    }

    private void handleAutoTrainingMenu(Player player) {
        if (player.dangKyTapTuDong) {
            player.dangKyTapTuDong = false;
            NpcService.gI().createTutorial(player, tempId, avartar, "Con đã hủy thành công đăng ký tập tự động\nTừ giờ con muốn tập Offline hãy tự đến đây trước");
        } else {
            showAutoTrainingRegistrationMenu(player);
        }
    }

    private void showAutoTrainingRegistrationMenu(Player player) {
        String message = String.format("Đăng ký để mỗi khi Offline quá 30 phút, con sẽ được tự động luyện tập với tốc độ %s sức mạnh mỗi phút",
                TrainingService.gI().getTnsmMoiPhut(player));
        this.createOtherMenu(player, 2001, message, "Hướng\ndẫn\nthêm", "Đồng ý\n1 ngọc\nmỗi lần", "Không\nđồng ý");
    }

    private void handleAutoTrainingRegistration(Player player, int select) {
        switch (select) {
            case 0:
                NpcService.gI().createTutorial(player, tempId, avartar, ConstNpc.TAP_TU_DONG);
                break;
            case 1:
                player.mapIdDangTapTuDong = mapId;
                player.dangKyTapTuDong = true;
                NpcService.gI().createTutorial(player, tempId, avartar, "Từ giờ, quá 30 phút Offline con sẽ được tự động luyện tập");
                break;
            default:
                break;
        }
    }
}