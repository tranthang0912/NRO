package npc.list;

import consts.ConstNpc;
import npc.Npc;
import player.NPoint;
import player.Player;
import services.OpenPowerService;
import services.Service;
import utils.Util;

public class QuocVuong extends Npc {

    public QuocVuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                "Con muốn nâng giới hạn sức mạnh cho bản thân hay đệ tử?",
                "Bản thân", "Đệ tử", "Từ chối");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;

        switch (player.idMark.getIndexMenu()) {
            case ConstNpc.BASE_MENU:
                switch (select) {
                    case 0: // Bản thân
                        if (player.nPoint.limitPower < NPoint.MAX_LIMIT) {
                            this.createOtherMenu(player, ConstNpc.OPEN_POWER_MYSEFT,
                                    "Ta sẽ truyền năng lượng giúp con mở giới hạn sức mạnh của bản thân lên "
                                            + Util.numberToMoney(player.nPoint.getPowerNextLimit()),
                                    "Nâng\ngiới hạn\nsức mạnh",
                                    "Nâng ngay\n" + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) + " vàng",
                                    "Đóng");
                        } else {
                            this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                    "Sức mạnh của con đã đạt tới giới hạn", "Đóng");
                        }
                        break;
                    case 1: // Đệ tử
                        if (player.pet != null) {
                            if (player.pet.nPoint.limitPower < NPoint.MAX_LIMIT) {
                                this.createOtherMenu(player, ConstNpc.OPEN_POWER_PET,
                                        "Ta sẽ truyền năng lượng giúp con mở giới hạn sức mạnh của đệ tử lên "
                                                + Util.numberToMoney(player.pet.nPoint.getPowerNextLimit()),
                                        "Nâng ngay\n" + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) + " vàng", "Đóng");
                            } else {
                                this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                        "Sức mạnh của đệ con đã đạt tới giới hạn", "Đóng");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Không thể thực hiện");
                        }
                        break;
                }
                break;

            case ConstNpc.OPEN_POWER_MYSEFT:
                switch (select) {
                    case 0:
                        OpenPowerService.gI().openPowerBasic(player);
                        break;
                    case 1:
                        if (player.inventory.gold >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
                            if (OpenPowerService.gI().openPowerSpeed(player)) {
                                player.inventory.gold -= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER;
                                Service.gI().sendMoney(player);
                            }
                        } else {
                            Service.gI().sendThongBao(player,
                                    "Bạn không đủ vàng để mở, còn thiếu "
                                            + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER - player.inventory.gold) + " vàng");
                        }
                        break;
                }
                break;

            case ConstNpc.OPEN_POWER_PET:
                if (select == 0 && player.pet != null) {
                    if (player.inventory.gold >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
                        if (OpenPowerService.gI().openPowerSpeed(player.pet)) {
                            player.inventory.gold -= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER;
                            Service.gI().sendMoney(player);
                        }
                    } else {
                        Service.gI().sendThongBao(player,
                                "Bạn không đủ vàng để mở, còn thiếu "
                                        + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER - player.inventory.gold) + " vàng");
                    }
                }
                break;
        }
    }
}