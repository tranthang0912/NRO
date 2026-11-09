package web;

import com.google.gson.*;
import database.DatabaseManager;
import item.Template;
import java.nio.file.*;
import java.util.*;
import server.Manager;

/** Existing game artwork and definitions, loaded once and cached by the browser. */
final class GameCatalog {
    static final Map<Integer, Object> parts = new HashMap<>();
    static final Map<Integer, String> mobNames = new HashMap<>();
    static final Map<Integer, Template.NpcTemplate> npcs = new HashMap<>();
    static String json;
    static void load() throws Exception {
        try (var c = DatabaseManager.getConnection(); var s = c.createStatement(); var rs = s.executeQuery("SELECT id,data FROM part ORDER BY id")) {
            while (rs.next()) parts.put(rs.getInt(1), JsonParser.parseString(rs.getString(2)));
        }
        for (var m : Manager.MOB_TEMPLATES) mobNames.put(m.id, m.name);
        for (var n : Manager.NPC_TEMPLATES) npcs.put(n.id, n);
        Map<Integer,Object> items = new HashMap<>();
        for (var i : Manager.ITEM_TEMPLATES) if (i != null) items.put((int)i.id, Map.of("name", i.name, "icon", i.iconID, "description", i.description));
        var mobFrames=JsonParser.parseString(Files.readString(Path.of("data/web-mob/frames.json")));
        json = GameWorld.JSON.toJson(Map.of("parts", parts, "npcs", npcs, "items", items,"mobFrames",mobFrames));
    }
    static Map<String,Object> map(map.Map m) {
        List<Object> decorations=new ArrayList<>();
        try(var in=new java.io.DataInputStream(Files.newInputStream(Path.of("data/map/item_bg_map_data/"+m.mapId)))){
            int count=in.readUnsignedShort();
            for(int i=0;i<count;i++){
                int id=in.readUnsignedShort(),x=in.readUnsignedShort(),y=in.readUnsignedShort();
                var bg=Manager.BG_ITEMS.stream().filter(b->b.id==id).findFirst().orElse(null);
                if(bg!=null)decorations.add(Map.of("image",bg.idImage,"layer",bg.layer,"x",x*24+bg.dx,"y",y*24+bg.dy));
            }
        }catch(java.nio.file.NoSuchFileException ignored){}
        catch(Exception e){System.err.println("Cannot load decorations for map "+m.mapId+": "+e.getMessage());}
        List<Object> npcList = new ArrayList<>();
        for (var npc : m.npcs) {
            var temp = npcs.get(npc.tempId);
            if (temp == null) continue;
            npcList.add(Map.of("id", npc.tempId, "name", temp.name, "x", npc.cx, "y", npc.cy, "head", temp.head, "body", temp.body, "leg", temp.leg));
        }
        List<Object> portals = new ArrayList<>();
        for (int i=0;i<m.wayPoints.size();i++) {
            var wp=m.wayPoints.get(i);
            portals.add(Map.of("index", i, "name", wp.name, "x", (wp.minX+wp.maxX)/2, "y", wp.maxY, "map", wp.goMap,
                "minX", wp.minX, "maxX", wp.maxX, "minY", wp.minY, "maxY", wp.maxY));
        }
        return Map.ofEntries(Map.entry("type", "map"), Map.entry("id", m.mapId), Map.entry("name", m.mapName),
            Map.entry("width", m.mapWidth), Map.entry("height", m.mapHeight), Map.entry("tileId", m.tileId), Map.entry("bgId", m.bgId),
            Map.entry("bgType", m.bgType), Map.entry("planet", m.planetId),
            Map.entry("tiles", m.tileMap), Map.entry("top", m.tileTop), Map.entry("npcs", npcList), Map.entry("portals", portals),Map.entry("decorations",decorations));
    }
}
