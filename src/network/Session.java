package network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import network.inetwork.IKeySessionHandler;
import network.inetwork.IMessageHandler;
import network.inetwork.IMessageSendCollect;
import network.inetwork.ISession;
import utils.StringUtil;

public class Session
implements ISession {
    private static ISession instance;
    private static int ID_INIT;
    private byte[] KEYS = "NRO".getBytes();
    private boolean sentKey;
    public int id = ID_INIT++;
    private Socket socket;
    private boolean connected;
    private Sender sender;
    private Collector collector;
    private final Thread tSender;
    private final Thread tCollector;
    private IKeySessionHandler keyHandler;
    private String ip;

    /** Browser transport: no TCP socket, collector or sender thread. */
    protected Session(String ip) {
        this.ip = ip;
        this.connected = true;
        this.tSender = null;
        this.tCollector = null;
    }

    public static ISession gI() throws Exception {
        if (instance == null) {
            throw new Exception("Instance has not been initialized!");
        }
        return instance;
    }
    public Session(Socket socket) {
        this.socket = socket;
        try {
            this.socket.setSendBufferSize(0x100000);
            this.socket.setReceiveBufferSize(0x100000);
        } catch (SocketException socketException) {
        }
        this.connected = true;
        this.ip = ((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress().toString().replace("/", "");
        this.sender = this.sender != null ? this.sender.setSocket(this.socket) : new Sender(this, this.socket);
        this.collector = this.collector != null ? this.collector.setSocket(this.socket) : new Collector(this, this.socket);
        this.tSender = new Thread((Runnable)(this.sender != null ? this.sender.setSocket(this.socket) : (this.sender = new Sender(this, this.socket))), "Sender - IP : " + this.ip);
        this.tCollector = new Thread((Runnable)(this.collector != null ? this.collector.setSocket(this.socket) : (this.collector = new Collector(this, this.socket))), "Collector - IP : " + this.ip);
    }

    @Override
    public void sendMessage(Message msg) {
        if (this.isConnected() && msg != null) {
            this.sender.sendMessage(msg);
        }
    }

    @Override
    public ISession setSendCollect(IMessageSendCollect collect) {
        this.sender.setSend(collect);
        this.collector.setCollect(collect);
        return this;
    }

    @Override
    public ISession setMessageHandler(IMessageHandler handler) {
        this.collector.setMessageHandler(handler);
        return this;
    }

    @Override
    public ISession setKeyHandler(IKeySessionHandler handler) {
        this.keyHandler = handler;
        return this;
    }

    @Override
    public ISession startSend() {
        this.tSender.start();
        return this;
    }

    @Override
    public ISession startCollect() {
        this.tCollector.start();
        return this;
    }

    @Override
    public String getIP() {
        return this.ip;
    }

    @Override
    public long getID() {
        return this.id;
    }

    @Override
    public void disconnect() {
        this.connected = false;
        this.sentKey = false;
        if (this.sender != null) {
            this.sender.close();
        }
        if (this.collector != null) {
            this.collector.close();
        }
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException iOException) {
                // empty catch block
            }
        }
        this.dispose();
    }

    @Override
    public void dispose() {

        if (this.sender != null) {
            this.sender.dispose();
        }
        if (this.collector != null) {
            this.collector.dispose();
        }
        if (this.tSender != null && this.tSender.isAlive()) {
            this.tSender.interrupt();
        }
        if (this.tCollector != null && this.tCollector.isAlive()) {
            this.tCollector.interrupt();
        }
        this.socket = null;
        this.sender = null;
        this.collector = null;
        this.ip = null;
        SessionManager.gI().removeSession(this);
    }

    @Override
    public void sendKey() throws Exception {
        if (this.keyHandler == null) {
            throw new Exception("Key handler has not been initialized!");
        }
        if (Network.gI().isRandomKey()) {
            this.KEYS = StringUtil.randomText(7).getBytes();
        }
        this.keyHandler.sendKey(this);
    }

    @Override
    public boolean sentKey() {
        return this.sentKey;
    }

    @Override
    public void setSentKey(boolean sent) {
        this.sentKey = sent;
    }

    @Override
    public void doSendMessage(Message msg) throws Exception {
        this.sender.doSendMessage(msg);
    }

    @Override
    public ISession start() {
        this.tSender.start();
        this.tCollector.start();
        return this;
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public byte[] getKey() {
        return this.KEYS;
    }

    @Override
    public int getNumMessages() {
        if (this.isConnected()) {
            return this.sender.getNumMessages();
        }
        return -1;
    }
}

