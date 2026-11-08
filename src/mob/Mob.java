package mob;

import player.Service.InventoryService;
import services.Service;
import services.TaskService;
import map.Service.ItemMapService;
import consts.ConstMap;
import consts.ConstMob;
import consts.ConstTask;
import item.Item;
import map.ItemMap;

import java.util.List;

import map.Zone;
import player.Location;
import player.Pet;
import player.Player;
import network.Message;
import java.io.IOException;
import server.Maintenance;
import server.Manager;
import utils.Util;

import java.util.ArrayList;
import services.AchievementService;
import Deputyhead.Service.TrainingService;
import server.ServerNotify;
import services.ChatGlobalService;
import services.ItemService;
import map.Service.MapService;
import skill.Skill;
import utils.TimeUtil;

public class Mob {

    public int id;
    public Zone zone;
    public int tempId;
    public String name;
    public byte level;

    public List<Player> temporaryEnemies = new ArrayList<>();

    public MobPoint point;
    public MobEffectSkill effectSkill;
    public Location location;

    public byte pDame;
    public int pTiemNang;
    private long maxTiemNang;

    public long lastTimeDie;
    public int lvMob = 0;
    public int status = 5;
    public int type = 1;

    private long lastTimeAttackPlayer;
    private long timeAttack = 2000;
    public long lastTimePhucHoi = System.currentTimeMillis();
    public long lastTimeSendEffect = System.currentTimeMillis();

    public Mob(Mob mob) {
        this.point = new MobPoint(this);
        this.effectSkill = new MobEffectSkill(this);
        this.location = new Location();
        this.id = mob.id;
        this.tempId = mob.tempId;
        this.level = mob.level;
        this.point.setHpFull(mob.point.getHpFull());
        this.point.sethp(this.point.getHpFull());
        this.location.x = mob.location.x;
        this.location.y = mob.location.y;
        this.pDame = mob.pDame;
        this.pTiemNang = mob.pTiemNang;
        this.type = mob.type;
        this.setTiemNang();
    }

    public Mob() {
        this.point = new MobPoint(this);
        this.effectSkill = new MobEffectSkill(this);
        this.location = new Location();
    }

    public void setTiemNang() {
        this.maxTiemNang = (long) this.point.getHpFull() * (long) (this.pTiemNang + Util.nextInt(-2, 2)) / 100L;
    }

    public boolean isDie() {
        return this.point.gethp() <= 0;
    }

    public void setDie() {
        this.lastTimePhucHoi = System.currentTimeMillis();
        this.lastTimeDie = System.currentTimeMillis();
    }

    public void addTemporaryEnemies(Player pl) {
        if (pl != null && !temporaryEnemies.contains(pl)) {
            temporaryEnemies.add(pl);
        }
    }

    public void injured(Player plAtt, long damage, boolean dieWhenHpFull) {
        if (!this.isDie()) {
            if (damage >= this.point.hp) {
                damage = this.point.hp;
            }
            if (!dieWhenHpFull) {
                if (this.point.hp == this.point.maxHp && damage >= this.point.hp) {
                    damage = this.point.hp - 1;
                }
                if ((this.tempId == ConstMob.MOC_NHAN || this.tempId == ConstMob.BU_NHIN_MA_QUAI)
                        && damage > this.point.maxHp / 10) {
                    damage = this.point.maxHp / 10;
                }
            }
            if (MapService.gI().isMapKhiGasHuyDiet(this.zone.map.mapId)) {
                boolean mob76Die = true;
                for (Mob mob : this.zone.mobs) {
                    if (!mob.isDie() && mob.tempId == ConstMob.CO_MAY_HUY_DIET) {
                        mob76Die = false;
                        break;
                    }
                }
                if (!mob76Die && plAtt != null && plAtt.playerSkill != null && plAtt.playerSkill.skillSelect != null) {
                    switch (plAtt.playerSkill.skillSelect.template.id) {
                        case Skill.LIEN_HOAN, Skill.ANTOMIC, Skill.MASENKO, Skill.KAMEJOKO ->
                            damage = 1;
                    }
                }
            }
            if (!dieWhenHpFull && !isBigBoss() && !MapService.gI().isMapPhoBan(this.zone.map.mapId) && this.lvMob > 0
                    && plAtt != null && plAtt.charms.tdOaiHung < System.currentTimeMillis()) {
                damage = (int) ((this.point.maxHp <= 20_000_000 ? this.point.maxHp * 1 : 2) * (10.0 / 100));
                this.mobAttackPlayer(plAtt);
            }
            if (plAtt != null && plAtt.isBoss && this.tempId > 0 && Util.isTrue(1, 2)
                    && Util.canDoWithTime(lastTimeAttackPlayer, 2500)) {
                this.mobAttackPlayer(plAtt);
                lastTimeAttackPlayer = System.currentTimeMillis();
            }

            if (damage > 2_000_000_000) {
                damage = 2_000_000_000;
            }

            this.point.hp -= damage;
            addTemporaryEnemies(plAtt);
            if (this.isDie()) {
                this.status = 0;
                this.setDie();
                this.temporaryEnemies.clear();
                if (plAtt != null) {
                    this.sendMobDieAffterAttacked(plAtt, (int) damage);
                    TaskService.gI().checkDoneTaskKillMob(plAtt, this);
                    TaskService.gI().checkDoneSideTaskKillMob(plAtt, this);
                    TaskService.gI().checkDoneClanTaskKillMob(plAtt, this);
                    AchievementService.gI().checkDoneTaskKillMob(plAtt, this);
                }
                if (this.id == 13) {
                    this.zone.isbulon1Alive = false;
                }
                if (this.id == 14) {
                    this.zone.isbulon2Alive = false;
                }
            } else {
                this.sendMobStillAliveAffterAttacked((int) damage,
                        plAtt != null ? (plAtt.nPoint != null && plAtt.nPoint.isCrit) : false);
            }
            if (plAtt != null) {
                if (plAtt.isPl() && plAtt.satellite != null && plAtt.satellite.isDefend) {
                    plAtt.satellite.isDefend = false;
                }
                Service.gI().addSMTN(plAtt, (byte) 2, getTiemNangForPlayer(plAtt, damage), true);
                TrainingService.gI().tangTnsmLuyenTap(plAtt, getTiemNangForPlayer(plAtt, damage));
            }
        }
    }

