package npc.list;

import consts.ConstNpc;
import npc.Npc;
import player.Player;
import services.Service;
import services.TaskService;
import database.PlayerDAO;
import shop.ShopService;

public class BulmaTuongLai extends Npc {

    private static final int MENU_BASE = 0;
    private static final int MENU_MTV = 1;
    private static final int MENU_MTV_FREE = 2;

    public BulmaTuongLai(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player))
            return;

        // 🔥 BẮT BUỘC – hoàn thành NV 22_3
        if (TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            return;
        }

        if (this.mapId == 102) {
            this.createOtherMenu(
                    player,
                    MENU_BASE,
                    "Bạn muốn làm gì?",
                    "MTV",
                    "Cửa hàng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player))
            return;

        switch (player.idMark.getIndexMenu()) {

            // MENU GỐC
            case MENU_BASE -> {
                switch (select) {
                    case 0 -> this.createOtherMenu(
                            player,
                            MENU_MTV,
                            "Bạn muốn làm gì?",
                            "MTV FREE",
                            "Đóng");
                    case 1 -> ShopService.gI().opendShop(player, "BUNMA_FUTURE", true);
                }
            }

            // MENU MTV
            case MENU_MTV -> {
                if (select == 0) {
                    this.createOtherMenu(
                            player,
                            MENU_MTV_FREE,
                            "Kích hoạt MTV miễn phí?\nChỉ áp dụng 1 lần.",
                            "Đồng ý",
                            "Hủy");
                }
            }

            // MENU MTV FREE
            case MENU_MTV_FREE -> {
                if (select == 0) {
                    handleActiveMTV(player);
                }
            }
        }
    }

    private void handleActiveMTV(Player player) {
        try {
            int accountId = player.getSession().userId;

            int active = PlayerDAO.getAccountActive(accountId);

            if (active == 1) {
                this.npcChat(player, "Tài khoản của bạn đã kích hoạt MTV rồi!");
                return;
            }

            PlayerDAO.updateAccountActive(accountId, 1);

            this.npcChat(
                    player,
                    "🎉 Kích hoạt MTV FREE thành công!\nVui lòng relog để áp dụng!");

        } catch (Exception e) {
            this.npcChat(player, "Có lỗi xảy ra, vui lòng thử lại sau!");
            e.printStackTrace();
        }
    }
}
