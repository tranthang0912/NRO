package network;

import database.DatabaseManager;
import network.inetwork.IServerClose;
import java.net.Socket;
import java.io.IOException;
import java.net.InetSocketAddress;
import network.inetwork.ISession;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import network.inetwork.ISessionAcceptHandler;
import network.inetwork.INetwork;
import utils.Logger;
import static database.DatabaseManager.DB_NAME;
import server.ServerManager;
public class Network implements INetwork, Runnable {

    private static Network instance;
    private int port = -1;
    private ServerSocketChannel serverSocketChannel;
    private Class sessionClone = Session.class;
    private boolean start;
    private boolean randomKey;
    private IServerClose serverClose;
    private ISessionAcceptHandler acceptHandler;
    private Thread loopServer;
    private Selector selector;

    public static Network gI() {
        if (instance == null) {
            instance = new Network();
        }
        return instance;
    }

    private Network() {
    }

    @Override
    public INetwork init() {
        try {
            this.selector = Selector.open();
        } catch (IOException ex) {
            Logger.error(ex.toString());
        }
        this.loopServer = new Thread((Runnable) this, "Network");
        return this;
    }

    @Override
    public INetwork start(int port) throws Exception {
        if (port < 0) {
            throw new Exception("Please initialize the server port!");
        }
        if (this.acceptHandler == null) {
            throw new Exception("AcceptHandler has not been initialized!");
        }
        if (!ISession.class.isAssignableFrom(this.sessionClone)) {
            throw new Exception("The session clone type is invalid!");
        }
        try {
            this.port = port;
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.configureBlocking(false);
            this.serverSocketChannel.socket().bind(new InetSocketAddress(port));
            this.serverSocketChannel.register(this.selector, 16);
        } catch (IOException ex) {
            Logger.error("Error initializing server at port " + port + "\n");
            System.exit(0);
        }
        this.start = true;
        this.loopServer.start();
        Logger.success("Server initialized and listening on port " + this.port + "\n");
        return this;
    }

    @Override
    public INetwork close() {
        this.start = false;
        if (this.serverSocketChannel != null) {
            try {
                this.serverSocketChannel.close();
            } catch (IOException iOException) {
            }
        }
        if (this.serverClose != null) {
            this.serverClose.serverClose();
        }
        return this;
    }

    @Override
    public INetwork dispose() {
        this.acceptHandler = null;
        this.loopServer = null;
        this.serverSocketChannel = null;
        return this;
    }

    @Override
    public INetwork setAcceptHandler(ISessionAcceptHandler handler) {
        this.acceptHandler = handler;
        return this;
    }

    @Override
    public void run() {
        while (this.start) {
            try {
                this.selector.select();
                for (SelectionKey key : this.selector.selectedKeys()) {
                    if (!key.isAcceptable()) {
                        continue;
                    }
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    Socket socket = server.accept().socket();
                    ISession session = SessionFactory.gI().cloneSession(this.sessionClone, socket);
                    this.acceptHandler.sessionInit(session);
                    SessionManager.gI().putSession(session);
                }
                this.selector.selectedKeys().clear();
            } catch (IOException iOException) {
            } catch (Exception ex2) {
                Logger.error(ex2.toString());
            }
        }
    }

    @Override
    public INetwork setDoSomeThingWhenClose(IServerClose serverClose) {
        this.serverClose = serverClose;
        return this;
    }

    @Override
    public INetwork randomKey(boolean isRandom) {
        this.randomKey = isRandom;
        return this;
    }

    @Override
    public boolean isRandomKey() {
        return this.randomKey;
    }

    @Override
    public INetwork setTypeSessioClone(Class clazz) throws Exception {
        this.sessionClone = clazz;
        return this;
    }

    @Override
    public ISessionAcceptHandler getAcceptHandler() throws Exception {
        if (this.acceptHandler == null) {
            throw new Exception("AcceptHandler has not been initialized!");
        }
        return this.acceptHandler;
    }

    @Override
    public void stopConnect() {
        this.start = false;
    }
}