    public long getTiemNangForPlayer(Player pl, long dame) {
        int levelPlayer = Service.gI().getCurrLevel(pl);
        int levelMob = this.level;
        int checkLevel = Math.abs(levelPlayer - this.level);
        long tiemNang = (long) (dame + (point.getHpFull() * 0.0005));
        switch (this.tempId) {
            case 0:
                tiemNang = 1;
                break;
        }
        if (checkLevel > 5 && levelPlayer > levelMob) {
            tiemNang = 1;
        } else {
            if (checkLevel < 0) {
                checkLevel = Math.abs(levelMob - levelPlayer);
            } else {
                tiemNang /= (int) (checkLevel * 0.5) + 1.25;
            }
        }
        if (tiemNang < 1) {
            tiemNang = 1;
        }
        if (pl.nPoint != null) {
            tiemNang = (int) pl.nPoint.calSucManhTiemNang(tiemNang);
        } else {
            return 0;
        }
        if (pl.zone.map.mapId == 122 || pl.zone.map.mapId == 123 || pl.zone.map.mapId == 124) {
        }
        return tiemNang;
    }

    public void update() {
        if (zone.isGoldenFriezaAlive && TimeUtil.is21H()) {
            if (!isDie()) {
                startDie();
                return;
            }
        }
        if (!this.isDie() && this.tempId == ConstMob.CO_MAY_HUY_DIET && Util.canDoWithTime(lastTimeSendEffect, 1000)) {
            sendEffect(55);
            lastTimeSendEffect = System.currentTimeMillis();
        }

        if (this.isDie() && !Maintenance.isRunning && !isBigBoss()) {
            switch (zone.map.type) {
                case ConstMap.MAP_DOANH_TRAI:
                    if (this.tempId == ConstMob.BULON && this.zone.isTUTAlive
                            && Util.canDoWithTime(lastTimeDie, 10000)) {
                        this.hoiSinh();
                        this.hoiSinhMobPhoBan();
                        if (this.id == 13) {
                            this.zone.isbulon1Alive = true;
                        }
                        if (this.id == 14) {
                            this.zone.isbulon2Alive = true;
                        }
                    }
                    break;
                case ConstMap.MAP_BAN_DO_KHO_BAU:
                    break;
                case ConstMap.MAP_CON_DUONG_RAN_DOC:
                    break;
                case ConstMap.MAP_KHI_GAS_HUY_DIET:
                    break;
                case ConstMap.MAP_TAY_KARIN:
                    break;
                default:
                    if (this.zone.isGoldenFriezaAlive && TimeUtil.is21H()) {
                        return;
                    }
                    if (Util.canDoWithTime(lastTimeDie, 3000)) {
                        this.hoiSinh();
                        this.sendMobHoiSinh();
                    }
                    if (Util.canDoWithTime(lastTimePhucHoi, 30000) && !isDie()) {
                        lastTimePhucHoi = System.currentTimeMillis();
                        int hpMax = this.point.maxHp;
                        if (this.point.hp < hpMax) {
                            hoi_hp(hpMax / 10);
                        } else {
                            this.sendMobHoiSinh();
                        }
                    }
            }
        }

        effectSkill.update();
        attack();
    }

