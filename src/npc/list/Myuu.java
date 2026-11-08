package npc.list;
/*
 * @Author Coder: Nguyễn Tấn Tài
 * @Description: Ngọc Rồng Kiwi - Máy Chủ Chuẩn Teamobi 2025
 * @Group Zalo: https://zalo.me/g/toiyeuvietnam2025
 */
import consts.ConstNpc;
import java.util.ArrayList;
import npc.Npc;
import player.Player;
import services.ItemTimeService;
import map.Service.NpcService;
import services.Service;
import services.TaskService;
import map.Service.ChangeMapService;
import shop.ShopService;
import utils.TimeUtil;
import utils.Util;
public class Myuu extends Npc {

    public Myuu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 20) {
                createOtherMenu(player, ConstNpc.IGNORE_MENU, "Năm 740, ta tìm thấy kí sinh trùng của King Tuffle,\n" +
                        "sau đó ta đã nghiên cứu và chế tạo ra kí sinh trùng Baby.\n" +
                        "Baby có khả năng bám vào cơ thể của người khác,\n" +
                        "kiểm soát sức mạnh của họ và làm việc theo ý của ta.\n" +
                        "tuy nhiên ta đã mất kiểm soát nó hoàn toàn...\n" +
                        "ngươi có thể giúp ta chế ngự nó không ?", "Đi Thôi", "Đóng");
            } else if (this.mapId == 164) {
                createOtherMenu(player, ConstNpc.IGNORE_MENU, "Ngươi muốn về hả ?", "Quay Về", "Đóng");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 20) {
                switch (select) {
                    case 0:
                        ChangeMapService.gI().changeMapNonSpaceship(player, 164, 145, 240);
                        break;
                }

            } else if (this.mapId == 164) {
                switch (select) {
                    case 0:
                        ChangeMapService.gI().changeMapNonSpaceship(player, 20, 1100, 360);
                        break;
                }
            }
        }
    }
}
