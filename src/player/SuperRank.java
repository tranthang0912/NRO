package player;
import java.util.ArrayList;
import java.util.List;
import matches.The23rdMartialArtCongress.SuperRankService;
import services.Service;
import map.Service.NpcService;

public class SuperRank {

    private Player player;
    public int rank;
    public int win;
    public int lose;
    public List<String> history;
    public List<Long> lastTime;
    public long lastPKTime;
    public long lastRewardTime;
    public int ticket = 3;

    public SuperRank(Player player) {
        this.player = player;
        this.history = new ArrayList<>();
        this.lastTime = new ArrayList<>();
    }

    public void history(String text, long lastTime) {
        if (this.history.size() > 4) {
        }
        this.history.add(text);
        this.lastTime.add(lastTime);
    }

    public void reward() {
        int rw = SuperRankService.gI().reward(rank);
        if (rw != -1) {
          Service.gI().sendThongBao(player, "Bạn đang ở TOP " + rank + " võ đài Siêu Hạng, được bú " + rw + " ngọc xanh");
            player.inventory.gem += rw;
        }
        lastRewardTime = System.currentTimeMillis();
    }

    public void dispose() {
        history.clear();
        lastTime.clear();
        win = -1;
        lose = -1;
        lastPKTime = -1;
        player = null;
    }
}
