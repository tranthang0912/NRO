package npc.list;import npc.Npc;
import player.Player;
import map.Service.ChangeMapService;
import utils.Util;

import java.time.LocalTime;
import java.util.Calendar;
import services.Service;
import utils.TimeUtil;

public class Tapion extends Npc {

    public Tapion(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (mapId == 19) {
                this.createOtherMenu(player, 0, "Ác quỷ truyền thuyết Hirudegarn\nđã thoát khỏi phong ấn ngàn năm\nHãy giúp tôi chế ngự nó", "OK", "Từ chối");
            } else if (mapId == 126) {
                this.createOtherMenu(player, 0, "Tôi sẽ đưa bạn về", "OK", "Từ chối");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (select) {
            case 0 -> {
            if (mapId == 19) {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int second = calendar.get(Calendar.SECOND);
            if ((hour == 22) || (hour == 23 && minute == 0 && second == 0)) {
            ChangeMapService.gI().changeMapNonSpaceship(player, 126, 200 + Util.nextInt(-100, 100), 360);
            } else {
            Service.gI().sendThongBao(player, "Vui lòng quay lại vào lúc 22h đến trước 23h");
            }
            } else if (mapId == 126) {
            ChangeMapService.gI().changeMapNonSpaceship(player, 19, 1000 + Util.nextInt(-100, 100), 360);
          }
        }
      }
    }
  }  
}