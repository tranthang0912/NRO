package boss.The23rdMartialArtCongress;


import boss.BossID;
import boss.BossesData;
import static consts.BossType.PHOBAN;
import player.Player;

public class PonPut extends The23rdMartialArtCongress {

    public PonPut(Player player) throws Exception {
        super(PHOBAN, BossID.PON_PUT, BossesData.PON_PUT);
        this.playerAtt = player;
    }
}
