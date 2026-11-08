package web;

import item.Item;
import java.util.*;
import player.Player;

final class PlayerView {
    static Map<String,Object> actor(Player p) {
        int facing = p.getSession() instanceof WebSession s ? s.facing : 1;
        return Map.ofEntries(Map.entry("id", p.id),Map.entry("name", p.name == null ? "" : p.name),
            Map.entry("x", p.location.x),Map.entry("y", p.location.y),Map.entry("hp", p.nPoint.hp),Map.entry("maxHp", p.nPoint.hpMax),
            Map.entry("head", p.getHead()),Map.entry("body", p.getBody()),Map.entry("leg", p.getLeg()),Map.entry("gender", p.gender),
            Map.entry("facing", facing),Map.entry("boss", p.isBoss),Map.entry("dead", p.isDie()));
    }
    static Object item(Item i, int index) {
        if (i == null || !i.isNotNullItem()) return Map.of("index", index, "id", -1);
        List<String> options = new ArrayList<>();
        for (var o : i.itemOptions) options.add(o.optionTemplate.name.replace("#", String.valueOf(o.param)));
        return Map.of("index", index, "id", i.template.id, "name", i.template.name, "icon", i.template.iconID, "quantity", i.quantity, "options", options, "type", i.template.type);
    }
    static List<Object> items(List<Item> source) {
        List<Object> out = new ArrayList<>();
        for(int i=0;i<source.size();i++) out.add(item(source.get(i),i));
        return out;
    }
    static Object inventory(Player p) { return Map.of("type","inventory","body", items(p.inventory.itemsBody),"bag",items(p.inventory.itemsBag),"box",items(p.inventory.itemsBox)); }
    static Object stats(Player p) {
        List<Object> skills = new ArrayList<>();
        for(var s:p.playerSkill.skills) if (s.template != null) {
            var template=server.Manager.NCLASS.stream().filter(c->c.classId==p.gender).flatMap(c->c.skillTemplatess.stream()).filter(t->t.id==s.template.id).findFirst().orElse(s.template);
            skills.add(Map.ofEntries(Map.entry("id",template.id),Map.entry("name",template.name==null?"Kỹ năng "+template.id:template.name),Map.entry("level",s.point),Map.entry("cooldown",s.coolDown),Map.entry("lastUse",s.lastTimeUseThisSkill),Map.entry("mana",s.manaUse),Map.entry("icon",template.iconId)));
        }
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("type", "stats"); out.put("hp",p.nPoint.hp);out.put("maxHp",p.nPoint.hpMax);out.put("mp",p.nPoint.mp);out.put("maxMp",p.nPoint.mpMax);
        out.put("power",p.nPoint.power);out.put("potential",p.nPoint.tiemNang);out.put("damage",p.nPoint.dame);out.put("defense",p.nPoint.def);out.put("crit",p.nPoint.crit);
        out.put("gold",p.inventory.gold);out.put("gem",p.inventory.gem);out.put("ruby",p.inventory.ruby);out.put("skills",skills);
        if(p.playerTask.taskMain!=null) {
            var t=p.playerTask.taskMain;out.put("task",Map.of("name",Objects.toString(t.name,"Nhiệm vụ"),"detail",Objects.toString(t.detail,""),"index",t.index,"subtasks",t.subTasks));
        }
        if(p.pet!=null) out.put("pet",actor(p.pet));
        if(p.clan!=null) out.put("clan",Map.of("id",p.clan.id,"name",p.clan.name));
        return out;
    }
    static Object state(Player p, int online, long tick) {
        List<Object> actors = new ArrayList<>();
        for(var a : p.zone.getHumanoids()) if(a.location != null && a.nPoint != null) actors.add(actor(a));
        List<Object> mobs = new ArrayList<>();
        for(var m:p.zone.mobs) mobs.add(Map.of("id",m.id,"template",m.tempId,"name",GameCatalog.mobNames.getOrDefault(m.tempId,"Quái"),"x",m.location.x,"y",m.location.y,"hp",m.point.hp,"maxHp",m.point.maxHp,"dead",m.isDie()));
        List<Object> drops = new ArrayList<>();
        for(var d:p.zone.items) if(d.itemTemplate!=null) drops.add(Map.of("id",d.itemMapId,"template",d.itemTemplate.id,"icon",d.itemTemplate.iconID,"x",d.x,"y",d.y));
        return Map.of("type","state","time",System.currentTimeMillis(),"tick",tick,"map",p.zone.map.mapId,"zone",p.zone.zoneId,"online",online,"players",actors,"mobs",mobs,"drops",drops);
    }
}