    public boolean isBigBoss() {
        return (this.tempId == ConstMob.HIRUDEGARN || this.tempId == ConstMob.VUA_BACH_TUOC
                || this.tempId == ConstMob.ROBOT_BAO_VE || this.tempId == ConstMob.GAU_TUONG_CUOP
                || this.tempId == ConstMob.VOI_CHIN_NGA || this.tempId == ConstMob.GA_CHIN_CUA
                || this.tempId == ConstMob.NGUA_CHIN_LMAO || this.tempId == ConstMob.PIANO);
    }

    public void attack() {
        Player player = getPlayerCanAttack();
        if (!isDie() && !effectSkill.isHaveEffectSkill() && tempId != ConstMob.MOC_NHAN
                && tempId != ConstMob.BU_NHIN_MA_QUAI && tempId != ConstMob.CO_MAY_HUY_DIET && !this.isBigBoss()
                && (this.lvMob < 1 || MapService.gI().isMapPhoBan(this.zone.map.mapId))
                && Util.canDoWithTime(lastTimeAttackPlayer, timeAttack)) {
            if (player != null) {
                this.mobAttackPlayer(player);
            }
            this.lastTimeAttackPlayer = System.currentTimeMillis();
        }
    }

    public Player getPlayerCanAttack() {
        Player plAttack = getFirstPlayerCanAttack();
        if (plAttack != null) {
            return plAttack;
        }
        int distance = 100;
        try {
            List<Player> players = this.zone.getNotBosses();
            for (Player pl : players) {
                if (!pl.isDie() && !pl.isBoss && !pl.isNewPet && (pl.satellite == null || !pl.satellite.isDefend)
                        && (pl.effectSkin == null || !pl.effectSkin.isVoHinh)
                        && (this.tempId > 18 || (this.tempId > 9 && this.type == 4)) || isBigBoss()) {
                    int dis = Util.getDistance(pl, this);
                    if (dis <= distance || isBigBoss()) {
                        plAttack = pl;
                        distance = dis;
                    }
                }
            }
            this.timeAttack = 2000;
        } catch (Exception e) {

        }
        return plAttack;
    }

    private Player getFirstPlayerCanAttack() {
        Player plAtt = null;
        try {
            List<Player> playersMap = zone.getHumanoids();
            int dis = 300;
            if (playersMap != null) {
                for (Player plAttt : playersMap) {
                    if (plAttt.isDie() || plAttt.isBoss || (plAttt.satellite != null && plAttt.satellite.isDefend)
                            || (plAttt.effectSkin != null && plAttt.effectSkin.isVoHinh)
                            || !this.temporaryEnemies.contains(plAttt)) {
                        continue;
                    }
                    int d = Util.getDistance(plAttt, this);
                    if (d <= dis) {
                        dis = d;
                        plAtt = plAttt;
                    }
                }
            }
            this.timeAttack = 1000;
        } catch (Exception e) {

        }
        return plAtt;
    }

    private void mobAttackPlayer(Player player) {
        int dameMob = this.point.getDameAttack();
        if (player.charms != null && player.charms.tdDaTrau > System.currentTimeMillis()) {
            dameMob /= 2;
        }
        if (player.isPet && ((Pet) player).master.charms != null
                && ((Pet) player).master.charms.tdDeTu > System.currentTimeMillis()) {
            dameMob /= 2;
        }
        if (this.lvMob > 0 && !MapService.gI().isMapPhoBan(this.zone.map.mapId)) {
            dameMob = (int) (player.nPoint.hpMax * (10.0 / 100));
        }
        if (player.satellite != null && player.satellite.isDefend) {
            dameMob -= dameMob / 5;
        }
        if (player.itemTime != null && player.itemTime.isUseCMS) {
            dameMob = (int) Math.round(dameMob * 0.1);
        }
        if (this.lvMob > 0 && player.charms.tdOaiHung > System.currentTimeMillis()) {
            dameMob = dameMob;
        }
        int dame = player.injured(null, dameMob, false, true);

        this.sendMobAttackMe(player, dame);
        this.sendMobAttackPlayer(player);
        this.phanSatThuong(player, dame);
    }

    private void sendMobAttackMe(Player player, int dame) {
        if (!player.isPet && !player.isNewPet) {
            Message msg;
            try {
                msg = new Message(-11);
                msg.writer().writeByte(this.id);
                msg.writer().writeInt(dame); // dame
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
            }
        }
    }

