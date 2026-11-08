package boss.The23rdMartialArtCongress;


import boss.BossID;
import boss.BossesData;
import static consts.BossType.PHOBAN;
import player.Player;

public class Pocolo extends The23rdMartialArtCongress {

    public Pocolo(Player player) throws Exception {
        super(PHOBAN, BossID.PO_CO_LO, BossesData.POCOLO);
        this.playerAtt = player;
    }
}
