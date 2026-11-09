package web;

import com.google.gson.*;
import io.undertow.websockets.core.*;
import java.io.*;
import java.nio.ByteBuffer;
import network.Message;
import network.MySession;

/** Adapts existing NPC and service UI packets to browser dialogs. */
public final class WebSession extends MySession {
    final WebSocketChannel channel;
    final GameWorld world;
    volatile boolean connected = true;
    boolean authenticated;
    boolean intentionalClose;
    long detachedUntil;
    String resumeToken;
    int sentMap = -1;
    long lastSeen = System.currentTimeMillis();
    long lastChat;
    long lastAction;
    int direction;
    int vertical;
    double velocityY;
    int facing = 1;
    boolean jump;
    boolean busySending;
    int requestCount;
    long requestWindow = System.currentTimeMillis();
    int menuNpc = -1;
    int menuChoices;
    int inputCount;
    int inputType;
    byte[] confirmItem;
    boolean combineOpen;
    int[] confirmSell;
    private final java.util.concurrent.atomic.AtomicInteger pendingSends=new java.util.concurrent.atomic.AtomicInteger();
    int incomingTrade=-1;
    int tradePeer=-1;
    boolean tradeLocked;
    public WebSession(GameWorld world, WebSocketChannel channel, String ip) {
        super(ip); this.world = world; this.channel = channel;
        this.version = 231; this.zoomLevel = 1; this.typeClient = 1;
        this.finishUpdate = true; this.timeWait = 0;
    }
    @Override public boolean isConnected() { return connected; }
    @Override public void disconnect() {
        connected = false;
        try { if (channel.isOpen()) WebSockets.sendClose(1000, "Đã thoát game", channel, null); }
        catch (Exception ignored) {}
    }
    @Override public void dispose() { disconnect(); }
    void send(Object data) {
        boolean frame=data instanceof java.util.Map<?,?> map&&"state".equals(map.get("type"));
        if(frame&&pendingSends.get()>=2)return;
        sendJson(GameWorld.JSON.toJson(data),frame);
    }
    void sendJson(String json,boolean replaceable){
        if(!connected||!channel.isOpen()||(replaceable&&pendingSends.get()>=2))return;
        if(pendingSends.get()>=64){world.disconnected(this);try{channel.close();}catch(IOException ignored){}return;}
        pendingSends.incrementAndGet();
        WebSockets.sendText(json,channel,new WebSocketCallback<Void>(){
            public void complete(WebSocketChannel c,Void context){pendingSends.decrementAndGet();}
            public void onError(WebSocketChannel c,Void context,Throwable error){pendingSends.decrementAndGet();world.disconnected(WebSession.this);}
        });
    }
    void copyPlayerSession(WebSession old) {
        this.player=old.player; this.userId=old.userId; this.uu=old.uu;
        this.isAdmin=old.isAdmin; this.actived=old.actived; this.goldBar=old.goldBar; this.gold=old.gold;
        this.eventPoint=old.eventPoint; this.itemsReward=old.itemsReward; this.dataReward=old.dataReward;
        this.is_gift_box=old.is_gift_box; this.bdPlayer=old.bdPlayer; this.vip=old.vip; this.luotquay=old.luotquay;
        this.authenticated=true; this.joinedGame=true; this.sentMap=-1; this.facing=old.facing;
        this.player.setSession(this);
    }
    void notice(String text) { send(java.util.Map.of("type", "notice", "text", java.util.Objects.toString(text,"Thao tác chưa thực hiện được."))); }
    @Override public void sendMessage(Message msg) {
        if (!connected || msg == null) return;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(msg.getData()))) {
            switch (msg.command) {
                case -25, -26 -> notice(in.readUTF());
                case 38 -> {
                    int npc = in.readShort();
                    String text = in.readUTF();
                    int portrait = in.available() >= 2 ? in.readShort() : -1;
                    send(java.util.Map.of("type", "tutorial", "npc", npc, "text", text, "portrait", portrait));
                }
                case 124 -> {
                    int npc = in.readShort();
                    send(java.util.Map.of("type", "npcChat", "npc", npc, "text", in.readUTF()));
                }
                case -34 -> {
                    int action = in.readUnsignedByte();
                    if (action == 0) {
                        int icon = in.readShort();
                        String name = in.readUTF();
                        int x = in.readShort(), y = in.readShort();
                        in.readByte(); // Tree level is included in its name.
                        int peas = in.readUnsignedShort(), capacity = in.readUnsignedShort();
                        in.readUTF(); in.readInt(); // Status text and timer.
                        JsonArray offsets = new JsonArray();
                        int size = in.readUnsignedByte();
                        for (int i = 0; i < size; i++) {
                            offsets.add(GameWorld.JSON.toJsonTree(new int[]{in.readByte(), in.readByte()}));
                        }
                        send(java.util.Map.of("type", "tree", "icon", icon, "name", name, "x", x, "y", y,
                            "peas", peas, "capacity", capacity, "offsets", offsets));
                    } else if (action == 1) {
                        menuNpc = consts.ConstNpc.DAU_THAN;
                        JsonArray choices = new JsonArray();
                        while (in.available() > 0) choices.add(in.readUTF());
                        menuChoices = choices.size();
                        send(java.util.Map.of("type", "menu", "npc", menuNpc, "text", "Đậu thần", "choices", choices));
                    } else if (action == 2 && player != null) {
                        send(java.util.Map.of("type", "treeHarvest", "peas", in.readUnsignedShort()));
                        send(PlayerView.inventory(player));
                    }
                }
                case 32 -> {
                    menuNpc = in.readShort(); String text = in.readUTF();
                    menuChoices = in.readUnsignedByte();
                    JsonArray choices = new JsonArray();
                    for (int i = 0; i < menuChoices; i++) choices.add(in.readUTF());
                    send(java.util.Map.of("type", "menu", "npc", menuNpc, "text", text, "choices", choices));
                }
                case -125 -> {
                    String title = in.readUTF(); inputCount = in.readUnsignedByte();
                    JsonArray fields = new JsonArray();
                    for (int i = 0; i < inputCount; i++) fields.add(GameWorld.JSON.toJsonTree(java.util.Map.of("label", in.readUTF(), "kind", in.readUnsignedByte())));
                    send(java.util.Map.of("type", "input", "title", title, "fields", fields));
                }
                case -43 -> {
                    confirmItem = new byte[]{in.readByte(), in.readByte(), in.readByte()};
                    send(java.util.Map.of("type", "confirmItem", "text", in.readUTF()));
                }
                case -44 -> {
                    if (player == null) return;
                    var shop=player.idMark.getShopOpen();
                    if(shop==null)return;
                    java.util.List<Object> tabs=new java.util.ArrayList<>();
                    for(var tab:shop.tabShops){
                        java.util.List<Object> items=new java.util.ArrayList<>();
                        for(var item:tab.itemShops)items.add(java.util.Map.of("id",item.temp.id,"gold",item.typeSell==0?item.cost:0,"gem",item.typeSell!=0?item.cost:0,"currency",item.typeSell));
                        tabs.add(java.util.Map.of("name",tab.name,"items",items));
                    }
                    send(java.util.Map.of("type","shop","tabs",tabs));
                }
                case -81 -> {
                    int action=in.readUnsignedByte();
                    if(action==0){combineOpen=true;send(java.util.Map.of("type","combine","text",in.readUTF(),"title",in.readUTF(),"bag",PlayerView.items(player.inventory.itemsBag)));}
                    else if(action==1){send(PlayerView.inventory(player));}
                    else if(action==2||action==3){notice(action==2?"Nâng cấp thành công!":"Nâng cấp thất bại.");send(PlayerView.inventory(player));}
                }
                case 7 -> {
                    confirmSell=new int[]{in.readUnsignedByte(),in.readShort()};
                    send(java.util.Map.of("type","confirmSell","text",in.readUTF()));
                }
                case -91 -> {
                    int count=in.readUnsignedByte();java.util.List<Object> maps=new java.util.ArrayList<>();
                    for(int i=0;i<count;i++)maps.add(java.util.Map.of("index",i,"name",in.readUTF(),"planet",in.readUTF()));
                    send(java.util.Map.of("type","travel","maps",maps));
                }
                case -86 -> {
                    int action=in.readUnsignedByte();
                    if(action==0){incomingTrade=in.readInt();send(java.util.Map.of("type","tradeInvite","peer",incomingTrade));}
                    else if(action==1){tradePeer=in.readInt();tradeLocked=false;direction=vertical=0;send(java.util.Map.of("type","tradeOpen","peer",tradePeer));}
                    else if(action==6){send(java.util.Map.of("type","tradePeerLocked"));}
                    else if(action==7){tradePeer=-1;tradeLocked=false;send(java.util.Map.of("type","tradeClosed"));if(player!=null)send(PlayerView.inventory(player));}
                }
                default -> { /* World state is serialized directly, without binary image downloads. */ }
            }
        } catch (Exception ignored) { /* Other legacy packets have their own browser state representation. */ }
    }
    boolean allowRequest() {
        long now = System.currentTimeMillis();
        if (now - requestWindow >= 1000) { requestWindow = now; requestCount = 0; }
        return ++requestCount <= 40;
    }
}
