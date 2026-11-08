
package network.inetwork;

import network.Message;

public interface IMessageHandler {
    public void onMessage(ISession var1, Message var2) throws Exception;
}

