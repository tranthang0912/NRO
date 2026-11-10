import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.*;

/** Real WebSocket smoke test. Uses disposable accounts only, never old SQL accounts. */
public class SmokeTest {
    static final Gson G=new Gson();
    static final String BASE=System.getenv().getOrDefault("NRO_TEST_URL","http://localhost:8080");
    static class Bot implements WebSocket.Listener,AutoCloseable {
        WebSocket ws; final BlockingQueue<JsonObject> queue=new LinkedBlockingQueue<>();
        final List<JsonObject> history=new CopyOnWriteArrayList<>(); final StringBuilder partial=new StringBuilder();
        volatile boolean measuring;long receivedBytes;long previousFrame;final List<Long> gaps=new CopyOnWriteArrayList<>();
        Bot() throws Exception {ws=HttpClient.newHttpClient().newWebSocketBuilder().header("Origin",BASE).buildAsync(URI.create(BASE.replaceFirst("^http","ws")+"/ws"),this).get(10,TimeUnit.SECONDS);}
        public void onOpen(WebSocket w){w.request(1);}
        public CompletionStage<?> onText(WebSocket w,CharSequence data,boolean last){partial.append(data);if(last){var j=JsonParser.parseString(partial.toString()).getAsJsonObject();if(measuring){receivedBytes+=partial.length();if(j.get("type").getAsString().equals("state")){long now=System.nanoTime();if(previousFrame!=0)gaps.add(TimeUnit.NANOSECONDS.toMillis(now-previousFrame));previousFrame=now;}}queue.offer(j);history.add(j);partial.setLength(0);}w.request(1);return null;}
        void send(Object o){ws.sendText(G.toJson(o),true).join();}
        JsonObject waitFor(String type,int seconds)throws Exception {long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);while(System.nanoTime()<end){var j=queue.poll(1,TimeUnit.SECONDS);if(j!=null){if(j.get("type").getAsString().equals(type))return j;if(j.get("type").getAsString().equals("notice"))System.out.println("NOTICE "+j.get("text").getAsString());}}throw new AssertionError("Timed out waiting for "+type);}
        public void close(){send(Map.of("type","logout"));ws.sendClose(1000,"test finished").join();}
    }
    static JsonObject ownPlayer(JsonObject state,long id) {
        return state.getAsJsonArray("players").asList().stream().map(JsonElement::getAsJsonObject)
            .filter(player->player.get("id").getAsLong()==id).findFirst().orElseThrow();
    }
    static JsonObject expectTask(Bot bot, int id, int index) throws Exception {
        JsonObject task = bot.waitFor("task", 5).getAsJsonObject("task");
        if (task.get("id").getAsInt() != id || task.get("index").getAsInt() != index) {
            throw new AssertionError("Wrong task or objective: " + task);
        }
        return task;
    }
    static JsonObject walkOutOfStarter(Bot bot, int home) throws Exception {
        bot.send(Map.of("type", "move", "x", 1, "y", 0, "jump", false));
        JsonObject map = bot.waitFor("map", 15);
        if (map.get("id").getAsInt() != home) throw new AssertionError("Wrong starter exit: " + map.get("id"));
        expectTask(bot, 0, 2);
        return map;
    }
    static JsonObject walkThrough(Bot bot,long id,JsonObject map,int destination)throws Exception {
        JsonObject portal=map.getAsJsonArray("portals").asList().stream().map(JsonElement::getAsJsonObject)
            .filter(entry->entry.get("map").getAsInt()==destination).findFirst().orElseThrow();
        int xMin=portal.get("minX").getAsInt(),xMax=portal.get("maxX").getAsInt();
        int yMin=portal.get("minY").getAsInt(),yMax=portal.get("maxY").getAsInt();
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(18);
        int direction=0;
        while(System.nanoTime()<deadline) {
            JsonObject player=ownPlayer(bot.waitFor("state",5),id);
            int x=player.get("x").getAsInt(),y=player.get("y").getAsInt();
            if(x>=xMin&&x<=xMax&&y>=yMin&&y<=yMax) {
                bot.send(Map.of("type","move","x",0,"y",0,"jump",false));
                bot.send(Map.of("type","portal","index",portal.get("index").getAsInt()));
                JsonObject changed=bot.waitFor("map",5);
                if(changed.get("id").getAsInt()!=destination)throw new AssertionError("Wrong portal destination: "+changed.get("id"));
                return changed;
            }
            int next=x<xMin?1:x>xMax?-1:0;
            if(next!=direction) {
                bot.send(Map.of("type","move","x",next,"y",0,"jump",false));
                direction=next;
            }
        }
        throw new AssertionError("Could not reach portal to "+destination);
    }
    public static void main(String[] args)throws Exception {
        String suffix=String.valueOf(System.currentTimeMillis()).substring(7);
        List<Bot> bots=new ArrayList<>();String password="Test-"+UUID.randomUUID();
        try {
            for(int i=0;i<5;i++) {
                Bot b=new Bot();bots.add(b);b.waitFor("hello",5);
                b.send(Map.of("type","register","username","qa"+suffix+i,"password",password,"name","qa"+suffix+i,"gender",0,"invite",""));
                long id=b.waitFor("joined",20).get("id").getAsLong();
                JsonObject start=b.waitFor("map",5);
                if(start.get("id").getAsInt()!=39)throw new AssertionError("Incorrect Earth starting map");
                JsonObject home=walkOutOfStarter(b,21);
                if(i==0) {
                    var original = b.waitFor("stats", 5).getAsJsonArray("skillShortcuts");
                    if (original.size() != 10 || original.get(0).getAsInt() != 0 || original.get(9).getAsInt() != -1) {
                        throw new AssertionError("Original shortcut slots not loaded: " + original);
                    }
                    b.queue.clear();
                    b.send(Map.of("type", "skillShortcut", "slot", 9, "skill", 0));
                    long shortcutEnd = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                    while (b.waitFor("stats", 5).getAsJsonArray("skillShortcuts").get(9).getAsInt() != 0) {
                        if (System.nanoTime() > shortcutEnd) throw new AssertionError("Shortcut assignment ignored");
                    }
                    var homeState = b.waitFor("state", 5);
                    if (homeState.getAsJsonArray("drops").asList().stream()
                        .anyMatch(drop -> drop.getAsJsonObject().get("template").getAsInt() == 74)) {
                        throw new AssertionError("The quest-only food appeared before the required objective");
                    }
                    b.send(Map.of("type","move","x",1,"y",0,"jump",false));
                    long walkEnd=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
                    while(ownPlayer(b.waitFor("state",5),id).get("x").getAsInt()<200) {
                        if(System.nanoTime()>walkEnd)throw new AssertionError("Cannot reach Gohan");
                    }
                    b.send(Map.of("type","move","x",0,"y",0,"jump",false));
                    b.send(Map.of("type","npc","id",0));
                    expectTask(b, 0, 3);
                    var tutorial=b.waitFor("tutorial",5);
                    if(!tutorial.get("text").getAsString().contains("rađa"))throw new AssertionError("Missing original NPC quest dialogue");
                    b.send(Map.of("type", "inventory"));
                    JsonObject items = b.waitFor("inventory", 5);
                    int radar = items.getAsJsonArray("box").asList().stream().map(JsonElement::getAsJsonObject)
                        .filter(item -> item.get("id").getAsInt() == 12)
                        .findFirst().orElseThrow().get("index").getAsInt();
                    b.send(Map.of("type", "withdraw", "index", radar));
                    expectTask(b, 0, 4);
                    b.waitFor("inventory", 5);
                    b.send(Map.of("type","move","x",1,"y",0,"jump",false));
                    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
                    int x=0;
                    while(x<310&&System.nanoTime()<deadline) {
                        x=ownPlayer(b.waitFor("state",5),id).get("x").getAsInt();
                    }
                    b.send(Map.of("type","move","x",0,"y",0,"jump",false));
                    if(x<310)throw new AssertionError("Cannot reach bean tree");
                    b.send(Map.of("type","npc","id",4));
                    var treeMenu=b.waitFor("menu",5);
                    if(treeMenu.get("npc").getAsInt()!=4)throw new AssertionError("Wrong tree menu");
                    b.send(Map.of("type","menu","id",4,"choice",0));
                    expectTask(b, 0, 5);
                    if(b.waitFor("treeHarvest",5).get("peas").getAsInt()!=0)throw new AssertionError("Beans were not harvested");
                    b.send(Map.of("type", "move", "x", -1, "y", 0, "jump", false));
                    long returnEnd = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                    while (ownPlayer(b.waitFor("state", 5), id).get("x").getAsInt() > 225) {
                        if (System.nanoTime() > returnEnd) throw new AssertionError("Cannot return to Gohan");
                    }
                    b.send(Map.of("type", "move", "x", 0, "y", 0, "jump", false));
                    b.send(Map.of("type","npc","id",0));
                    expectTask(b, 1, 0);
                    b.waitFor("tutorial",5);
                    System.out.println("PASS: Original tutorial, chest radar, bean harvest and transition to training quest.");
                }
                walkThrough(b,id,home,0);
            }
            for(Bot b:bots)b.queue.clear();
            for(Bot b:bots){var st=b.waitFor("state",5);if(st.get("online").getAsInt()!=5||st.getAsJsonArray("players").size()<5)throw new AssertionError("Players do not share the map");}
            System.out.println("PASS: Five distinct accounts share one map and zone.");
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(".local"));
            java.nio.file.Files.writeString(java.nio.file.Path.of(".local/qa-fixture.json"),G.toJson(Map.of("username","qa"+suffix+"0","password",password,"base",BASE,"schema",System.getenv().getOrDefault("DB_SCHEMA","nro_web"))));
            Bot previous=bots.get(0);var ticket=previous.history.stream().filter(j->j.get("type").getAsString().equals("joined")).findFirst().orElseThrow();
            previous.ws.abort();Thread.sleep(200);Bot resumed=new Bot();resumed.waitFor("hello",5);resumed.send(Map.of("type","resume","token",ticket.get("token").getAsString()));
            var rejoined=resumed.waitFor("joined",5);if(rejoined.get("id").getAsLong()!=ticket.get("id").getAsLong())throw new AssertionError("Reconnect created another character");bots.set(0,resumed);
            resumed.waitFor("state",5);
            try(Bot replay=new Bot()){replay.waitFor("hello",5);replay.send(Map.of("type","resume","token",ticket.get("token").getAsString()));replay.waitFor("resumeFailed",5);}
            System.out.println("PASS: Network reconnect restores the same live character; old ticket cannot be reused.");
            try(Bot sixth=new Bot()){sixth.waitFor("hello",5);sixth.send(Map.of("type","login","username","qa"+suffix+"0","password",password));var reply=sixth.waitFor("notice",5);if(!reply.get("text").getAsString().contains("đủ"))throw new AssertionError(reply);}
            System.out.println("PASS: Sixth player refused without disconnecting the first five.");
            Bot first=bots.get(0);first.queue.clear();var before=first.waitFor("state",3);long id=first.history.stream().filter(j->j.get("type").getAsString().equals("joined")).findFirst().get().get("id").getAsLong();
            int oldX=before.getAsJsonArray("players").asList().stream().map(JsonElement::getAsJsonObject).filter(j->j.get("id").getAsLong()==id).findFirst().orElseThrow().get("x").getAsInt();
            first.send(Map.of("type","move","x",1,"y",0,"jump",false));Thread.sleep(900);first.send(Map.of("type","move","x",0,"y",0,"jump",false));
            bots.get(1).queue.clear();var after=bots.get(1).waitFor("state",3);
            var seen=after.getAsJsonArray("players").asList().stream().filter(j->j.getAsJsonObject().get("id").getAsLong()==id).findFirst().orElseThrow().getAsJsonObject();
            if(seen.get("x").getAsInt()<=oldX+30)throw new AssertionError("Movement not replicated");
            System.out.println("PASS: Movement broadcast to another device, x="+seen.get("x"));
            first.send(Map.of("type","chat","text","qa multiplayer message"));bots.get(1).waitFor("chat",5);
            System.out.println("PASS: Map chat reaches another session.");
            Bot second=bots.get(1);long secondId=second.history.stream().filter(j->j.get("type").getAsString().equals("joined")).findFirst().orElseThrow().get("id").getAsLong();
            first.send(Map.of("type","trade","action",0,"peer",secondId));second.waitFor("tradeInvite",8);
            second.send(Map.of("type","trade","action",1,"peer",id));first.waitFor("tradeOpen",8);second.waitFor("tradeOpen",8);
            first.send(Map.of("type","trade","action",2,"index",-1,"quantity",10));
            first.send(Map.of("type","trade","action",5));second.send(Map.of("type","trade","action",5));Thread.sleep(200);
            first.send(Map.of("type","trade","action",7));first.send(Map.of("type","trade","action",7));Thread.sleep(200);
            first.queue.clear();first.send(Map.of("type","tradeView"));var pending=first.waitFor("tradeState",5);if(!pending.get("locked").getAsBoolean())throw new AssertionError("Trade bypassed second player's approval");
            first.queue.clear();second.send(Map.of("type","trade","action",7));long tradeEnd=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);boolean traded=false;
            while(System.nanoTime()<tradeEnd){var st=first.waitFor("stats",3);if(st.get("gold").getAsLong()==1990){traded=true;break;}}
            if(!traded)throw new AssertionError("Trade did not transfer the expected gold");
            System.out.println("PASS: In-game trade requires both players, resists duplicate confirmation, transfers 10 gold.");
            first.queue.clear();var combatState=first.waitFor("state",5);var mobs=combatState.getAsJsonArray("mobs");
            if(!mobs.isEmpty()){
                var mob=mobs.get(0).getAsJsonObject();int mobId=mob.get("id").getAsInt();int tx=mob.get("x").getAsInt();
                long walkEnd=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
                while(System.nanoTime()<walkEnd){first.queue.clear();var st=first.waitFor("state",3);var self=st.getAsJsonArray("players").asList().stream().map(JsonElement::getAsJsonObject).filter(p->p.get("id").getAsLong()==id).findFirst().orElseThrow();int dx=tx-self.get("x").getAsInt();if(Math.abs(dx)<65)break;first.send(Map.of("type","move","x",Integer.signum(dx),"y",0,"jump",false));}
                first.send(Map.of("type","move","x",0,"y",0,"jump",false));first.queue.clear();var st=first.waitFor("state",3);var target=st.getAsJsonArray("mobs").asList().stream().map(JsonElement::getAsJsonObject).filter(m->m.get("id").getAsInt()==mobId).findFirst().orElseThrow();long hp=target.get("hp").getAsLong();
                first.send(Map.of("type","attack","mob",mobId,"skill",0));long hitEnd=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);boolean hit=false;
                while(System.nanoTime()<hitEnd){var next=first.waitFor("state",3);var m=next.getAsJsonArray("mobs").asList().stream().map(JsonElement::getAsJsonObject).filter(v->v.get("id").getAsInt()==mobId).findFirst().orElseThrow();if(m.get("hp").getAsLong()<hp){hit=true;break;}}
                if (!hit) throw new AssertionError("Attack did not reduce mob HP");
                // The training objective must advance when the original wooden dummy dies.
                JsonObject progress = null;
                for (int attempt = 0; attempt < 20 && progress == null; attempt++) {
                    Thread.sleep(550);
                    first.send(Map.of("type", "attack", "mob", mobId, "skill", 0));
                    progress = first.history.stream().filter(j -> j.get("type").getAsString().equals("task"))
                        .map(j -> j.getAsJsonObject("task"))
                        .filter(t -> t.get("id").getAsInt() == 1 && t.get("index").getAsInt() == 0
                            && t.getAsJsonArray("subtasks").get(0).getAsJsonObject().get("count").getAsInt() == 1)
                        .findFirst().orElse(null);
                }
                if (progress == null) throw new AssertionError("Killing a wooden dummy did not advance training");
                System.out.println("PASS: Defeating a wooden dummy advances the original training objective to 1/5.");
            }else throw new AssertionError("No village mobs loaded");
            first.queue.clear();first.send(Map.of("type","inventory"));var inv=first.waitFor("inventory",5);if(inv.getAsJsonArray("body").isEmpty())throw new AssertionError("missing equipment");
            first.waitFor("stats",5);
            System.out.println("PASS: Inventory received from the original character.");
            first.send(Map.of("type","move","x",1,"y",0,"jump",false));Thread.sleep(700);first.send(Map.of("type","move","x",0,"y",0,"jump",false));
            first.queue.clear();var finalState=first.waitFor("state",5);int savedX=finalState.getAsJsonArray("players").asList().stream().map(JsonElement::getAsJsonObject).filter(j->j.get("id").getAsLong()==id).findFirst().orElseThrow().get("x").getAsInt();
            first.send(Map.of("type","logout"));Thread.sleep(1000);
            database.DatabaseManager.initialize();
            try(var c=database.DatabaseManager.getConnection();var ps=c.prepareStatement("SELECT data_location FROM player WHERE id=?")){ps.setLong(1,id);try(var rs=ps.executeQuery()){rs.next();var location=JsonParser.parseString(rs.getString(1)).getAsJsonArray();if(location.get(1).getAsInt()!=savedX)throw new AssertionError("Position not saved to PostgreSQL: "+location+" expected x="+savedX);}}
            try(Bot reconnect=new Bot()){reconnect.waitFor("hello",5);reconnect.send(Map.of("type","login","username","qa"+suffix+"0","password",password));var joined=reconnect.waitFor("joined",15);if(joined.get("id").getAsLong()!=id)throw new AssertionError("Character identity lost");var restored=reconnect.waitFor("state",5);var p=restored.getAsJsonArray("players").asList().stream().map(JsonElement::getAsJsonObject).filter(j->j.get("id").getAsLong()==id).findFirst().orElseThrow();if(p.get("x").getAsInt()!=savedX)throw new AssertionError("Position not restored");
                var shortcuts = reconnect.waitFor("stats", 5).getAsJsonArray("skillShortcuts");
                if (shortcuts.get(9).getAsInt() != 0) throw new AssertionError("Shortcut not restored");
            }
            System.out.println("PASS: PostgreSQL contains the new position; reconnect restores it.");
        } finally {for(Bot b:bots)try{b.close();}catch(Exception ignored){}database.DatabaseManager.close();}
    }
}
