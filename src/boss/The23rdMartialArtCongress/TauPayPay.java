package boss.The23rdMartialArtCongress;


import boss.BossID;
import boss.BossesData;
import static consts.BossType.PHOBAN;
import player.Player;

public class TauPayPay extends The23rdMartialArtCongress {

    public TauPayPay(Player player) throws Exception {
        super(PHOBAN, BossID.TAU_PAY_PAY, BossesData.TAU_PAY_PAY);
        this.playerAtt = player;
    }
}
