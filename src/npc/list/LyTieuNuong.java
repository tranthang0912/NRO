package npc.list;

import consts.ConstNpc;
import database.PlayerDAO;
import npc.Npc;
import player.Player;
import services.Service;
import services.TaskService;
import services.func.Input;
import player.Service.InventoryService;

public class LyTieuNuong extends Npc {

    public LyTieuNuong(int mapid, int status, int cx, int cy, int tempid, int avartar) {
        super(mapid, status, cx, cy, tempid, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player) && !TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "|0| BlackGoku - Game Ngọc Rồng Chuẩn Teamobi 2025",
                    "Mua Thành Viên", "Đổi Thỏi Vàng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player) && player.idMark.isBaseMenu()) {
            switch (select) {
                case 0 -> {
                    if (!player.getSession().actived) {
                        if (player.getSession().vnd >= 10000) {
                            player.getSession().actived = true;
                            if (PlayerDAO.MuaThanhVien(player, 0)) {
                                InventoryService.gI().sendItemBags(player);
                                Service.gI().sendMoney(player);
                                npcChat(player, "Đã Mua thành viên thành công!");
                            } else {
                                npcChat(player, "Không đủ tiền để mở thành viên...!");
                            }
                        }
                    } else {
                        npcChat(player, "Đã mở thành viên!");
                    }
                }
                case 1 -> {
                    Input.gI().createFormTradeGold(player);
                }
            }
        }
    }
}