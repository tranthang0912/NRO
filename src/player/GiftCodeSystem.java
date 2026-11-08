package player;
import item.Item.ItemOption;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import player.Player;

public class GiftCodeSystem {

    public String code;
    public int countLeft;
    public int id;
    public HashMap<Integer, Integer> detail = new HashMap<>();
    public HashMap<Integer, ArrayList<ItemOption>> option = new HashMap<>();
    public Timestamp datecreate;
    public Timestamp dateexpired;

    public boolean isUsedGiftCode(Player player) {
        return player.giftCode.isUsedGiftCode(code);
    }

    public boolean timeCode() {
        return this.datecreate.getTime() > this.dateexpired.getTime();
    }
}
