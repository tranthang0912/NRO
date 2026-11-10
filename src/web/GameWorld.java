package web;

import com.google.gson.*;
import database.*;
import io.undertow.websockets.core.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import map.Service.*;
import mob.Mob;
import network.Message;
import player.Player;
import player.Service.*;
import server.*;
import services.*;
import services.func.*;
import skill.Skill;
import boss.BossManager.*;

/** One authoritative game loop. Devices share the same Player/Zone instances. */
public final class GameWorld implements AutoCloseable {
    private static volatile GameWorld activeWorld;
    private static final ScheduledExecutorService fallbackTimers=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"game-timers");t.setDaemon(true);return t;});
    public static void schedule(Runnable task,long delay){GameWorld active=activeWorld;if(active!=null&&!active.stopping)active.loop.schedule(task,delay,TimeUnit.MILLISECONDS);else fallbackTimers.schedule(task,delay,TimeUnit.MILLISECONDS);}
    static final Gson JSON = new GsonBuilder().disableHtmlEscaping().create();
    private final ScheduledThreadPoolExecutor loop = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "game-world"));
    private final AuthService auth = new AuthService();
    private final Set<WebSession> sessions = new HashSet<>();
    private final Map<String,WebSession> resumeTokens = new HashMap<>();
    private final Map<String, long[]> authAttempts = new HashMap<>();
    private final Set<String> authenticating = new HashSet<>();
    private final Map<String, Long> recentlyLoggedOut = new HashMap<>();
    private final ArrayBlockingQueue<Runnable> commands = new ArrayBlockingQueue<>(256);
    private long ticks;
    volatile long lastTick = System.currentTimeMillis();
    volatile int online;
    volatile boolean ready;
    private volatile boolean stopping;
    private final List<BossManager> bossManagers;

    public GameWorld() {
        activeWorld=this;
        bossManagers=List.of(BossManager.gI(),BrolyManager.gI(),FinalBossManager.gI(),OtherBossManager.gI(),SkillSummonedManager.gI(),TreasureUnderSeaManager.gI(),SnakeWayManager.gI(),RedRibbonHQManager.gI(),GasDestroyManager.gI(),YardartManager.gI());
        BossManager.gI().loadBoss();Manager.MAPS.forEach(map.Map::initBoss);
        loop.setRemoveOnCancelPolicy(true);
        ready = true;
        loop.scheduleAtFixedRate(this::tickSafely, 0, WebConfig.TICK_MS, TimeUnit.MILLISECONDS);
    }
    boolean enqueue(Runnable r) { return !stopping && commands.offer(r); }
    void connected(WebSession s) {
        if (!enqueue(() -> {
            if (sessions.size() >= 16) { s.notice("Máy chủ đủ kết nối, hãy thử lại sau."); s.disconnect(); return; }
            sessions.add(s); s.send(Map.of("type","hello","maxPlayers",WebConfig.MAX_PLAYERS,"online",online));
        })) s.disconnect();
    }
    void disconnected(WebSession s) { enqueue(() -> {
        if(!sessions.contains(s))return;
        if(s.authenticated&&!s.intentionalClose){
            if(s.player!=null)TransactionService.gI().cancelTrade(s.player);
            s.connected=false;s.direction=0;s.vertical=0;
            if(s.detachedUntil==0)s.detachedUntil=System.currentTimeMillis()+45000;
        }else remove(s);
    }); }
    private void remove(WebSession s) {
        if (!sessions.remove(s)) return;
        s.intentionalClose=true;
        if(s.resumeToken!=null)resumeTokens.remove(s.resumeToken);
        if (s.player != null) {
            long playerId=s.player.id;
            Client.gI().kickSession(s);
            recentlyLoggedOut.put(s.uu, playerId);
            s.player=null;
        }
        s.disconnect();
        online=(int)sessions.stream().filter(x->x.authenticated&&x.player!=null).count();
    }
    void receive(WebSession s, String text) {
        if (!s.allowRequest()) { s.notice("Thao tác quá nhanh."); return; }
        try {
            JsonObject data=JsonParser.parseString(text).getAsJsonObject();
            if (!enqueue(() -> {
                if (!s.connected || !sessions.contains(s)) return;
                s.lastSeen=System.currentTimeMillis();
                try { action(s,data); }
                catch (IllegalArgumentException e) { s.notice(e.getMessage()==null?"Dữ liệu không hợp lệ.":e.getMessage()); }
                catch (Exception e) { System.err.println("Game action failed: "+e.getClass().getSimpleName()+": "+e.getMessage()); s.notice("Thao tác chưa thực hiện được, hãy thử lại."); }
            })) s.disconnect();
        } catch (Exception e) { s.notice("Dữ liệu không hợp lệ."); }
    }
    private void action(WebSession s, JsonObject d) throws Exception {
        String type=string(d,"type",32);
        if(type.equals("ping")){s.send(Map.of("type","pong","time",System.currentTimeMillis()));return;}
        if(type.equals("resume")){resume(s,string(d,"token",100));return;}
        if(type.equals("login")||type.equals("register")) { login(s,d,type.equals("register"));return; }
        Player p=s.player;
        if(!s.authenticated||p==null) throw new IllegalArgumentException("Hãy đăng nhập trước.");
        if (type.equals("automation")) {
            s.automation.setMode(string(d, "mode", 12));
            return;
        }
        if(TransactionService.gI().check(p)&&!Set.of("trade","tradeView","chat","inventory","logout","move").contains(type))throw new IllegalArgumentException("Hãy kết thúc giao dịch trước.");
        if (s.automation.active() && !Set.of("move", "chat", "inventory", "clans", "tradeView").contains(type)) {
            s.automation.stop("Đã tắt Auto khi bạn thao tác trực tiếp.");
        }
        switch(type) {
            case "logout" -> remove(s);
            case "move" -> {
                int direction = bounded(d, "x", -1, 1);
                int vertical = bounded(d, "y", -1, 1);
                boolean jump = d.has("jump") && d.get("jump").getAsBoolean();
                if (s.automation.active()) {
                    if (direction == 0 && vertical == 0 && !jump) return;
                    s.automation.stop("Đã tắt Auto khi bạn điều khiển nhân vật.");
                }
                if (direction != s.facing) s.autoPortalAttempt = null;
                s.direction = direction;
                s.vertical = vertical;
                s.jump = jump;
                if (direction != 0) s.facing = direction;
            }
            case "attack" -> attack(s,d);
            case "chat" -> {
                String scope = d.has("channel") ? string(d, "channel", 5) : "map";
                boolean global = scope.equals("world");
                if (!global && !scope.equals("map")) throw new IllegalArgumentException("Kênh chat không hợp lệ.");
                String message = string(d, "text", global ? 100 : 120).trim();
                long now = System.currentTimeMillis();
                if (message.isEmpty() || now - s.lastChat < 1000) return;
                s.lastChat = now;
                if (global && !ChatGlobalService.tryChargePlayerMessage(p)) return;
                var event = Map.of("type", "chat", "channel", scope, "id", p.id, "name", p.name, "text", message);
                for (var other : sessions) {
                    if (other.authenticated && other.player != null && (global || other.player.zone == p.zone)) other.send(event);
                }
                if (global) s.send(PlayerView.stats(p));
            }
            case "portal" -> {
                if (d.has("map") && d.get("map").getAsInt() != p.zone.map.mapId) return;
                var waypoint = p.zone.map.wayPoints.get(bounded(d, "index", 0, p.zone.map.wayPoints.size() - 1));
                portal(s, waypoint);
            }
            case "npc" -> {
                int id=bounded(d,"id",0,1000);
                if(p.zone.map.getNpc(p,id)==null)throw new IllegalArgumentException("Hãy đến gần NPC.");
                MenuController.gI().openMenuNPC(s,id,p);
                if(id==consts.ConstNpc.RUONG_DO&&MapService.gI().isHome(p.zone.map.mapId)){
                    restoreChestObjective(p);
                    s.send(PlayerView.inventory(p));
                    s.send(Map.of("type","openBox"));
                }
            }
            case "menu" -> {
                int choice=bounded(d,"choice",0,s.menuChoices-1);int id=bounded(d,"id",-1,1000);
                if(id!=s.menuNpc)throw new IllegalArgumentException("Menu đã đóng.");
                s.menuChoices=0;
                var npc=p.zone.map.npcs.stream().filter(n->n.tempId==id).findFirst().orElse(null);
                if(npc!=null&&distance(p.location.x,p.location.y,npc.cx,npc.cy)>180)throw new IllegalArgumentException("Hãy đến gần NPC.");
                MenuController.gI().doSelectMenu(p,id,choice);
            }
            case "inventory" -> s.send(PlayerView.inventory(p));
            case "equip" -> {
                int i=bounded(d,"index",0,p.inventory.itemsBag.size()-1);
                InventoryService.gI().itemBagToBody(p,i);s.send(PlayerView.inventory(p));
            }
            case "unequip" -> {InventoryService.gI().itemBodyToBag(p,bounded(d,"index",0,p.inventory.itemsBody.size()-1));s.send(PlayerView.inventory(p));}
            case "store" -> {
                requireBox(p);InventoryService.gI().itemBagToBox(p,bounded(d,"index",0,p.inventory.itemsBag.size()-1));s.send(PlayerView.inventory(p));
            }
            case "withdraw" -> {
                requireBox(p);
                int index = bounded(d, "index", 0, p.inventory.itemsBox.size() - 1);
                withdraw(s, index);
            }
            case "use" -> {
                int i=bounded(d,"index",0,p.inventory.itemsBag.size()-1);
                try(var msg=packet(-43,out->{out.writeByte(0);out.writeByte(1);out.writeByte(i);})) {UseItem.gI().doItem(p,msg.message);}
                s.send(PlayerView.inventory(p));
            }
            case "confirmItem" -> {
                byte[] item=s.confirmItem;s.confirmItem=null;if(item==null)return;
                try(var msg=packet(-43,out->{out.writeByte(3);out.writeByte(item[1]);out.writeByte(item[2]);})) {UseItem.gI().doItem(p,msg.message);}
                s.send(PlayerView.inventory(p));
            }
            case "pickup" -> {
                int id=bounded(d,"id",0,32767);var item=p.zone.getItemMapByItemMapId(id);
                pickup(s, item);
            }
            case "pea" -> UseItem.gI().eatPea(p);
            case "revive" -> {
                if (!p.isPKDHVT) PlayerService.gI().hoiSinh(p);
            }
            case "returnHome" -> {
                returnHome(p);
            }
            case "skillShortcut" -> {
                byte[] shortcuts = p.playerSkill.skillShortCut;
                int slot = bounded(d, "slot", 0, shortcuts.length - 1);
                int skillId = bounded(d, "skill", -1, Byte.MAX_VALUE);
                if (skillId >= 0) {
                    var skill = p.playerSkill.getSkillbyId(skillId);
                    if (skill == null || skill.point <= 0) throw new IllegalArgumentException("Kỹ năng chưa học.");
                }
                shortcuts[slot] = (byte) skillId;
                s.send(PlayerView.stats(p));
            }
            case "upgrade" -> p.nPoint.increasePoint((byte)bounded(d,"stat",0,4),(short)bounded(d,"amount",1,100));
            case "input" -> {
                JsonArray values=d.getAsJsonArray("values");
                if(values==null||values.size()!=s.inputCount||values.size()>5)throw new IllegalArgumentException("Biểu mẫu không hợp lệ.");
                s.inputCount=0;
                try(var msg=packet(-125,out->{out.writeByte(values.size());for(var v:values){String value=v.getAsString();if(value.length()>160)throw new IllegalArgumentException("Nội dung quá dài.");out.writeUTF(value);}})) {Input.gI().doInput(p,msg.message);}
            }
            case "buy" -> {
                if(p.idMark.getShopOpen()==null)throw new IllegalArgumentException("Hãy mở cửa hàng từ NPC.");
                int id=bounded(d,"id",0,32000);
                shop.ShopService.gI().takeItem(p,(byte)0,id);
                s.send(PlayerView.inventory(p));
            }
            case "petStatus" -> {if(p.pet!=null)p.pet.changeStatus((byte)bounded(d,"status",0,4));}
            case "combine" -> {
                if(!s.combineOpen)throw new IllegalArgumentException("Hãy mở nâng cấp từ NPC.");
                var npc=p.idMark.getNpcChose();
                if(npc!=null&&distance(p.location.x,p.location.y,npc.cx,npc.cy)>180)throw new IllegalArgumentException("Hãy đến gần NPC.");
                var indices=d.getAsJsonArray("indices");if(indices==null||indices.size()>8)throw new IllegalArgumentException("Chọn tối đa 8 vật phẩm.");
                int[] selected=new int[indices.size()];Set<Integer> unique=new HashSet<>();
                for(int i=0;i<selected.length;i++){selected[i]=indices.get(i).getAsInt();if(selected[i]<0||selected[i]>=p.inventory.itemsBag.size()||!unique.add(selected[i]))throw new IllegalArgumentException("Vật phẩm không hợp lệ.");}
                combine.CombineService.gI().showInfoCombine(p,selected);
            }
            case "sell" -> {
                var npc=p.idMark.getNpcChose();
                if(npc==null||distance(p.location.x,p.location.y,npc.cx,npc.cy)>180)throw new IllegalArgumentException("Hãy mở cửa hàng ở NPC trước.");
                shop.ShopService.gI().showConfirmSellItem(p,1,bounded(d,"index",0,p.inventory.itemsBag.size()-1));
            }
            case "confirmSell" -> {
                int[] choice=s.confirmSell;s.confirmSell=null;if(choice==null)return;
                var npc=p.idMark.getNpcChose();if(npc==null||distance(p.location.x,p.location.y,npc.cx,npc.cy)>180)throw new IllegalArgumentException("Hãy đến gần cửa hàng.");
                shop.ShopService.gI().sellItem(p,choice[0],choice[1]);s.send(PlayerView.inventory(p));
            }
            case "travel" -> UseItem.gI().choseMapCapsule(p,bounded(d,"index",0,200));
            case "clans" -> clanView(s);
            case "clanCreate" -> {
                String name=string(d,"name",25).trim();if(name.length()<3)throw new IllegalArgumentException("Tên bang cần ít nhất 3 ký tự.");
                try(var msg=packet(-46,out->{out.writeByte(2);out.writeByte(0);out.writeUTF(name);})){ClanService.gI().getClan(p,msg.message);}
                clanView(s);
            }
            case "clanJoinRequest" -> {
                int id=bounded(d,"id",0,Integer.MAX_VALUE);
                try(var msg=packet(-51,out->{out.writeByte(2);out.writeInt(id);})){ClanService.gI().clanMessage(p,msg.message);}
                clanView(s);
            }
            case "clanApprove" -> {
                int id=bounded(d,"message",0,Integer.MAX_VALUE);
                try(var msg=packet(-54,out->{out.writeInt(id);out.writeByte(0);})){ClanService.gI().joinClan(p,msg.message);}
                clanView(s);
            }
            case "clanChat" -> {if(p.clan!=null)ClanService.gI().chat(p,string(d,"text",120));clanView(s);}
            case "clanLeave" -> ClanService.gI().showMenuLeaveClan(p);
            case "trade" -> {
                int action=bounded(d,"action",0,7);
                if(!Set.of(0,1,2,3,5,7).contains(action))throw new IllegalArgumentException("Thao tác giao dịch không hợp lệ.");
                int peer=(action==0||action==1)?bounded(d,"peer",1,Integer.MAX_VALUE):s.tradePeer;
                Player target=p.zone.getPlayerInMap(peer);
                if(action!=3&&(target==null||target.id==p.id||!(target.getSession() instanceof WebSession other)||!other.connected||distance(p.location.x,p.location.y,target.location.x,target.location.y)>180))throw new IllegalArgumentException("Đối phương phải ở gần và đang trực tuyến.");
                if(action==1&&s.incomingTrade!=peer)throw new IllegalArgumentException("Lời mời giao dịch đã hết hạn.");
                int index=action==2?bounded(d,"index",-1,p.inventory.itemsBag.size()-1):0;
                int quantity=action==2?bounded(d,"quantity",0,index==-1?(int)Math.min(Integer.MAX_VALUE,p.inventory.gold):9999):0;
                try(var msg=packet(-86,out->{out.writeByte(action);if(action==0||action==1)out.writeInt(peer);if(action==2){out.writeByte(index);out.writeInt(quantity);}})) {TransactionService.gI().controller(p,msg.message);}
                if(action==5)s.tradeLocked=true;
                tradeView(s);
                if(target!=null&&target.getSession() instanceof WebSession other)tradeView(other);
            }
            case "tradeView" -> tradeView(s);
            default -> throw new IllegalArgumentException("Chức năng không hợp lệ.");
        }
        if(s.player!=null) s.send(PlayerView.stats(p));
    }
    private void tradeView(WebSession s){
        Player peer=s.player==null?null:Client.gI().getPlayer(s.tradePeer);
        boolean reveal=peer!=null&&peer.getSession() instanceof WebSession other&&other.tradeLocked;
        Object view=s.player==null?null:TransactionService.gI().webView(s.player,reveal);
        if(view!=null)s.send(view);
    }
    private void clanView(WebSession s){
        List<Object> list=new ArrayList<>(),members=new ArrayList<>(),messages=new ArrayList<>();
        for(var clan:Manager.CLANS)list.add(Map.of("id",clan.id,"name",clan.name,"leader",clan.getLeader().name,"members",clan.getCurrMembers(),"max",clan.maxMember));
        var clan=s.player.clan;
        if(clan!=null){
            for(var m:clan.members)members.add(Map.of("id",m.id,"name",m.name,"role",m.role,"power",m.powerPoint));
            for(var m:clan.getCurrClanMessages())messages.add(Map.of("id",m.id,"type",m.type,"name",Objects.toString(m.playerName,""),"text",Objects.toString(m.text,"")));
        }
        s.send(Map.of("type","clans","list",list,"members",members,"messages",messages,"ownClan",clan==null?-1:clan.id,"canApprove",clan!=null&&(clan.isLeader(s.player)||clan.isDeputy(s.player))));
    }
    private void login(WebSession s, JsonObject d, boolean register) {
        if(s.authenticated||s.busySending)return;
        if(online>=WebConfig.MAX_PLAYERS){s.notice("Máy chủ đủ "+WebConfig.MAX_PLAYERS+" người. Hãy thử lại sau.");return;}
        long now=System.currentTimeMillis();
        authAttempts.entrySet().removeIf(e->now-e.getValue()[0]>120000);
        long[] attempts=authAttempts.computeIfAbsent(s.getIP(),k->new long[]{now,0});
        if(now-attempts[0]>60000){attempts[0]=now;attempts[1]=0;}
        if(++attempts[1]>12){s.notice("Quá nhiều lần đăng nhập. Hãy chờ một phút.");return;}
        String user=string(d,"username",20).trim().toLowerCase(Locale.ROOT),password=string(d,"password",72);
        String name=register?string(d,"name",10):"";
        int gender=register?bounded(d,"gender",0,2):0;
        String invite=d.has("invite")?string(d,"invite",128):"";
        recentlyLoggedOut.values().removeIf(id -> !WebPersistence.playerSavePending(id));
        if (recentlyLoggedOut.containsKey(user)) {
            s.notice("Đang lưu nhân vật. Hãy thử đăng nhập lại sau ít giây.");
            return;
        }
        if (!authenticating.add(user)) {
            s.notice("Máy chủ đang xử lý đăng nhập, hãy thử lại.");
            return;
        }
        boolean wasInGame = sessions.stream().anyMatch(other -> other.authenticated && user.equals(other.uu));
        s.busySending=true;
        try {
            auth.authenticate(user,password,register,name,gender,invite).whenCompleteAsync((result,error)->{
                s.busySending=false;
                authenticating.remove(user);
                if(stopping||!sessions.contains(s)||!s.connected||!s.channel.isOpen()||s.authenticated)return;
                if(error!=null){Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();s.notice(cause instanceof IllegalArgumentException?cause.getMessage():"Không thể đăng nhập, hãy thử lại.");return;}
                try {
                    if(online>=WebConfig.MAX_PLAYERS)throw new IllegalArgumentException("Máy chủ đủ người.");
                    if(wasInGame||Client.gI().getPlayerByUser(result.accountId())!=null)throw new IllegalArgumentException("Tài khoản đang chơi hoặc vừa thoát. Hãy thử lại.");
                    if (WebPersistence.playerChangedSince(result.playerId(), result.saveVersion())) {
                        s.notice("Đang lưu nhân vật. Hãy thử đăng nhập lại sau ít giây.");
                        return;
                    }
                    s.uu=result.username();
                    Player p=NTTSqlFetcher.loadWebPlayer(s,result.account(),result.player(),result.rank());
                    if(p==null)throw new IllegalArgumentException("Không tải được nhân vật. Hãy kiểm tra tài khoản hoặc thử lại.");
                    s.player=p;p.setSession(s);p.setClothes.setup();p.nPoint.calPoint();
                    if(p.zone==null)p.zone=MapService.gI().getMapById(39 + p.gender).zones.get(0);
                    if (!p.zone.map.hasTerrain()) {
                        p.zone = MapService.gI().getMapById(21 + p.gender).zones.get(0);
                        p.location.x = 200;
                        p.location.y = p.zone.map.yPhysicInTop(200, 100);
                    }
                    // All normal maps have one shared zone. Never create per-device copies.
                    if(!MapService.gI().isMapPhoBan(p.zone.map.mapId))p.zone=p.zone.map.zones.get(0);
                    p.location.x=Math.max(0,Math.min(p.zone.map.mapWidth-1,p.location.x));
                    p.location.y=Math.max(0,Math.min(p.zone.map.mapHeight-1,p.location.y));
                    p.zone.addPlayer(p);p.idMark.setLoadedAllDataPlayer(true);
                    Client.gI().put(p);s.joinedGame=true;s.authenticated=true;
                    online++;
                    WebPersistence.accountLogin(s.userId, s.ipAddress);
                    joined(s);
                    sendZone(s);
                    s.send(PlayerView.inventory(p));s.send(PlayerView.stats(p));
                    s.notice("Chào " + p.name + "!");
                } catch(Exception e){s.notice(e.getMessage());System.err.println("Player login failure: "+e.getMessage());if(s.player!=null)remove(s);}
            }, loop);
        } catch(RejectedExecutionException e){authenticating.remove(user);s.busySending=false;s.notice("Máy chủ đang xử lý đăng nhập, hãy thử lại.");}
    }
    private void joined(WebSession s) {
        byte[] token=new byte[32];new java.security.SecureRandom().nextBytes(token);
        s.resumeToken=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        resumeTokens.put(s.resumeToken,s);
        s.send(Map.of("type","joined","id",s.player.id,"name",s.player.name,"maxPlayers",WebConfig.MAX_PLAYERS,"token",s.resumeToken));
        s.send(PlayerView.taskMessage(s.player));
    }
    private void resume(WebSession s,String token) {
        if(s.authenticated)return;
        WebSession old=resumeTokens.remove(token);
        if(old==null||old.player==null||old.intentionalClose||(old.detachedUntil!=0&&old.detachedUntil<System.currentTimeMillis())){
            s.send(Map.of("type","resumeFailed"));return;
        }
        TransactionService.gI().cancelTrade(old.player);
        s.copyPlayerSession(old);old.player=null;old.authenticated=false;old.intentionalClose=true;
        sessions.remove(old);old.disconnect();
        joined(s);s.send(PlayerView.inventory(s.player));s.send(PlayerView.stats(s.player));
        s.automation.restore(old.automation);
        s.notice("Đã kết nối lại, tiếp tục hành trình!");
    }

    void returnHome(Player player) {
        if (!player.isDie()) throw new IllegalArgumentException("Chỉ có thể về nhà bằng cách này khi đã ngã gục.");
        int destination = MapService.gI().isMapMaBu(player.zone.map.mapId) ? 114 : player.gender + 21;
        ChangeMapService.gI().changeMapBySpaceShip(player, destination, 0, -1);
    }

    boolean mobClaimedByAnother(WebSession requester, Mob mob) {
        for (WebSession other : sessions) {
            if (other != requester && other.connected && other.player != null && other.player.zone == mob.zone
                    && other.automation.claims(mob)) return true;
        }
        return false;
    }

    boolean tryQuestClanPair(WebSession requester) throws Exception {
        if (!requester.automation.readyForClanPair()) return false;
        var peers = sessions.stream().filter(other -> other != requester && other.automation.readyForClanPair())
                .sorted(Comparator.comparingLong(other -> other.player.id)).toList();
        for (WebSession other : peers) {
            if (WebQuestActions.pairClans(requester.player, other.player)) return true;
        }
        return false;
    }
    void restoreChestObjective(Player player) {
        var tasks = TaskService.gI();
        if (tasks.getIdTask(player) != consts.ConstTask.TASK_0_3) return;

        // Every starter chest contains this radar until it is withdrawn. Older web clients
        // could transfer it without notifying TaskService, and it may have been sold since.
        final int starterRadar = 12;
        if (InventoryService.gI().findItem(player.inventory.itemsBox, starterRadar) == null) {
            tasks.checkDoneTaskGetItemBox(player);
        }
    }

    private void requireBox(Player p) { if(!MapService.gI().isHome(p.zone.map.mapId))throw new IllegalArgumentException("Hãy về nhà để sử dụng rương."); }
    void withdraw(WebSession s, int index) throws Exception {
        Player p = s.player;
        requireBox(p);
        if (!p.inventory.itemsBox.get(index).isNotNullItem()) throw new IllegalArgumentException("Ô rương trống.");
        try (var msg = packet(-40, out -> {
            out.writeByte(0); // Box to body or bag.
            out.writeByte(index);
        })) {
            UseItem.gI().getItem(s, msg.message);
        }
        s.send(PlayerView.inventory(p));
    }

    void pickup(WebSession s, map.ItemMap item) {
        Player p = s.player;
        if (item != null && p.zone.isItemVisibleTo(p, item)
                && distance(p.location.x, p.location.y, item.x, item.y) <= 90) {
            ItemMapService.gI().pickItem(p, item.itemMapId, false);
            s.send(PlayerView.inventory(p));
        }
    }

    void portal(WebSession s, map.WayPoint waypoint) {
        Player p = s.player;
        if (MapService.gI().getWaypointPlayerIn(p) != waypoint) {
            throw new IllegalArgumentException("Hãy đi vào lối chuyển khu vực.");
        }
        var oldZone = p.zone;
        ChangeMapService.gI().changeMapWaypoint(p);
        s.direction = s.vertical = 0;
        s.velocityY = 0;
        s.jump = false;
        if (p.zone != oldZone) {
            s.autoPortalAttempt = null;
            sendZone(s);
        }
    }

    private void sendZone(WebSession s) {
        var player = s.player;
        s.send(GameCatalog.map(player.zone.map));
        s.sentZone = player.zone;
        var tasks = services.TaskService.gI();
        tasks.checkDoneTaskGoToMap(player, s.sentZone);
        tasks.restoreStarterHomeArrival(player);
        if (MapService.gI().isHome(s.sentZone.map.mapId) && player.magicTree != null) player.magicTree.loadMagicTree();
    }
    private void attack(WebSession s,JsonObject d) throws Exception {
        Player p=s.player;if(p.isDie() || p.newSkill.isStartSkillSpecial)return;
        int skill=bounded(d,"skill",0,26);
        var selected=p.playerSkill.skills.stream().filter(k->k.template!=null&&k.template.id==skill&&k.point>0).findFirst().orElseThrow(()->new IllegalArgumentException("Kỹ năng chưa học."));
        Mob mob=null;Player target=null;
        if(d.has("mob")) {int id=bounded(d,"mob",0,10000);mob=p.zone.mobs.stream().filter(m->m.id==id&&!m.isDie()).findFirst().orElse(null);}
        if(d.has("player")) {long id=d.get("player").getAsLong();target=p.zone.getHumanoids().stream().filter(a->a.id==id&&!a.isDie()&&a.id!=p.id).findFirst().orElse(null);}
        if (selected.template.type == 4) {
            int targetX = mob != null ? mob.location.x : target != null ? target.location.x : p.location.x + s.facing * selected.dx;
            int direction = targetX == p.location.x ? s.facing : Integer.compare(targetX, p.location.x);
            p.playerSkill.skillSelect = selected;
            try (var msg = packet(-45, out -> {
                out.writeByte(skill);
                out.writeShort(p.location.x);
                out.writeShort(p.location.y);
                out.writeByte(direction);
                out.writeShort(targetX);
                out.writeShort(selected.dy);
            })) {
                if (SkillService.gI().useSkill(p, null, null, 20, msg.message)) {
                    s.facing = direction;
                    s.send(PlayerView.stats(p));
                }
            }
            return;
        }
        attack(s, selected, target, mob, false);
    }
    void attack(WebSession s, Skill selected, Player target, Mob mob, boolean questBasicAttack) {
        Player p = s.player;
        if (p.isDie() || p.newSkill.isStartSkillSpecial) return;
        int skill = selected.template.id;
        p.playerSkill.skillSelect = selected;
        if(mob!=null&&distance(p.location.x,p.location.y,mob.location.x,mob.location.y)>Math.max(110,selected.dx+40))return;
        if(target!=null&&distance(p.location.x,p.location.y,target.location.x,target.location.y)>Math.max(110,selected.dx+40))return;
        if(selected.template.type==1&&mob==null&&target==null)return;
        long lastUse = selected.lastTimeUseThisSkill;
        var skills = SkillService.gI();
        boolean used = questBasicAttack ? skills.useQuestBasicAttack(p, target, mob) : skills.useSkill(p, target, mob, -1, null);
        if (!used || selected.template.type != 1 || selected.lastTimeUseThisSkill == lastUse) return;
        for(var other:sessions)if(other.player!=null&&other.player.zone==p.zone)other.send(Map.of("type","attack","id",p.id,"skill",skill,"x",p.location.x,"y",p.location.y,"targetX",mob!=null?mob.location.x:target!=null?target.location.x:p.location.x+s.facing*70,"targetY",mob!=null?mob.location.y:target!=null?target.location.y:p.location.y));
    }
    static int moveSpeed(Player player) {
        return Math.max(12, Math.min(16, player.nPoint.speed + 4));
    }

    private void move(WebSession s) {
        Player p=s.player;if(p==null||p.zone==null||p.isDie()||p.newSkill.isStartSkillSpecial||p.effectSkill.isHaveEffectSkill()||TransactionService.gI().check(p))return;
        var map=p.zone.map;
        if (!map.hasTerrain()) return;
        int x=Math.max(0,Math.min(map.mapWidth-1,p.location.x+s.direction*moveSpeed(p)));
        int y=p.location.y;
        if (map.blocksHorizontalMove(p.location.x, x, y)) x = p.location.x;
        var entrance = map.getWaypointAt(x, y);
        if (s.automation.approaches(entrance)) {
            // Enter at the current height before the boundary tile changes the falling position.
            if (x != p.location.x) PlayerService.gI().playerMove(p, x, y);
            if (p.zone.map == map) s.automation.enterPortal(p, entrance);
            return;
        }
        if (!s.automation.active() && entrance != null && !entrance.isEnter && entrance != s.autoPortalAttempt
                && (s.direction < 0 && entrance.minX == 0 || s.direction > 0 && entrance.maxX >= map.mapWidth - 1)) {
            if (x != p.location.x) PlayerService.gI().playerMove(p, x, y);
            if (p.zone.map == map) {
                s.autoPortalAttempt = entrance;
                portal(s, entrance);
            }
            return;
        }
        int ground=map.yPhysicInTop(x,Math.max(0,y-18));
        if(ground<=0)ground=map.mapHeight-48;
        boolean onGround=y>=ground-2;
        if(s.jump&&onGround){s.velocityY=-16;s.jump=false;}
        if(s.vertical<0&&p.nPoint.mp>0){s.velocityY=-6;if(ticks%10==0)p.nPoint.addMp(-1);}
        else if(s.vertical>0)s.velocityY=7;
        else s.velocityY=Math.min(10,s.velocityY+0.8);
        y+=Math.round((float)s.velocityY);
        if(y>=ground&&s.velocityY>=0){y=ground;s.velocityY=0;}
        y=Math.max(48,Math.min(map.mapHeight-25,y));
        if(x!=p.location.x||y!=p.location.y)PlayerService.gI().playerMove(p,x,y);
        if (s.automation.active() || p.zone.map != map || !((s.direction < 0 && x == 0) || (s.direction > 0 && x == map.mapWidth - 1))) return;
        var waypoint = MapService.gI().getWaypointPlayerIn(p);
        if (waypoint != null && !waypoint.isEnter && waypoint != s.autoPortalAttempt) {
            s.autoPortalAttempt = waypoint;
            portal(s, waypoint);
        }
    }
    private void tickSafely() {
        if(stopping)return;
        try {
            Runnable r;for(int i=0;i<128&&(r=commands.poll())!=null;i++)r.run();
            ticks++;
            if(ticks%3==0)for(var manager:bossManagers)manager.updateWeb();
            if(ticks%6==0)services.func.TransactionService.gI().updateWeb();
            if(ticks%10==0)managers.SuperRankManager.gI().updateWeb();
            for(var s:new ArrayList<>(sessions)) {
                if(s.detachedUntil!=0){if(System.currentTimeMillis()>s.detachedUntil)remove(s);continue;}
                if(!s.connected||!s.channel.isOpen()){if(s.authenticated&&!s.intentionalClose){if(s.player!=null)TransactionService.gI().cancelTrade(s.player);s.connected=false;s.detachedUntil=System.currentTimeMillis()+45000;s.direction=0;s.vertical=0;}else remove(s);continue;}
                if(System.currentTimeMillis()-Math.max(s.lastSeen,s.lastAutoPong)>(s.authenticated?120000:30000)){remove(s);continue;}
                if (s.authenticated) {
                    if (s.automation.active()) {
                        if (ticks % 400 == 0) {
                            s.awaitingAutoPong = true;
                            WebSockets.sendPing(ByteBuffer.allocate(0), s.channel, null);
                        }
                        s.automation.update();
                    }
                    move(s);
                }
            }
            if(ticks%20==0) {
                services.shenron.Shenron_Manager.gI().update();
                Deputyhead.Service.NgocRongNamecService.gI().updateWeb();
                matches.tournament.The23rdMartialArtCongressManager.gI().update();
                matches.tournament.DeathOrAliveArenaManager.gI().update();
                matches.tournament.WorldMartialArtsTournamentManager.gI().update();
                // Inactive zones sleep; their respawn timers use wall-clock time.
                Set<map.Zone> active=new HashSet<>();
                for(var s:sessions)if(s.player!=null&&s.player.zone!=null)active.add(s.player.zone);
                for(var z:active)z.update();
            }
            if(ticks%2==0){
                Map<map.Zone,String> snapshots=new HashMap<>();
                for(var s:sessions)if(s.connected&&s.authenticated&&s.player!=null&&s.player.zone!=null) {
                    if(s.sentZone!=s.player.zone)sendZone(s);
                    String snapshot = s.player.zone.hasPersonalItems()
                        ? JSON.toJson(PlayerView.state(s.player, online, ticks))
                        : snapshots.computeIfAbsent(s.player.zone, z -> JSON.toJson(PlayerView.state(s.player, online, ticks)));
                    s.sendJson(snapshot,true);
                    if(ticks%20==0)s.send(PlayerView.stats(s.player));
                }
            }
            if(ticks%600==0)save();
            lastTick=System.currentTimeMillis();
        } catch(Exception e){System.err.println("World tick failed: "+e);e.printStackTrace();}
    }
    private void save() {
        for(var s:sessions)if(s.player!=null){s.player.mapIdBeforeLogout=s.player.zone.map.mapId;PlayerDAO.updatePlayer(s.player);}
        player.Service.ClanService.gI().close();
    }
    static String string(JsonObject d,String key,int max) {if(!d.has(key)||!d.get(key).isJsonPrimitive())throw new IllegalArgumentException("Thiếu "+key);String value=d.get(key).getAsString();if(value.length()>max)throw new IllegalArgumentException("Nội dung quá dài.");return value;}
    static int bounded(JsonObject d,String key,int min,int max) {int n=d.get(key).getAsInt();if(n<min||n>max)throw new IllegalArgumentException("Giá trị không hợp lệ: "+key);return n;}
    static double distance(int x,int y,int tx,int ty){return Math.hypot(x-tx,y-ty);}
    interface Writer {void write(DataOutputStream out)throws Exception;}
    record Packet(Message message) implements AutoCloseable {public void close(){message.cleanup();}}
    static Packet packet(int command,Writer writer)throws Exception {var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){writer.write(out);}return new Packet(new Message((byte)command,bytes.toByteArray()));}
    @Override public void close() {
        ready=false;
        try {loop.submit(()->{stopping=true;for(var s:new ArrayList<>(sessions))remove(s);player.Service.ClanService.gI().close();}).get(5,TimeUnit.SECONDS);}
        catch(Exception e){System.err.println("Shutdown save incomplete: "+e.getClass().getSimpleName());}
        WebPersistence.flush(20000);
        loop.shutdownNow();auth.close();
    }
}
