package boss.The23rdMartialArtCongress;


import boss.BossID;
import boss.BossesData;
import static consts.BossType.PHOBAN;
import player.Player;

public class SoiHecQuyn extends The23rdMartialArtCongress {

    public SoiHecQuyn(Player player) throws Exception {
        super(PHOBAN, BossID.SOI_HEC_QUYN, BossesData.SOI_HEC_QUYN);
        this.playerAtt = player;
    }
}
