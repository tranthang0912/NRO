package server;

import radar.OptionCard;
import services.RadarService;
import radar.RadarCard;
import kygui.ConsignItem;
import kygui.ConsignShopManager;
import database.DatabaseManager;
import consts.ConstPlayer;
import consts.ConstMap;
import data.DataGame;
import database.ShopDAO;
import item.Template.*;
import clan.Clan;
import clan.ClanMember;

import static data.DataGame.MAP_MOUNT_NUM;
import static database.DatabaseManager.DB_NAME;

import player.GiftCodeSystem;
import managers.GiftCodeManager;
import intrinsic.Intrinsic;
import item.Item;
import item.Item.ItemOption;
import map.WayPoint;
import npc.Npc;
import npc.NpcFactory;
import shop.Shop;
import skill.NClass;
import skill.Skill;
import task.SideTaskTemplate;
import task.SubTaskMain;
import task.TaskMain;
import map.Service.MapService;
import utils.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import map.Zone;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import npc.NonInteractiveNPC;
import player.badges.BagesTemplate;
import task.BadgesTaskTemplate;
import task.ClanTaskTemplate;
import matches.TOP;
public final class Manager {

    private static Manager instance;
    public static byte SERVER = 1;
    public static byte SECOND_WAIT_LOGIN = 5;
    public static int MAX_PER_IP = 10;
    public static int MAX_PLAYER = 2000;
    public static byte RATE_EXP_SERVER = 1;
    public static boolean LOCAL = false;
    public static boolean TEST = false;
    public static boolean DAO_AUTO_UPDATER = false;
    public static MapTemplate[] MAP_TEMPLATES;
    public static final List<map.Map> MAPS = new ArrayList<>();
    private final ScheduledExecutorService mapUpdater = Executors.newSingleThreadScheduledExecutor();
    public static final List<ItemOptionTemplate> ITEM_OPTION_TEMPLATES = new ArrayList<>();
    public static final List<ArrHead2Frames> ARR_HEAD_2_FRAMES = new ArrayList<>();
    public static final Map<String, Byte> IMAGES_BY_NAME = new HashMap<>();
    public static final List<ItemTemplate> ITEM_TEMPLATES = new ArrayList<>();
    public static final List<MobTemplate> MOB_TEMPLATES = new ArrayList<>();
    public static final List<NpcTemplate> NPC_TEMPLATES = new ArrayList<>();
    public static final List<TaskMain> TASKS = new ArrayList<>();
    public static final List<SideTaskTemplate> SIDE_TASKS_TEMPLATE = new ArrayList<>();
    public static final List<ClanTaskTemplate> CLAN_TASKS_TEMPLATE = new ArrayList<>();
    public static final List<AchievementTemplate> ACHIEVEMENT_TEMPLATE = new ArrayList<>();
    public static final List<Intrinsic> INTRINSICS = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_TD = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_NM = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_XD = new ArrayList<>();
    public static final List<HeadAvatar> HEAD_AVATARS = new ArrayList<>();
    public static final List<BgItem> BG_ITEMS = new ArrayList<>();
    public static final List<FlagBag> FLAGS_BAGS = new ArrayList<>();
    public static final List<NClass> NCLASS = new ArrayList<>();
    public static final List<Npc> NPCS = new ArrayList<>();
    public static List<Shop> SHOPS = new ArrayList<>();
    public static final List<Clan> CLANS = new ArrayList<>();
    public static final List<String> NOTIFY = new ArrayList<>();
    public static final List<BadgesTaskTemplate> TASKS_BADGES_TEMPLATE = new ArrayList<>();
    public static final List<BagesTemplate> BAGES_TEMPLATES = new ArrayList<>();
    public static final short[][] trangBiKichHoat = {{0, 6, 21, 27}, {1, 7, 22, 28}, {2, 8, 23, 29}};
    public static final short[] aotd = {138, 139, 230, 231, 232, 233, 555};
    public static final short[] quantd = {142, 143, 242, 243, 244, 245, 556};
    public static final short[] gangtd = {146, 147, 254, 255, 256, 257, 562};
    public static final short[] giaytd = {150, 151, 266, 267, 268, 269, 563};
    public static final short[] aoxd = {170, 171, 238, 239, 240, 241, 559};
    public static final short[] quanxd = {174, 175, 250, 251, 252, 253, 560};
    public static final short[] gangxd = {178, 179, 262, 263, 264, 265, 566};
    public static final short[] giayxd = {182, 183, 274, 275, 276, 277, 567};
    public static final short[] aonm = {154, 155, 234, 235, 236, 237, 557};
    public static final short[] quannm = {158, 159, 246, 247, 248, 249, 558};
    public static final short[] gangnm = {162, 163, 258, 259, 260, 261, 564};
    public static final short[] giaynm = {166, 167, 270, 271, 272, 273, 565};
    public static final short[] radaSKHVip = {186, 187, 278, 279, 280, 281, 561};
    public static final short[][][] doSKHVip = {{aotd, quantd, gangtd, giaytd}, {aonm, quannm, gangnm, giaynm},
    {aoxd, quanxd, gangxd, giayxd}};
    public static final int[][][] LIST_ITEM_CLOTHES = {
        {{0, 33, 3, 34, 136, 137, 138, 139, 230, 231, 232, 233, 555}, {6, 35, 9, 36, 140, 141, 142, 143, 242, 243, 244, 245, 556}, {21, 24, 37, 38, 144, 145, 146, 147, 254, 255, 256, 257, 562}, {27, 30, 39, 40, 148, 149, 150, 151, 266, 267, 268, 269, 563}, {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561}},
        {{1, 41, 4, 42, 152, 153, 154, 155, 234, 235, 236, 237, 557}, {7, 43, 10, 44, 156, 157, 158, 159, 246, 247, 248, 249, 558}, {22, 46, 25, 45, 160, 161, 162, 163, 258, 259, 260, 261, 564}, {28, 47, 31, 48, 164, 165, 166, 167, 270, 271, 272, 273, 565}, {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561}},
        {{2, 49, 5, 50, 168, 169, 170, 171, 238, 239, 240, 241, 559}, {8, 51, 11, 52, 172, 173, 174, 175, 250, 251, 252, 253, 560}, {23, 53, 26, 54, 176, 177, 178, 179, 262, 263, 264, 265, 566}, {29, 55, 32, 56, 180, 181, 182, 183, 274, 275, 276, 277, 567}, {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561}}
    };
    public static final short[] Ao_TraiDat = {0, 33, 3, 34, 136, 137, 138, 139, 230, 231, 232, 233, 555, 650};
    public static final short[] Quan_TraiDat = {6, 35, 9, 36, 140, 141, 142, 143, 242, 243, 244, 245, 556, 651};
    public static final short[] Gang_TraiDat = {21, 24, 37, 38, 144, 145, 146, 147, 254, 255, 256, 257, 562, 657};
    public static final short[] Giay_TraiDat = {27, 30, 39, 40, 148, 149, 150, 151, 266, 267, 268, 269, 563, 658};
    public static final short[] Ao_Namek = {1, 41, 4, 42, 152, 153, 154, 155, 234, 235, 236, 237, 557, 652};
    public static final short[] Quan_Namek = {7, 43, 10, 44, 156, 157, 158, 159, 246, 247, 248, 249, 558, 653};
    public static final short[] Gang_Namek = {22, 46, 25, 45, 160, 161, 162, 163, 258, 259, 260, 261, 564, 659};
    public static final short[] Giay_Namek = {28, 47, 31, 48, 164, 165, 166, 167, 270, 271, 272, 273, 565, 660};
    public static final short[] Ao_Xayda = {2, 49, 5, 50, 168, 169, 170, 171, 238, 239, 240, 241, 559, 654};
    public static final short[] Quan_Xayda = {8, 51, 11, 52, 172, 173, 174, 175, 250, 251, 252, 253, 560, 655};
    public static final short[] Gang_Xayda = {23, 53, 26, 54, 176, 177, 178, 179, 262, 263, 264, 265, 566, 661};
    public static final short[] Giay_Xayda = {29, 55, 32, 56, 180, 181, 182, 183, 274, 275, 276, 277, 567, 662};
    public static final short[] Rada_Gender = {12, 57, 58, 59, 184, 185, 186, 187, 278, 279, 280, 281, 561, 656};
    public static final short[][][] TrangBiKichHoat = {{Ao_TraiDat, Ao_Namek, Ao_Xayda,}, {Quan_TraiDat, Quan_Namek, Quan_Xayda},
    {Gang_TraiDat, Gang_Namek, Gang_Xayda}, {Giay_TraiDat, Giay_Namek, Giay_Xayda}, {Rada_Gender, Rada_Gender, Rada_Gender, Rada_Gender}};
    public static Manager gI() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }

    private Manager() {
        try {
            loadProperties();

        } catch (IOException ex) {
            Logger.logException(Manager.class, ex, "Lỗi load properites");
            throw new IllegalStateException("Cannot load server configuration", ex);
        }

        this.loadDatabase();
        NpcFactory.createNpcConMeo();
        NpcFactory.createNpcRongThieng();
        this.initMap(); 
    }
    private void initMap() {
       int[][] tileTyleTop = readTileIndexTileType(ConstMap.TILE_TOP);
       int[][] tileLeft = readTileIndexTileType(ConstMap.TILE_LEFT);
       int[][] tileRight = readTileIndexTileType(ConstMap.TILE_RIGHT);
       for (MapTemplate mapTemp : MAP_TEMPLATES) {
        int[][] tileMap = readTileMap(mapTemp.id);
        int[] tileTop = tileTyleTop[mapTemp.tileId - 1];
        map.Map map = new map.Map(
        mapTemp.id, mapTemp.name, mapTemp.planetId, mapTemp.tileId,
        mapTemp.bgId, mapTemp.bgType, mapTemp.type, tileMap, tileTop,
        tileLeft[mapTemp.tileId - 1], tileRight[mapTemp.tileId - 1],
        mapTemp.zones, mapTemp.maxPlayerPerZone, mapTemp.wayPoints
        );
        MAPS.add(map);
        map.initMob(mapTemp.mobTemp, mapTemp.mobLevel, mapTemp.mobHp, mapTemp.mobX, mapTemp.mobY);
        map.initNpc(mapTemp.npcId, mapTemp.npcX, mapTemp.npcY);
    }
    new NonInteractiveNPC().initNonInteractiveNPC();
    if (web.WebConfig.ENABLED) return;
    new Thread(() -> {
        try {
            while (!Maintenance.isRunning) {
                long st = System.currentTimeMillis();
                for (map.Map map : MAPS) {
                    for (Zone zone : map.zones) {
                        try {
                            zone.update();
                        } catch (Exception e) {
                            Logger.log(Logger.RED, "Lỗi khi cập nhật zone: " + e.getMessage());
                        }
                    }
                }
                long timeDo = System.currentTimeMillis() - st;
                long sleepTime = 1000 - timeDo;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            }
         } catch (InterruptedException e) {
            Logger.log(Logger.RED, "Thread cập nhật map bị gián đoạn: " + e.getMessage());
            Thread.currentThread().interrupt();
         } catch (Exception e) {
            Logger.log(Logger.RED, "Lỗi không xác định trong thread cập nhật map: " + e.getMessage());
         }
       }, "Update Maps").start();
   }
    private void loadDatabase() {
        long st = System.currentTimeMillis();
        JSONArray dataArray;
        JSONObject dataObject;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try (Connection ConnectionDatabase = DatabaseManager.getConnection()) {
            ps = ConnectionDatabase.prepareStatement("select * from part");
            rs = ps.executeQuery();
            List<Part> parts = new ArrayList<>();
            while (rs.next()) {
                Part part = new Part();
                part.id = rs.getShort("id");
                part.type = rs.getByte("type");
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data").replaceAll("\\\"", ""));
                for (int j = 0; j < dataArray.size(); j++) {
                    JSONArray pd = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                    part.partDetails.add(new PartDetail(Short.parseShort(String.valueOf(pd.get(0))),
                            Byte.parseByte(String.valueOf(pd.get(1))),
                            Byte.parseByte(String.valueOf(pd.get(2)))));
                    pd.clear();
                }
                parts.add(part);
                dataArray.clear();
            }
            DataOutputStream dos = new DataOutputStream(new FileOutputStream("data/update_data/part"));
            dos.writeShort(parts.size());
            for (Part part : parts) {
                dos.writeByte(part.type);
                for (PartDetail partDetail : part.partDetails) {
                    dos.writeShort(partDetail.iconId);
                    dos.writeByte(partDetail.dx);
                    dos.writeByte(partDetail.dy);
                }
            }
            dos.flush();
            ps = ConnectionDatabase.prepareStatement("select * from bg_item_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                BgItem bgItem = new BgItem();
                bgItem.id = rs.getInt("id");
                bgItem.layer = rs.getByte("layer");
                bgItem.dx = rs.getShort("dx");
                bgItem.dy = rs.getShort("dy");
                bgItem.idImage = rs.getShort("image_id");
                BG_ITEMS.add(bgItem);
            }

            ps = ConnectionDatabase.prepareStatement("select * from array_head_2_frames");
            rs = ps.executeQuery();
            while (rs.next()) {
                ArrHead2Frames arrHead2Frames = new ArrHead2Frames();
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data"));
                for (int i = 0; i < dataArray.size(); i++) {
                    arrHead2Frames.frames.add(Integer.valueOf(dataArray.get(i).toString()));
                }
                ARR_HEAD_2_FRAMES.add(arrHead2Frames);
            }
            ps = ConnectionDatabase.prepareStatement("select * from clan");
            rs = ps.executeQuery();
            while (rs.next()) {
                Clan clan = new Clan();
                clan.id = rs.getInt("id");
                clan.name = rs.getString("name");
                clan.name2 = rs.getString("name_2");
                clan.slogan = rs.getString("slogan");
                clan.imgId = rs.getByte("img_id");
                clan.powerPoint = rs.getLong("power_point");
                clan.maxMember = rs.getByte("max_member");
                clan.capsuleClan = rs.getInt("clan_point");
                clan.level = rs.getByte("level");
                if (clan.level < 1) {
                    clan.level = 1;
                }
                clan.createTime = (int) (rs.getTimestamp("create_time").getTime() / 1000);
                dataArray = (JSONArray) JSONValue.parse(rs.getString("members"));
                for (int i = 0; i < dataArray.size(); i++) {
                    dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                    ClanMember cm = new ClanMember();
                    cm.clan = clan;
                    cm.id = Integer.parseInt(String.valueOf(dataObject.get("id")));
                    cm.name = String.valueOf(dataObject.get("name"));
                    cm.head = Short.parseShort(String.valueOf(dataObject.get("head")));
                    cm.body = Short.parseShort(String.valueOf(dataObject.get("body")));
                    cm.leg = Short.parseShort(String.valueOf(dataObject.get("leg")));
                    cm.role = Byte.parseByte(String.valueOf(dataObject.get("role")));
                    cm.donate = Integer.parseInt(String.valueOf(dataObject.get("donate")));
                    cm.receiveDonate = Integer.parseInt(String.valueOf(dataObject.get("receive_donate")));
                    cm.memberPoint = Integer.parseInt(String.valueOf(dataObject.get("member_point")));
                    cm.clanPoint = Integer.parseInt(String.valueOf(dataObject.get("clan_point")));
                    cm.joinTime = Integer.parseInt(String.valueOf(dataObject.get("join_time")));
                    cm.timeAskPea = Long.parseLong(String.valueOf(dataObject.get("ask_pea_time")));
                    try {
                        cm.powerPoint = Long.parseLong(String.valueOf(dataObject.get("power")));
                    } catch (NumberFormatException e) {
                    }
                    clan.addClanMember(cm);
                }
                dataArray.clear();
                CLANS.add(clan);
            }

            ps = ConnectionDatabase.prepareStatement("select id from clan order by id desc limit 1");
            rs = ps.executeQuery();
            if (rs.next()) {
                Clan.NEXT_ID = rs.getInt("id") + 1;
            }
            ps = ConnectionDatabase.prepareStatement("select * from skill_template order by nclass_id, slot");
            rs = ps.executeQuery();
            byte nClassId = -1;
            NClass nClass = null;
            while (rs.next()) {
                byte id = rs.getByte("nclass_id");
                if (id != nClassId) {
                    nClassId = id;
                    nClass = new NClass();
                    nClass.name = id == ConstPlayer.TRAI_DAT ? "Trái Đất" : id == ConstPlayer.NAMEC ? "Namếc" : "Xayda";
                    nClass.classId = nClassId;
                    NCLASS.add(nClass);
                }
                SkillTemplate skillTemplate = new SkillTemplate();
                skillTemplate.classId = nClassId;
                skillTemplate.id = rs.getByte("id");
                skillTemplate.name = rs.getString("name");
                skillTemplate.maxPoint = rs.getByte("max_point");
                skillTemplate.manaUseType = rs.getByte("mana_use_type");
                skillTemplate.type = rs.getByte("type");
                skillTemplate.iconId = rs.getShort("icon_id");
                skillTemplate.damInfo = rs.getString("dam_info");
                nClass.skillTemplatess.add(skillTemplate);

                dataArray = (JSONArray) JSONValue.parse(
                        rs.getString("skills")
                                .replaceAll("\\[\"", "[")
                                .replaceAll("\"\\[", "[")
                                .replaceAll("\"\\]", "]")
                                .replaceAll("\\]\"", "]")
                                .replaceAll("\\}\",\"\\{", "},{")
                );
                for (int j = 0; j < dataArray.size(); j++) {
                    JSONObject dts = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(j)));
                    Skill skill = new Skill();
                    skill.template = skillTemplate;
                    skill.skillId = Short.parseShort(String.valueOf(dts.get("id")));
                    skill.point = Byte.parseByte(String.valueOf(dts.get("point")));
                    skill.powRequire = Long.parseLong(String.valueOf(dts.get("power_require")));
                    skill.manaUse = Integer.parseInt(String.valueOf(dts.get("mana_use")));
                    skill.coolDown = Integer.parseInt(String.valueOf(dts.get("cool_down")));
                    skill.dx = Integer.parseInt(String.valueOf(dts.get("dx")));
                    skill.dy = Integer.parseInt(String.valueOf(dts.get("dy")));
                    skill.maxFight = Integer.parseInt(String.valueOf(dts.get("max_fight")));
                    skill.damage = Short.parseShort(String.valueOf(dts.get("damage")));
                    skill.price = Short.parseShort(String.valueOf(dts.get("price")));
                    skill.moreInfo = String.valueOf(dts.get("info"));
                    skillTemplate.skillss.add(skill);
                }
            }

            ps = ConnectionDatabase.prepareStatement("select * from head_avatar");
            rs = ps.executeQuery();
            while (rs.next()) {
                HeadAvatar headAvatar = new HeadAvatar(rs.getInt("head_id"), rs.getInt("avatar_id"));
                HEAD_AVATARS.add(headAvatar);
            }

            ps = ConnectionDatabase.prepareStatement("select * from flag_bag");
            rs = ps.executeQuery();
            while (rs.next()) {
                FlagBag flagBag = new FlagBag();
                flagBag.id = rs.getInt("id");
                flagBag.name = rs.getString("name");
                flagBag.gold = rs.getInt("gold");
                flagBag.gem = rs.getInt("gem");
                flagBag.iconId = rs.getShort("icon_id");
                String[] iconData = rs.getString("icon_data").split(",");
                flagBag.iconEffect = new short[iconData.length];
                for (int j = 0; j < iconData.length; j++) {
                    flagBag.iconEffect[j] = Short.parseShort(iconData[j].trim());
                }
                FLAGS_BAGS.add(flagBag);
            }
            ps = ConnectionDatabase.prepareStatement("select * from intrinsic");
            rs = ps.executeQuery();
            while (rs.next()) {
                Intrinsic intrinsic = new Intrinsic();
                intrinsic.id = rs.getByte("id");
                intrinsic.name = rs.getString("name");
                intrinsic.paramFrom1 = rs.getShort("param_from_1");
                intrinsic.paramTo1 = rs.getShort("param_to_1");
                intrinsic.paramFrom2 = rs.getShort("param_from_2");
                intrinsic.paramTo2 = rs.getShort("param_to_2");
                intrinsic.icon = rs.getShort("icon");
                intrinsic.gender = rs.getByte("gender");
                switch (intrinsic.gender) {
                    case ConstPlayer.TRAI_DAT ->
                        INTRINSIC_TD.add(intrinsic);
                    case ConstPlayer.NAMEC ->
                        INTRINSIC_NM.add(intrinsic);
                    case ConstPlayer.XAYDA ->
                        INTRINSIC_XD.add(intrinsic);
                    default -> {
                        INTRINSIC_TD.add(intrinsic);
                        INTRINSIC_NM.add(intrinsic);
                        INTRINSIC_XD.add(intrinsic);
                    }
                }
                INTRINSICS.add(intrinsic);
            }

            ps = ConnectionDatabase.prepareStatement("SELECT id, task_main_template.name, detail, "
                    + "task_sub_template.name AS sub_name, max_count, notify, npc_id, map "
                    + "FROM task_main_template JOIN task_sub_template ON task_main_template.id = "
                    + "task_sub_template.task_main_id ORDER BY task_main_template.id, task_sub_template.NguyenTanTaiPro");
            rs = ps.executeQuery();
            int taskId = -1;
            TaskMain task = null;
            while (rs.next()) {
                int id = rs.getInt("id");
                if (id != taskId) {
                    taskId = id;
                    task = new TaskMain();
                    task.id = taskId;
                    task.name = rs.getString("name");
                    task.detail = rs.getString("detail");
                    TASKS.add(task);
                }
                SubTaskMain subTask = new SubTaskMain();
                subTask.name = rs.getString("sub_name");
                subTask.maxCount = rs.getShort("max_count");
                subTask.notify = rs.getString("notify");
                subTask.npcId = rs.getByte("npc_id");
                subTask.mapId = rs.getShort("map");
                task.subTasks.add(subTask);
            }
            ps = ConnectionDatabase.prepareStatement("select * from side_task_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                SideTaskTemplate sideTask = new SideTaskTemplate();
                sideTask.id = rs.getInt("id");
                sideTask.name = rs.getString("name");
                String[] mc1 = rs.getString("max_count_lv1").split("-");
                String[] mc2 = rs.getString("max_count_lv2").split("-");
                String[] mc3 = rs.getString("max_count_lv3").split("-");
                String[] mc4 = rs.getString("max_count_lv4").split("-");
                String[] mc5 = rs.getString("max_count_lv5").split("-");
                sideTask.count[0][0] = Integer.parseInt(mc1[0]);
                sideTask.count[0][1] = Integer.parseInt(mc1[1]);
                sideTask.count[1][0] = Integer.parseInt(mc2[0]);
                sideTask.count[1][1] = Integer.parseInt(mc2[1]);
                sideTask.count[2][0] = Integer.parseInt(mc3[0]);
                sideTask.count[2][1] = Integer.parseInt(mc3[1]);
                sideTask.count[3][0] = Integer.parseInt(mc4[0]);
                sideTask.count[3][1] = Integer.parseInt(mc4[1]);
                sideTask.count[4][0] = Integer.parseInt(mc5[0]);
                sideTask.count[4][1] = Integer.parseInt(mc5[1]);
                SIDE_TASKS_TEMPLATE.add(sideTask);
            }
            ps = ConnectionDatabase.prepareStatement("select * from task_badges_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                BadgesTaskTemplate badgesTaskTemplate = new BadgesTaskTemplate();
                badgesTaskTemplate.id = rs.getInt("id");
                badgesTaskTemplate.name = rs.getString("NAME");
                badgesTaskTemplate.count = rs.getInt("maxCount");
                badgesTaskTemplate.idbadgesReward = rs.getInt("idbadgesReward");
                TASKS_BADGES_TEMPLATE.add(badgesTaskTemplate);
            }

            ps = ConnectionDatabase.prepareStatement("select * from clan_task_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                ClanTaskTemplate clanTask = new ClanTaskTemplate();
                clanTask.id = rs.getInt("id");
                clanTask.name = rs.getString("name");
                String[] mc1 = rs.getString("max_count_lv1").split("-");
                String[] mc2 = rs.getString("max_count_lv2").split("-");
                String[] mc3 = rs.getString("max_count_lv3").split("-");
                String[] mc4 = rs.getString("max_count_lv4").split("-");
                String[] mc5 = rs.getString("max_count_lv5").split("-");
                clanTask.count[0][0] = Integer.parseInt(mc1[0]);
                clanTask.count[0][1] = Integer.parseInt(mc1[1]);
                clanTask.count[1][0] = Integer.parseInt(mc2[0]);
                clanTask.count[1][1] = Integer.parseInt(mc2[1]);
                clanTask.count[2][0] = Integer.parseInt(mc3[0]);
                clanTask.count[2][1] = Integer.parseInt(mc3[1]);
                clanTask.count[3][0] = Integer.parseInt(mc4[0]);
                clanTask.count[3][1] = Integer.parseInt(mc4[1]);
                clanTask.count[4][0] = Integer.parseInt(mc5[0]);
                clanTask.count[4][1] = Integer.parseInt(mc5[1]);
                CLAN_TASKS_TEMPLATE.add(clanTask);
            }

            ps = ConnectionDatabase.prepareStatement("select * from achievement_template ORDER BY id");
            rs = ps.executeQuery();
            while (rs.next()) {
                ACHIEVEMENT_TEMPLATE.add(new AchievementTemplate(rs.getString("info1"), rs.getString("info2"), rs.getInt("money"), rs.getLong("max_count")));
            }
            int batchSize = 750;
            int offset = 0;

            try {
                while (true) {
                    ps = ConnectionDatabase.prepareStatement("SELECT * FROM item_template ORDER BY id LIMIT ? OFFSET ?");
                    ps.setInt(1, batchSize);  
                    ps.setInt(2, offset);     
                    rs = ps.executeQuery();
                    if (!rs.next()) {
                        break;
                    }
                    do {
                        ItemTemplate itemTemp = new ItemTemplate();
                        itemTemp.id = rs.getShort("id");
                        itemTemp.type = rs.getByte("type");
                        itemTemp.gender = rs.getByte("gender");
                        itemTemp.name = rs.getString("name");
                        itemTemp.description = rs.getString("description");
                        itemTemp.level = rs.getByte("level");
                        itemTemp.iconID = rs.getShort("icon_id");
                        itemTemp.part = rs.getShort("part");
                        itemTemp.isUpToUp = rs.getBoolean("is_up_to_up");
                        itemTemp.strRequire = rs.getInt("power_require");
                        itemTemp.gold = rs.getInt("gold");
                        itemTemp.gem = rs.getInt("gem");
                        itemTemp.head = rs.getInt("head");
                        itemTemp.body = rs.getInt("body");
                        itemTemp.leg = rs.getInt("leg");

                        ITEM_TEMPLATES.add(itemTemp);
                    } while (rs.next());
                    offset += batchSize;
                }

            } catch (SQLException e) {
                Logger.error("Error loading item templates: " + e.getMessage());
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                } catch (SQLException e) {
                    Logger.error("Error closing resources: " + e.getMessage());
                }
            }

            ps = ConnectionDatabase.prepareStatement("select id, name from item_option_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                ItemOptionTemplate optionTemp = new ItemOptionTemplate();
                optionTemp.id = rs.getInt("id");
                optionTemp.name = rs.getString("name");
                ITEM_OPTION_TEMPLATES.add(optionTemp);
            }

            SHOPS = ShopDAO.getShops(ConnectionDatabase);

            ps = ConnectionDatabase.prepareStatement("select * from notify order by id desc");
            rs = ps.executeQuery();
            while (rs.next()) {
                NOTIFY.add(rs.getString("name") + "<>" + rs.getString("text"));
            }

            ps = ConnectionDatabase.prepareStatement("select name, n_frame from img_by_name");
            rs = ps.executeQuery();
            while (rs.next()) {
                IMAGES_BY_NAME.put(rs.getString("name"), rs.getByte("n_frame"));
            }
            for (ItemTemplate item : ITEM_TEMPLATES) {
                if (item.type == 23 && getNFrameImageByName("mount_" + item.part + "_0") != 0) {
                    MAP_MOUNT_NUM.put(item.id, (short) (item.part + 30000));
                }
            }

            ps = ConnectionDatabase.prepareStatement("SELECT * FROM shop_ky_gui");
            rs = ps.executeQuery();
            while (rs.next()) {
                int i = rs.getInt("id");
                int idPl = rs.getInt("player_id");
                byte tab = rs.getByte("tab");
                short itemId = rs.getShort("item_id");
                int gold = rs.getInt("gold");
                int gem = rs.getInt("gem");
                int quantity = rs.getInt("quantity");
                byte isUp = rs.getByte("isUpTop");
                boolean isBuy = rs.getByte("isBuy") == 1;
                List<Item.ItemOption> op = new ArrayList<>();
                JSONArray jsa2 = (JSONArray) JSONValue.parse(rs.getString("itemOption"));
                for (int j = 0; j < jsa2.size(); ++j) {
                    JSONObject jso2 = (JSONObject) jsa2.get(j);
                    int idOptions = Integer.parseInt(jso2.get("id").toString());
                    int param = Integer.parseInt(jso2.get("param").toString());
                    op.add(new Item.ItemOption(idOptions, param));
                }
                ConsignShopManager.gI().listItem.add(new ConsignItem(i, itemId, idPl, tab, gold, gem, quantity, isUp, op, isBuy));
            }

            ps = ConnectionDatabase.prepareStatement("select * from mob_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                MobTemplate mobTemp = new MobTemplate();
                mobTemp.id = rs.getByte("id");
                mobTemp.type = rs.getByte("type");
                mobTemp.name = rs.getString("name");
                mobTemp.hp = rs.getInt("hp");
                mobTemp.rangeMove = rs.getByte("range_move");
                mobTemp.speed = rs.getByte("speed");
                mobTemp.dartType = rs.getByte("dart_type");
                mobTemp.percentDame = rs.getByte("percent_dame");
                mobTemp.percentTiemNang = rs.getByte("percent_tiem_nang");
                MOB_TEMPLATES.add(mobTemp);
            }

            ps = ConnectionDatabase.prepareStatement("select * from npc_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                NpcTemplate npcTemp = new NpcTemplate();
                npcTemp.id = rs.getByte("id");
                npcTemp.name = rs.getString("name");
                npcTemp.head = rs.getShort("head");
                npcTemp.body = rs.getShort("body");
                npcTemp.leg = rs.getShort("leg");
                npcTemp.avatar = rs.getInt("avatar");
                NPC_TEMPLATES.add(npcTemp);
            }
            ps = ConnectionDatabase.prepareStatement("select * from data_badges");
            rs = ps.executeQuery();
            while (rs.next()) {
                BagesTemplate template = new BagesTemplate();
                template.id = rs.getInt("id");
                template.idEffect = rs.getInt("idEffect");
                template.idItem = rs.getInt("idItem");
                template.NAME = rs.getString("NAME");

                JSONArray option = (JSONArray) JSONValue.parse(rs.getString("Options"));;
                if (option != null) {
                    for (int u = 0; u < option.size(); u++) {
                        JSONObject jsonobject = (JSONObject) option.get(u);
                        int optionId = Integer.parseInt(jsonobject.get("id").toString());
                        int param = Integer.parseInt(jsonobject.get("param").toString());
                        template.options.add(new Item.ItemOption(optionId, param));
                    }
                }
                BAGES_TEMPLATES.add(template);
            }
            ps = ConnectionDatabase.prepareStatement("select count(id) from map_template");
            rs = ps.executeQuery();
            if (rs.next()) {
                int countRow = rs.getShort(1);
                MAP_TEMPLATES = new MapTemplate[countRow];
                ps = ConnectionDatabase.prepareStatement("select * from map_template ORDER BY id");
                rs = ps.executeQuery();
                short i = 0;
                while (rs.next()) {
                    MapTemplate mapTemplate = new MapTemplate();
                    int mapId = rs.getInt("id");
                    String mapName = rs.getString("name");
                    mapTemplate.id = mapId;
                    mapTemplate.name = mapName;
                    mapTemplate.type = rs.getByte("type");
                    mapTemplate.planetId = rs.getByte("planet_id");
                    mapTemplate.bgType = rs.getByte("bg_type");
                    mapTemplate.tileId = rs.getByte("tile_id");
                    mapTemplate.bgId = rs.getByte("bg_id");
                    mapTemplate.zones = rs.getByte("zones");
                    mapTemplate.maxPlayerPerZone = rs.getByte("max_player");
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("waypoints")
                            .replaceAll("\\[\"\\[", "[[")
                            .replaceAll("\\]\"\\]", "]]")
                            .replaceAll("\",\"", ",")
                    );
                    for (int j = 0; j < dataArray.size(); j++) {
                        WayPoint wp = new WayPoint();
                        JSONArray dtwp = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        wp.name = String.valueOf(dtwp.get(0));
                        wp.minX = Short.parseShort(String.valueOf(dtwp.get(1)));
                        wp.minY = Short.parseShort(String.valueOf(dtwp.get(2)));
                        wp.maxX = Short.parseShort(String.valueOf(dtwp.get(3)));
                        wp.maxY = Short.parseShort(String.valueOf(dtwp.get(4)));
                        wp.isEnter = Byte.parseByte(String.valueOf(dtwp.get(5))) == 1;
                        wp.isOffline = Byte.parseByte(String.valueOf(dtwp.get(6))) == 1;
                        wp.goMap = Short.parseShort(String.valueOf(dtwp.get(7)));
                        wp.goX = Short.parseShort(String.valueOf(dtwp.get(8)));
                        wp.goY = Short.parseShort(String.valueOf(dtwp.get(9)));
                        mapTemplate.wayPoints.add(wp);
                        dtwp.clear();
                    }
                    dataArray.clear();
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("mobs").replaceAll("\\\"", ""));
                    mapTemplate.mobTemp = new byte[dataArray.size()];
                    mapTemplate.mobLevel = new byte[dataArray.size()];
                    mapTemplate.mobHp = new int[dataArray.size()];
                    mapTemplate.mobX = new short[dataArray.size()];
                    mapTemplate.mobY = new short[dataArray.size()];
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONArray dtm = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        mapTemplate.mobTemp[j] = Byte.parseByte(String.valueOf(dtm.get(0)));
                        mapTemplate.mobLevel[j] = Byte.parseByte(String.valueOf(dtm.get(1)));
                        mapTemplate.mobHp[j] = Integer.parseInt(String.valueOf(dtm.get(2)));
                        mapTemplate.mobX[j] = Short.parseShort(String.valueOf(dtm.get(3)));
                        mapTemplate.mobY[j] = Short.parseShort(String.valueOf(dtm.get(4)));
                        dtm.clear();
                    }
                    dataArray.clear();
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("npcs").replaceAll("\\\"", ""));
                    mapTemplate.npcId = new byte[dataArray.size()];
                    mapTemplate.npcX = new short[dataArray.size()];
                    mapTemplate.npcY = new short[dataArray.size()];
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONArray dtn = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        mapTemplate.npcId[j] = Byte.parseByte(String.valueOf(dtn.get(0)));
                        mapTemplate.npcX[j] = Short.parseShort(String.valueOf(dtn.get(1)));
                        mapTemplate.npcY[j] = Short.parseShort(String.valueOf(dtn.get(2)));
                        dtn.clear();
                    }
                    dataArray.clear();
                    MAP_TEMPLATES[i++] = mapTemplate;
                }
            }

            ps = ConnectionDatabase.prepareStatement("select * from radar");
            rs = ps.executeQuery();
            while (rs.next()) {
                RadarCard rd = new RadarCard();
                rd.Id = rs.getShort("id");
                rd.IconId = rs.getShort("iconId");
                rd.Rank = rs.getByte("rank");
                rd.Max = rs.getByte("max");
                rd.Type = rs.getByte("type");
                rd.Template = rs.getShort("mob_id");
                rd.Name = rs.getString("name");
                rd.Info = rs.getString("info");
                JSONArray arr = (JSONArray) JSONValue.parse(rs.getString("body"));
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject ob = (JSONObject) arr.get(i);
                    if (ob != null) {
                        rd.Head = Short.parseShort(ob.get("head").toString());
                        rd.Body = Short.parseShort(ob.get("body").toString());
                        rd.Leg = Short.parseShort(ob.get("leg").toString());
                        rd.Bag = Short.parseShort(ob.get("bag").toString());
                    }
                }
                rd.Options.clear();
                arr = (JSONArray) JSONValue.parse(rs.getString("options"));
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject ob = (JSONObject) arr.get(i);
                    if (ob != null) {
                        rd.Options.add(new OptionCard(Integer.parseInt(ob.get("id").toString()), Short.parseShort(ob.get("param").toString()), Byte.parseByte(ob.get("activeCard").toString())));
                    }
                }
                rd.Require = rs.getShort("require");
                rd.RequireLevel = rs.getShort("require_level");
                rd.AuraId = rs.getShort("aura_id");
                RadarService.gI().RADAR_TEMPLATE.add(rd);
            }

            ps = ConnectionDatabase.prepareStatement("SELECT * FROM giftcode");
            rs = ps.executeQuery();
            while (rs.next()) {
                GiftCodeSystem giftcode = new GiftCodeSystem();
                giftcode.code = rs.getString("code");
                giftcode.id = rs.getInt("id");
                giftcode.countLeft = rs.getInt("count_left");
                if (giftcode.countLeft == -1) {
                    giftcode.countLeft = 999999999;
                }
                giftcode.datecreate = rs.getTimestamp("datecreate");
                giftcode.dateexpired = rs.getTimestamp("expired");
                JSONArray jar = (JSONArray) JSONValue.parse(rs.getString("detail"));
                if (jar != null) {
                    for (int i = 0; i < jar.size(); ++i) {
                        JSONObject jsonObj = (JSONObject) jar.get(i);

                        int id = Integer.parseInt(jsonObj.get("id").toString());
                        int quantity = Integer.parseInt(jsonObj.get("quantity").toString());

                        JSONArray option = (JSONArray) jsonObj.get("options");
                        ArrayList<ItemOption> optionList = new ArrayList<>();

                        if (option != null) {
                            for (int u = 0; u < option.size(); u++) {
                                JSONObject jsonobject = (JSONObject) option.get(u);
                                int optionId = Integer.parseInt(jsonobject.get("id").toString());
                                int param = Integer.parseInt(jsonobject.get("param").toString());
                                optionList.add(new Item.ItemOption(optionId, param));
                            }
                        }

                        giftcode.option.put(id, optionList);
                        giftcode.detail.put(id, quantity);
                    }
                }
                GiftCodeManager.gI().listGiftCode.add(giftcode);
            }
        } catch (Exception e) {
            Logger.logException(Manager.class, e, "Database loading error");
            throw new IllegalStateException("Cannot load PostgreSQL game data", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
            }
        }
        Logger.success("Successfully loaded all database " + DatabaseManager.DB_NAME + "\n");
        Logger.success("Total database loading time -> " + (System.currentTimeMillis() - st) + " (ms)\n");

    }

    public void loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("Config.properties"));
        Object value;
        if ((value = properties.get("server.sv")) != null) {
            SERVER = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("server.name")) != null) {
            String name = String.valueOf(value);
            ServerManager.NAME = name.equals("BlackGoku") ? " BlackGoku 1" : name;
        }
        if ((value = properties.get("server.port")) != null) {
            ServerManager.PORT = Integer.parseInt(String.valueOf(value));
        }
        String linkServer = "";
        if ((value = properties.get("server.ip")) != null) {
            ServerManager.IP = String.valueOf(value);
            linkServer += ServerManager.NAME + ":" + ServerManager.IP + ":" + ServerManager.PORT + ":0,";
        }
        for (int i = 1; i <= 10; i++) {
            value = properties.get("server.sv" + i);
            if (value != null) {
                linkServer += String.valueOf(value) + ":0,";
            }
        }
        DataGame.LINK_IP_PORT = linkServer.substring(0, linkServer.length() - 1);
        if ((value = properties.get("server.waitlogin")) != null) {
            SECOND_WAIT_LOGIN = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("server.maxperip")) != null) {
            MAX_PER_IP = Integer.parseInt(String.valueOf(value));
        }
        if ((value = properties.get("server.maxplayer")) != null) {
            MAX_PLAYER = Integer.parseInt(String.valueOf(value));
        }
        if ((value = properties.get("server.expserver")) != null) {
            RATE_EXP_SERVER = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("server.local")) != null) {
            LOCAL = String.valueOf(value).toLowerCase().equals("true");
        }
        if ((value = properties.get("server.test")) != null) {
            TEST = String.valueOf(value).toLowerCase().equals("true");
        }
        if ((value = properties.get("server.daoautoupdater")) != null) {
            DAO_AUTO_UPDATER = String.valueOf(value).equalsIgnoreCase("true");
        }
    }

    /**
     * @param tileTypeFocus tile type: top, bot, left, right...
     * @return [tileMapId][tileType]
     */
    private int[][] readTileIndexTileType(int tileTypeFocus) {
        int[][] tileIndexTileType = null;
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream("data/map/tile_set_info"));
            int numTileMap = dis.readByte();
            tileIndexTileType = new int[numTileMap][];
            for (int i = 0; i < numTileMap; i++) {
                int numTileOfMap = dis.readByte();
                for (int j = 0; j < numTileOfMap; j++) {
                    int tileType = dis.readInt();
                    int numIndex = dis.readByte();
                    if (tileType == tileTypeFocus) {
                        tileIndexTileType[i] = new int[numIndex];
                    }
                    for (int k = 0; k < numIndex; k++) {
                        int typeIndex = dis.readByte();
                        if (tileType == tileTypeFocus) {
                            tileIndexTileType[i][k] = typeIndex;

                        }
                    }
                }
            }
        } catch (IOException e) {
            Logger.logException(MapService.class, e);
        }
        return tileIndexTileType;
    }

    /**
     * @param mapId mapId
     * @return tile map for paint
     */
    private int[][] readTileMap(int mapId) {
        int[][] tileMap = null;
        try {
            try (DataInputStream dis = new DataInputStream(new FileInputStream("data/map/tile_map_data/" + mapId))) {
                int w = dis.readByte();
                int h = dis.readByte();
                tileMap = new int[h][w];
                for (int[] tm : tileMap) {
                    for (int j = 0; j < tm.length; j++) {
                        tm[j] = dis.readByte();
                    }
                }
            }
        } catch (IOException e) {
        }
        return tileMap;
    }
    public static Clan getClanById(int id) throws Exception {
        for (Clan clan : CLANS) {
            if (clan.id == id) {
                return clan;
            }
        }
        throw new Exception("Không tìm thấy clan id: " + id);
    }

    public static void addClan(Clan clan) {
        CLANS.add(clan);
    }

    public static int getNumClan() {
        return CLANS.size();

    }

    public static MobTemplate getMobTemplateByTemp(int mobTempId) {
        for (MobTemplate mobTemp : MOB_TEMPLATES) {
            if (mobTemp.id == mobTempId) {
                return mobTemp;
            }
        }
        return null;
    }

    public static byte getNFrameImageByName(String name) {
        Object n = IMAGES_BY_NAME.get(name);
        if (n != null) {
            return Byte.parseByte(String.valueOf(n));
        } else {
            return 0;
        }
    }
}