    private void sendMobAttackPlayer(Player player) {
        Message msg;
        try {
            msg = new Message(-10);
            msg.writer().writeByte(this.id);
            msg.writer().writeInt((int) player.id);
            msg.writer().writeInt(player.nPoint.hp);
            Service.gI().sendMessAnotherNotMeInMap(player, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void hoiSinh() {
        this.status = 5;
        this.point.hp = this.point.maxHp;
        this.setTiemNang();
    }

    public int lvMob() {
        for (Mob mobMap : this.zone.mobs) {
            if (mobMap.lvMob > 0) {
                return 0;
            }
        }
        this.lvMob = this.tempId > 12 && this.tempId < 34 && !isBigBoss() ? Util.isTrue(0, 10000) ? 1 : 0 : 0;
        this.point.hp = this.lvMob > 0 ? this.point.maxHp <= 20000000 ? this.point.maxHp * 10 : 2000000000
                : this.point.maxHp;
        return this.lvMob;
    }

    public void sendMobHoiSinh() {
        Message msg = null;
        try {
            msg = new Message(-13);
            msg.writer().writeByte(this.id);
            msg.writer().writeByte(this.tempId);
            msg.writer().writeByte(lvMob());
            msg.writer().writeInt(this.point.hp);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            this.sendMobMaxHp(this.point.hp);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void hoi_hp(int hp) {
        Message msg = null;
        try {
            this.point.sethp(this.point.gethp() + hp);
            int HP = hp > 0 ? 1 : Math.abs(hp);
            msg = new Message(-9);
            msg.writer().writeByte(this.id);
            msg.writer().writeInt(this.point.gethp());
            msg.writer().writeInt(HP);
            msg.writer().writeBoolean(false);
            msg.writer().writeByte(-1);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
                msg = null;
            }
        }
    }

    public void sendEffect(int Effect) {
        Message msg = null;
        try {
            msg = new Message(-9);
            msg.writer().writeByte(this.id);
            msg.writer().writeInt(this.point.gethp());
            msg.writer().writeInt(this.point.gethp());
            msg.writer().writeBoolean(false);
            msg.writer().writeByte(Effect);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
                msg = null;
            }
        }
    }

    private void sendMobDieAffterAttacked(Player plKill, int dameHit) {
        Message msg;
        try {
            msg = new Message(-12);
            msg.writer().writeByte(this.id);
            msg.writer().writeInt(dameHit);
            msg.writer().writeBoolean(plKill.nPoint.isCrit); // crit
            List<ItemMap> items = mobReward(plKill, this.dropItemTask(plKill), msg);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
            hutItem(plKill, items);
        } catch (Exception e) {
        }
    }

    private void hutItem(Player player, List<ItemMap> items) {
        if (!player.isPet && !player.isNewPet) {
            if (player.charms.tdThuHut > System.currentTimeMillis()) {
                for (ItemMap item : items) {
                    ItemMapService.gI().pickItem(player, item.itemMapId, true);
                }
            }
        } else {
            if (((Pet) player).master.charms.tdThuHut > System.currentTimeMillis()) {
                for (ItemMap item : items) {
                    ItemMapService.gI().pickItem(((Pet) player).master, item.itemMapId, true);
                }
            }
        }
    }

    private List<ItemMap> mobReward(Player player, ItemMap itemTask, Message msg) {
        List<ItemMap> itemReward = new ArrayList<>();
        try {
            itemReward = this.getItemMobReward(player, this.location.x + Util.nextInt(-10, 10),
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y));
            if (itemTask != null) {
                itemReward.add(itemTask);
            }
            msg.writer().writeByte(itemReward.size()); // sl item roi
            for (ItemMap itemMap : itemReward) {
                msg.writer().writeShort(itemMap.itemMapId);// itemmapid
                msg.writer().writeShort(itemMap.itemTemplate.id); // id item
                msg.writer().writeShort(itemMap.x); // xend item
                msg.writer().writeShort(itemMap.y); // yend item
                msg.writer().writeInt((int) itemMap.playerId); // id nhan vat
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return itemReward;
    }

    public List<ItemMap> getItemMobReward(Player player, int x, int yEnd) {
        List<ItemMap> list = new ArrayList<>();
        if (player.isBoss) {
            return list;
        }

        if (this.tempId == 0) {
            return list;
        }
        int mapid = player.zone.map.mapId;
        if (player.itemTime.isUseMayDo
                && (Util.isTrue(10, 100))
                && this.tempId > 57 && this.tempId < 66) {
            list.add(new ItemMap(zone, 380, 1, x, yEnd, player.id));
        }

        if (player.isPl()
                && TaskService.gI().getIdTask(player) == ConstTask.TASK_8_1) {

            if (player.isPl() && TaskService.gI().getIdTask(player) == ConstTask.TASK_8_1) {
                if (player.gender == 0 && this.tempId == 11 || player.gender == 1 && this.tempId == 12
                        || player.gender == 2 && this.tempId == 10) {
                    list.add(new ItemMap(zone, 444, 1, x, yEnd, player.id));
                    TaskService.gI().checkDoneTaskFind7Stars(player);
                }
            }
        }

        if (MapService.gI().isMapUpPorata(mapid)) {
            if (Util.isTrue(10, 100)) {
                ItemMap it = new ItemMap(zone, 933, 1, x, yEnd, player.id);
                it.options.add(new Item.ItemOption(31, 1));
                list.add(it);
            } else if (Util.isTrue(5, 100) && player.itemEvent.canDropManhVo(150)) {
                ItemMap it = new ItemMap(zone, 934, 1, x, yEnd, player.id);
                it.options.add(new Item.ItemOption(31, 1));
                list.add(it);
            } else if (Util.isTrue(1, 500)) {
                ItemMap it = new ItemMap(zone, 935, 1, x, yEnd, player.id);
                it.options.add(new Item.ItemOption(31, 1));
                list.add(it);
            }
        }
        // =======================
        // RƠI NGẪU NHIÊN 220–224 (1/100)
        // =======================
        if (Util.isTrue(1, 200)) { // 1%
            int[] items = { 220, 221, 222, 223, 224 };
            int itemId = items[Util.nextInt(items.length)];

            ItemMap it = new ItemMap(
                    zone,
                    itemId,
                    1,
                    x,
                    yEnd,
                    player.id);

            list.add(it);
        }

        if (MapService.gI().isMap3Planets(mapid)) {
            if (Util.isTrue(1, 10)) {
                int vang = Util.nextInt(500, 1000);
                if (vang < 300) {
                    list.add(new ItemMap(zone, 76, vang, x, yEnd, player.id));
                } else if (vang < 600) {
                    list.add(new ItemMap(zone, 188, vang, x, yEnd, player.id));
                } else {
                    list.add(new ItemMap(zone, 189, vang, x, yEnd, player.id));
                }
            }
        }
        if (MapService.gI().isMapNappa(mapid)) {
            if (Util.isTrue(1, 10)) {
                int vang = Util.nextInt(1000, 2000);
                if (vang < 1300) {
                    list.add(new ItemMap(zone, 188, vang, x, yEnd, player.id));
                } else if (vang < 1600) {
                    list.add(new ItemMap(zone, 189, vang, x, yEnd, player.id));
                } else {
                    list.add(new ItemMap(zone, 190, vang, x, yEnd, player.id));
                }
            }
        }
        if (MapService.gI().isMapCold(mapid)) {
            if (Util.isTrue(1, 10)) {
                int vang = Util.nextInt(4000, 8000);
                if (vang < 5000) {
                    list.add(new ItemMap(zone, 189, vang, x, yEnd, player.id)); // Rơi vàng cấp 189
                } else if (vang < 6000) {
                    list.add(new ItemMap(zone, 190, vang, x, yEnd, player.id)); // Rơi vàng cấp 190
                } else {
                    list.add(new ItemMap(zone, 190, vang, x, yEnd, player.id)); // Rơi vàng cấp 190
                }
            }
        }
        if (MapService.gI().isMapTuongLai(mapid)) {
            if (Util.isTrue(1, 10)) {
                int vang = Util.nextInt(2000, 4000);
                if (vang < 2500) {
                    list.add(new ItemMap(zone, 188, vang, x, yEnd, player.id));
                } else if (vang < 3000) {
                    list.add(new ItemMap(zone, 189, vang, x, yEnd, player.id));
                } else {
                    list.add(new ItemMap(zone, 190, vang, x, yEnd, player.id));
                }
            }
        }
        if (MapService.gI().isMapPhoBan(mapid)) {
            if (Util.isTrue(1, 100)) {
                int vang = Util.nextInt(8000, 20000);
                if (vang < 6000) {
                    list.add(new ItemMap(zone, 188, vang, x, yEnd, player.id));
                } else if (vang < 10000) {
                    list.add(new ItemMap(zone, 189, vang, x, yEnd, player.id));
                } else {
                    list.add(new ItemMap(zone, 190, vang, x, yEnd, player.id));
                }
            }
        }
        if (Util.isTrue(1, 1000000)) {
            int ngoc = Util.nextInt(1, 1);
            list.add(new ItemMap(zone, 77, ngoc, x, yEnd, player.id));
        }
        // if (((Util.isTrue(50, 5000)))
        // && MapService.gI().isMapUpSKH(mapid)) {
        // short itTemp = (short) ItemService.gI().randTempItemKichHoat(player.gender);
        // ItemMap it = new ItemMap(zone, itTemp, 1, x, yEnd, player.id);
        // List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(itTemp);
        // if (!ops.isEmpty()) {
        // it.options = ops;
        // }
        // int[] opsrand = ItemService.gI().randOptionItemKichHoatNew(player.gender);
        // it.options.add(new Item.ItemOption(opsrand[0], 0));
        // it.options.add(new Item.ItemOption(opsrand[1], 0));
        // it.options.add(new Item.ItemOption(opsrand[2], 0));
        // it.options.add(new Item.ItemOption(opsrand[3], 0));
        // it.options.add(new Item.ItemOption(30, 0));
        // list.add(it);
        // ChatGlobalService.gI().ThongBaoRoiDo(player, "[Hệ Thống] " + player.name + "
        // vừa nhặt được " + it.itemTemplate.name + " sét kích hoạt" + " tại " +
        // this.zone.map.mapName + " khu " + this.zone.zoneId);
        // }
        // if (Util.isTrue(1, 10000) && MapService.gI().isMapUpSKH(mapid)) {

        // short itTemp = (short) ItemService.gI().randTempItemDoSao(player.gender);
        // ItemMap it = new ItemMap(zone, itTemp, 1, x, yEnd, player.id);

        // // Option gốc của đồ
        // List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(itTemp);
        // if (!ops.isEmpty()) {
        // it.options = ops;
        // }

        // // ⭐ RANDOM SAO (BẮT BUỘC)
        // int randAddOption = Util.nextInt(100);
        // int star;

        // if (randAddOption < 50) {
        // star = 1;
        // } else if (randAddOption < 99) {
        // star = 2;
        // } else {
        // star = 3;
        // }

        // it.options.add(new Item.ItemOption(107, star));

        // // Thêm item vào danh sách rơi
        // list.add(it);

        // // Thông báo
        // ChatGlobalService.gI().ThongBaoRoiDo(
        // player,
        // "[Hệ Thống] " + player.name
        // + " vừa nhặt được "
        // + it.itemTemplate.name
        // + " +" + star + " sao"
        // );
        // }

        // if (((Util.isTrue(50, 50000)))
        // && MapService.gI().isMapUpSKH(mapid)) {
        // int baseRate = 50;
        // int powerReduction = (int) Math.min(player.nPoint.power / 100000, 5) * 20;
        // int finalRate = Math.max(baseRate - powerReduction, 0);
        // if (finalRate > 0 && Util.nextInt(100) < finalRate) {
        // short itTemp = (short) ItemService.gI().randDoSao(player.gender);
        // ItemMap it = new ItemMap(zone, itTemp, 1, x, yEnd, player.id);
        // List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(itTemp);
        // if (!ops.isEmpty()) {
        // it.options = ops;
        // }
        // int randOption = Util.nextInt(1,100);
        // boolean hasOption = false;
        // if (randOption < 50) {
        // int randAddOption = Util.nextInt(100);
        // if (randAddOption < 60) {
        // it.options.add(new Item.ItemOption(107, 1));
        // hasOption = true;
        // } else if (randAddOption < 90) {
        // it.options.add(new Item.ItemOption(107, 2));
        // hasOption = true;
        // } else {
        // it.options.add(new Item.ItemOption(107, 3));
        // hasOption = true;
        // }
        // }
        // if (hasOption) {
        // list.add(it);
        // ChatGlobalService.gI().ThongBaoRoiDo(player, "[ Hệ Thống ] " + player.name +
        // " vừa nhặt được " + it.itemTemplate.name + " tại " + this.zone.map.mapName +
        // " khu " + this.zone.zoneId);
        // }
        // }
        // }

        // if (MapService.gI().isMapCold(mapid)) {
        // if (player.isPet) {
        // player = ((Pet) player).master;
        // }
        // if (Util.isTrue(1, 50000)) {
        // ItemMap it = ItemService.gI().randDoTL(this.zone, 1, x, yEnd, player.id);
        // list.add(it);
        // ChatGlobalService.gI().ThongBaoRoiDo(player, "[ Hệ Thống ] " + player.name +
        // " vừa nhặt được " + it.itemTemplate.name + " tại " + this.zone.map.mapName +
        // " khu " + this.zone.zoneId);
        // }
        // if (Util.isTrue(10, 100)) {
        // if (player.setClothes.checkSetGod()) {
        // ItemMap it = new ItemMap(zone, Util.nextInt(663, 667), 1, x, yEnd,
        // player.id);
        // list.add(it);
        // }
        // }
        // }
        if (MapService.gI().isMapCold(mapid)) {
            if (Util.isTrue(1, 100)) {
                int rand = Util.nextInt(0, 4);
                ItemMap it = new ItemMap(zone, 220 + rand, 1, x, yEnd, player.id);
                it.options.add(new Item.ItemOption(71 - rand, 0));
                list.add(it);
            }
        }
        if (MapService.gI().isMapDoanhTrai(mapid)
                && (Util.isTrue(10, 100))) {
            ItemMap it = new ItemMap(zone, 225, 1, x, yEnd, player.id);
            it.options.add(new Item.ItemOption(74, 0));
            list.add(it);
        }
        // if (MapService.gI().isMap3Planets(mapid)
        // && (Util.isTrue(1, 5))) {
        // ItemMap it = new ItemMap(zone, 225, 1, x, yEnd, player.id);
        // it.options.add(new Item.ItemOption(74, 0));
        // list.add(it);
        // }
        // if (MapService.gI().isMap3Planets(mapid)
        // || MapService.gI().isMapNappa(mapid)
        // || MapService.gI().isMapTuongLai(mapid)
        // || MapService.gI().isMapCold(mapid)) {
        // if (Util.isTrue(10, 70)
        // || (player.isActive() && Util.isTrue(1, 100))) {
        // int rand = Util.nextInt(0, 1);
        // ItemMap it = new ItemMap(zone, 19 + rand, 1, x, yEnd, player.id);
        // list.add(it);
        // }
        // }
        // if ((Util.isTrue(10, 100) || (player.isActive() &&
        // player.setClothes.checkSetDes() && Util.isTrue(20, 100))) &&
        // MapService.gI().isMapNgucTu(mapid)) {
        // list.add(new ItemMap(zone, Util.nextInt(1066, 1070), 1, x, yEnd, player.id));
        // }
        // if ((Util.isTrue(10, 100) || (player.isActive() &&
        // player.setClothes.checkSetDes() && Util.isTrue(20, 100))) &&
        // MapService.gI().isMapNgucTu(mapid)) {
        // list.add(new ItemMap(zone, 1229, 1, x, yEnd, player.id));
        // }
        // if ((Util.isTrue(10, 100) && MapService.gI().isMapNguHanhSon(mapid))) {
        // list.add(new ItemMap(zone, Util.nextInt(541,542), 1, x, yEnd, player.id));
        // }
        // if (this.zone.map.mapId >= 0) {
        // if (Util.isTrue(1, 300)) { // nro
        // list.add(new ItemMap(zone, Util.nextInt(19, 20), 1, x, this.location.y,
        // player.id));
        // }
        // }
        int mapId = this.zone.map.mapId;
        int rate = 0; // tính theo /1000

        if (MapService.gI().isMap3Planets(mapId)) {
            rate = 10; // 0.1%
        } else if (MapService.gI().isMapNappa(mapId)) {
            rate = 15; // 0.3%
        } else if (MapService.gI().isMapTuongLai(mapId)) {
            rate = 20; // 0.5%
        } else if (MapService.gI().isMapCold(mapId)) {
            rate = 25; // 1%
        }

        if (rate > 0 && Util.nextInt(1000) < rate) {
            list.add(new ItemMap(
                    Util.spl(zone,
                            Util.nextInt(441, 443), // id vật phẩm
                            1,
                            x,
                            this.location.y,
                            player.id)));
        }

        // MAP TƯƠNG LAI & MAP COLD
        if (MapService.gI().isMapTuongLai(mapid) || MapService.gI().isMapCold(mapid)) {

            // Tỉ lệ rơi
            boolean drop = MapService.gI().isMapTuongLai(mapid)
                    ? Util.isTrue(1, 14000)
                    : Util.isTrue(1, 18000);

            if (drop) {

                int[] items = {
                        233, 241, 237,
                        245, 253, 249,
                        265, 261, 257,
                        277, 273, 269,
                        281
                };

                int itemId = items[Util.nextInt(items.length)];
                ItemMap it = new ItemMap(zone, itemId, 1, x, yEnd, player.id);

                // ĐẢM BẢO OPTIONS KHÔNG NULL
                if (it.options == null) {
                    it.options = new ArrayList<>();
                }

                // OPTION CHÍNH
                if (itemId == 233 || itemId == 241 || itemId == 237) {
                    it.options.add(new Item.ItemOption(47, Util.nextInt(500, 701)));
                } else if (itemId == 265 || itemId == 261 || itemId == 257) {
                    it.options.add(new Item.ItemOption(0, Util.nextInt(2100, 2501)));
                } else if (itemId == 245 || itemId == 253 || itemId == 249) {
                    it.options.add(new Item.ItemOption(6, Util.nextInt(23000, 26001)));
                } else if (itemId == 277 || itemId == 273 || itemId == 269) {
                    it.options.add(new Item.ItemOption(7, Util.nextInt(25000, 28001)));
                } else if (itemId == 281) {
                    it.options.add(new Item.ItemOption(14, Util.nextInt(11, 14)));
                }

                // OPTION PHỤ (LUÔN CÓ 1 TRONG 3)
                int[] extraOps = { 50, 77, 103 };
                int opId = extraOps[Util.nextInt(extraOps.length)];
                it.options.add(new Item.ItemOption(opId, Util.nextInt(1, 5)));

                // RANDOM SAO
                int randAddOption = Util.nextInt(100);
                int star;

                if (randAddOption < 50) {
                    star = 1;
                } else if (randAddOption < 99) {
                    star = 2;
                } else {
                    star = 3;
                }

                // GẮN SAO (OPTION 107)
                it.options.add(new Item.ItemOption(107, star));

                // THÊM ITEM
                list.add(it);

                // CHAT GLOBAL ĐỒ SAO
                ChatGlobalService.gI().ThongBaoRoiDo(
                        player,
                        "[Hệ Thống] " + player.name
                                + " vừa nhặt được "
                                + it.itemTemplate.name
                                + " +" + star + " sao");
            }
        }

        // MAP NAPPA

        // Rơi item 17 - 2%
        if (Util.isTrue(1, 400)) {
            list.add(new ItemMap(
                    zone,
                    18, // id item
                    1,
                    x,
                    yEnd,
                    player.id));
        }

        // Rơi item 19 hoặc 20 - 0.5%
        if (Util.isTrue(1, 250)) { // 0.5%
            int rand = Util.nextInt(0, 1); // 0 hoặc 1
            list.add(new ItemMap(
                    zone,
                    19 + rand, // 19 hoặc 20
                    1,
                    x,
                    yEnd,
                    player.id));

        }

        return list;
    }

    private ItemMap dropItemTask(Player player) {
        ItemMap itemMap = null;
        switch (tempId) {
            case ConstMob.KHUNG_LONG:
            case ConstMob.LON_LOI:
            case ConstMob.QUY_DAT:
                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_2_0) {
                    itemMap = new ItemMap(zone, 73, 1, location.x, location.y, player.id);
                }
                break;
            case ConstMob.THAN_LAN_ME:
            case ConstMob.QUY_BAY_ME:
            case ConstMob.PHI_LONG_ME:
                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_8_1) {
                    if (Util.isTrue(1, 10)) {
                        itemMap = new ItemMap(zone, 444, 1, location.x, location.y, player.id);
                    } else {
                        Service.gI().sendThongBao(player,
                                "Con thằn lằn mẹ này không giữ ngọc, hãy tìm con thằn lằn mẹ khác");
                    }
                }
        }
        if (itemMap != null) {
            return itemMap;
        }
        return null;
    }

    private void sendMobStillAliveAffterAttacked(int dameHit, boolean crit) {
        Message msg;
        try {
            msg = new Message(-9);
            msg.writer().writeByte(this.id);
            msg.writer().writeInt(this.point.gethp());
            msg.writer().writeInt(dameHit);
            msg.writer().writeBoolean(crit); // chí mạng
            msg.writer().writeInt(-1);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void hoiSinhMobPhoBan() {
        this.point.hp = this.point.maxHp;
        this.setTiemNang();
        Message msg;
        try {
            msg = new Message(-13);
            msg.writer().writeByte(this.id);
            msg.writer().writeByte(this.tempId);
            msg.writer().writeByte(this.lvMob); // level mob
            msg.writer().writeInt(this.point.hp);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void hoiSinhMobTayKarin() {
        this.point.hp = this.point.maxHp;
        this.maxTiemNang = 1;
        Message msg;
        try {
            msg = new Message(-13);
            msg.writer().writeByte(this.id);
            msg.writer().writeByte(this.tempId);
            msg.writer().writeByte(this.lvMob); // level mob
            msg.writer().writeInt(this.point.hp);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendSieuQuai(int type) {
        Message msg;
        try {
            msg = new Message(-75);
            msg.writer().writeByte(this.id);
            msg.writer().writeByte(type);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendDisable(boolean bool) {
        Message msg;
        try {
            msg = new Message(81);
            msg.writer().writeByte(this.id);
            msg.writer().writeBoolean(bool);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendDoneMove(boolean bool) {
        Message msg;
        try {
            msg = new Message(82);
            msg.writer().writeByte(this.id);
            msg.writer().writeBoolean(bool);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendFire(boolean bool) {
        Message msg;
        try {
            msg = new Message(85);
            msg.writer().writeByte(this.id);
            msg.writer().writeBoolean(bool);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendIce(boolean bool) {
        Message msg;
        try {
            msg = new Message(86);
            msg.writer().writeByte(this.id);
            msg.writer().writeBoolean(bool);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendWind(boolean bool) {
        Message msg;
        try {
            msg = new Message(87);
            msg.writer().writeByte(this.id);
            msg.writer().writeBoolean(bool);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendMobMaxHp(int maxHp) {
        Message msg;
        try {
            msg = new Message(87);
            msg.writer().writeByte(this.id);
            msg.writer().writeInt(maxHp);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    private void phanSatThuong(Player plTarget, long dame) {
        if (plTarget.nPoint == null) {
            return;
        }
        int percentPST = plTarget.nPoint.tlPST;
        if (percentPST != 0) {
            int damePST = (int) (long) (dame * percentPST / 100L);
            Message msg;
            try {
                msg = new Message(-9);
                msg.writer().writeByte(this.id);
                if (damePST >= this.point.hp) {
                    damePST = this.point.hp - 1;
                }
                int hpMob = this.point.hp;
                injured(null, damePST, true);
                damePST = hpMob - this.point.hp;
                msg.writer().writeInt(this.point.hp);
                msg.writer().writeInt(damePST);
                msg.writer().writeBoolean(false);
                msg.writer().writeByte(36);
                Service.gI().sendMessAllPlayerInMap(this.zone, msg);
                msg.cleanup();
            } catch (IOException e) {
            }
        }
    }

    public void startDie() {
        Message msg;
        try {
            setDie();
            this.point.hp = -1;
            this.status = 0;
            msg = new Message(-12);
            msg.writer().writeByte(this.id);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }
}
