package matches.The23rdMartialArtCongress;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SuperRankBuilder {

    private long id;
    private int rank;
    private long lastPKTime;
    private long lastTimeReward;
    private int ticket;
    private int win;
    private int lose;
    private String info;

    private int head;
    private int body;
    private int leg;
    private String name;

    public void dispose() {
        name = null;
        info = null;
    }
}