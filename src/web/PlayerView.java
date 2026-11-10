package web;

import item.Item;
import item.Template.SkillTemplate;
import java.util.*;
import player.Player;
import server.Manager;
import skill.Skill;

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
    static Map<String, Object> task(Player player) {
        var task = player.playerTask.taskMain;
        if (task == null) return Map.of();
        return Map.of("id", task.id, "name", Objects.toString(task.name, "Nhiệm vụ"),
            "detail", Objects.toString(task.detail, ""), "index", task.index, "subtasks", task.subTasks);
    }

    static Object taskMessage(Player player) {
        return Map.of("type", "task", "task", task(player));
    }

    static Object stats(Player p) {
        Map<Byte, SkillTemplate> templates = new LinkedHashMap<>();
        Manager.NCLASS.stream().filter(c -> c.classId == p.gender).findFirst()
                .ifPresent(c -> c.skillTemplatess.forEach(template -> templates.put(template.id, template)));
        Map<Byte, Skill> saved = new HashMap<>();
        for (var skill : p.playerSkill.skills) if (skill.template != null) {
            templates.putIfAbsent(skill.template.id, skill.template);
            saved.put(skill.template.id, skill);
        }
        List<Object> skills = new ArrayList<>();
        for (var template : templates.values()) {
            Skill skill = saved.get(template.id);
            skills.add(Map.of("id", template.id, "name", Objects.toString(template.name, "Kỹ năng " + template.id),
                    "level", skill == null ? 0 : skill.point, "cooldown", skill == null ? 0 : skill.coolDown,
                    "lastUse", skill == null ? 0 : skill.lastTimeUseThisSkill, "mana", skill == null ? 0 : skill.manaUse,
                    "type", template.type, "icon", template.iconId));
        }
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("type", "stats"); out.put("hp",p.nPoint.hp);out.put("maxHp",p.nPoint.hpMax);out.put("mp",p.nPoint.mp);out.put("maxMp",p.nPoint.mpMax);
        out.put("power",p.nPoint.power);out.put("potential",p.nPoint.tiemNang);out.put("damage",p.nPoint.dame);out.put("defense",p.nPoint.def);out.put("crit",p.nPoint.crit);
        out.put("gold",p.inventory.gold);out.put("gem",p.inventory.gem);out.put("ruby",p.inventory.ruby);out.put("skills",skills);
        out.put("skillShortcuts", p.playerSkill.skillShortCut);
        if (p.playerTask.taskMain != null) out.put("task", task(p));
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
        for(var d:p.zone.getItemMapsForPlayer(p)) drops.add(Map.of("id",d.itemMapId,"template",d.itemTemplate.id,"icon",d.itemTemplate.iconID,"x",d.x,"y",d.y));
        return Map.of("type","state","time",System.currentTimeMillis(),"tick",tick,"map",p.zone.map.mapId,"zone",p.zone.zoneId,"online",online,"players",actors,"mobs",mobs,"drops",drops);
    }
}
