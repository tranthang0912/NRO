package services.func;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import player.Player;
class TradeTest {
    @Test void onePlayerCannotConfirmTwiceOrChangeAnOfferAfterLocking(){
        Player one=new Player(),two=new Player();one.id=1001;two.id=1002;one.inventory.gold=100;two.inventory.gold=100;
        Trade trade=new Trade(one,two);
        trade.acceptTrade(one);assertEquals(0,trade.accept);
        trade.lockTran(one);trade.lockTran(two);
        trade.acceptTrade(one);trade.acceptTrade(one);assertEquals(1,trade.accept);
        trade.addItemTrade(one,(byte)-1,50);
        assertEquals(0,((Number)trade.webView(one,true).get("gold")).intValue());
        trade.dispose();
    }
}
