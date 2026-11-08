package player;

import lombok.Getter;
import lombok.Setter;
import player.Player;

/**
 * @author outcast c-cute hột me 😳
 */
@Setter
@Getter
public class PlayerEvent {
    private int eventPoint;
    private Player player;

    public PlayerEvent(Player player) {
        this.player = player;
    }
    
    public void addEventPoint(int num) {
        eventPoint += num;
    }
    
    public void subEventPoint(int num) {
        eventPoint -= num;
    }

    public void update() {
       
    }

}
