package server;

import services.shenron.Shenron_Manager;
import kygui.ConsignShopManager;
import database.HistoryTransactionDAO;
import boss.BossManager.BossManager;
import boss.BossManager.OtherBossManager;
import boss.BossManager.TreasureUnderSeaManager;
import boss.BossManager.SnakeWayManager;
import boss.BossManager.RedRibbonHQManager;
import boss.BossManager.GasDestroyManager;
import boss.BossManager.YardartManager;
import boss.BossManager.SkillSummonedManager;
import network.inetwork.ISession;
import network.Network;
import network.MyKeyHandler;
import network.MySession;
import Deputyhead.Service.NgocRongNamecService;
import utils.Logger;
import utils.TimeUtil;

import java.util.*;
import matches.tournament.The23rdMartialArtCongressManager;
import matches.tournament.DeathOrAliveArenaManager;
import matches.tournament.WorldMartialArtsTournamentManager;
import network.MessageSendCollect;
import managers.SuperRankManager;
import network.inetwork.ISessionAcceptHandler;
import boss.BossManager.BrolyManager;
import boss.BossManager.FinalBossManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import database.DatabaseManager;
import database.PlayerDAO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import network.Message;
import network.SessionManager;
import player.Player;
import player.Service.ClanService;
import utils.SystemMetrics;

public class ServerManager {

    public static String timeStart;

    public static final Map<Object, Object> CLIENTS = new HashMap<>();
    public static String NAME_SERVER = "NRO 1999"; // Tên Máy Chủ
    public static String DOMAIN = "https://nro1999.online/"; // Domain Truy Cập
    public static String NAME = "NRO 1999"; // Name Khi Vào Giao Diện Game
    public static String IP = "NgocRongOnline"; // IPs - Không Cần Sửa
    private static final int HTTP_PORT = 8080;
    private static final int MAX_REQ_PER_MINUTE = 20;
    private static final int BLACKLIST_TIME_SEC = 60;
    public static int PORT = 14445; // Port
    public static int EVENT_SEVER = 0;

    private static ServerManager instance;

    public static boolean isRunning;

    public void init() {
        Manager.gI();
    }

    public static ServerManager gI() {
        if (instance == null) {
            instance = new ServerManager();
            instance.init();
        }
        return instance;
    }

    public static void main(String[] args) {
        try { web.WebServer.main(args); }
        catch (Exception e) { e.printStackTrace(); System.exit(1); }
    }

   public void run() {
    isRunning = true;
    activeServerSocket();
        new Thread(NgocRongNamecService.gI(), "Update NRNM").start();
        new Thread(SuperRankManager.gI(), "Update Super Rank").start();
        new Thread(The23rdMartialArtCongressManager.gI(), "Update DHVT23").start();
        new Thread(DeathOrAliveArenaManager.gI(), "Update Võ Đài Sinh Tử").start();
        new Thread(WorldMartialArtsTournamentManager.gI(), "Update WMAT").start();
        new Thread(Shenron_Manager.gI(), "Update Shenron").start();
        BossManager.gI().loadBoss();
        Manager.MAPS.forEach(map.Map::initBoss);
        new Thread(BossManager.gI(), "Update boss").start();
        // new Thread(YardartManager.gI(), "Update yardart boss").start();
        new Thread(FinalBossManager.gI(), "Update final boss").start();
        new Thread(SkillSummonedManager.gI(), "Update Skill-summoned boss").start();
        new Thread(BrolyManager.gI(), "Update broly boss").start();
        new Thread(OtherBossManager.gI(), "Update other boss").start();
        // new Thread(RedRibbonHQManager.gI(), "Update reb ribbon hq boss").start();
        new Thread(TreasureUnderSeaManager.gI(), "Update treasure under sea boss").start();
        // new Thread(SnakeWayManager.gI(), "Update snake way boss").start();
        // new Thread(GasDestroyManager.gI(), "Update gas destroy boss").start();
    }

    
    public void activeServerSocket() {
        try {
            Network.gI().init().setAcceptHandler(new ISessionAcceptHandler() {
                @Override
                public void sessionInit(ISession is) {
                    if (!canConnectWithIp(is.getIP())) {
                        is.disconnect();
                        return;
                    }
                    is.setMessageHandler(Controller.gI())
                            .setSendCollect(new MessageSendCollect())
                            .setKeyHandler(new MyKeyHandler())
                            .startCollect();
                }

                @Override
                public void sessionDisconnect(ISession session) {
                    Client.gI().kickSession((MySession) session);
                    disconnect((MySession) session);
                }
            }).setTypeSessioClone(MySession.class)
              .setDoSomeThingWhenClose(() -> {
                  Logger.error("SERVER CLOSE\n");
                  System.exit(0);
              })
              .start(PORT);
        } catch (Exception e) {
            Logger.error("Lỗi khi khởi động máy chủ: " + e.getMessage());
        }
    }
    private boolean canConnectWithIp(String ipAddress) {
        Object o = CLIENTS.get(ipAddress);
        if (o == null) {
            CLIENTS.put(ipAddress, 1);
            return true;
        } else {
            int n = Integer.parseInt(String.valueOf(o));
            if (n < Manager.MAX_PER_IP) {
                n++;
                CLIENTS.put(ipAddress, n);
                return true;
            } else {
                return false;
            }
        }
    }
    public void disconnect(MySession session) {
        Object o = CLIENTS.get(session.getIP());
        if (o != null) {
            int n = Integer.parseInt(String.valueOf(o));
            n--;
            if (n < 0) {
                n = 0;
            }
            CLIENTS.put(session.getIP(), n);
        }
    }
    public void close() {
        isRunning = false;
        try {
            ClanService.gI().close();
        } catch (Exception e) {
            Logger.error("Lỗi save clan!\n");
        }
        try {
            ConsignShopManager.gI().save();
        } catch (Exception e) {
            Logger.error("Lỗi save shop ký gửi!\n");
        }
        Client.gI().close();
        Logger.success("SUCCESSFULLY MAINTENANCE!\n");
        System.exit(0);
    }
}
