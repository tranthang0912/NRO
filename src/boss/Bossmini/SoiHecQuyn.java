package boss.Bossmini;
import boss.BossID;
import consts.BossStatus;
import boss.BossManager.BossManager;
import boss.*;
import consts.ConstTaskBadges;
import java.util.ArrayList;
import java.util.List;
import map.ItemMap;
import map.Zone;
import player.Player;
import services.Service;
import map.Service.ChangeMapService;
import task.BadgesTaskService;
import utils.Logger;
import utils.Util;

public class SoiHecQuyn extends Boss {

    private long lastTimeDrop;
    private long st;
    private int timeLeave;
    private boolean NguyenTanTai_KiemTraNhatXuong = false;
    private long NguyenTanTai_ThoiGianNhatXuong = 0;
    private long lastTimRestPawn;

    public SoiHecQuyn() throws Exception {
        super(BossID.SOI_HEC_QUYN1, BossesData.SOI_HEC_QUYN);
    }

    @Override
    public void joinMap() {
        if (zoneFinal != null) {
            joinMapByZone(zoneFinal);
            this.changeStatus(BossStatus.CHAT_S);
            this.wakeupAnotherBossWhenAppear();
            this.NguyenTanTai_ThoiGianNhatXuong = 0;
            this.NguyenTanTai_KiemTraNhatXuong = false;
            return;
        }

        if (this.zone == null) {
            if (this.parentBoss != null) {
                this.zone = parentBoss.zone;
            } else if (this.lastZone == null) {
                this.zone = getMapJoin();
            } else {
                this.zone = this.lastZone;
            }
        }

        if (this.zone != null) {
            try {
                List<Zone> availableZones = new ArrayList<>();

                for (Zone zone : this.zone.map.zones) {
                    if (zone.getNumOfPlayers() <= 10 && !BossManager.gI().checkBosses(zone, BossID.SOI_HEC_QUYN)) {
                        availableZones.add(zone);
                    }
                }

                if (!availableZones.isEmpty()) {
                    int randomIndex = Util.nextInt(availableZones.size());
                    this.zone = availableZones.get(randomIndex);
                    ChangeMapService.gI().changeMap(this, this.zone, Util.nextInt(100, 500), this.zone.map.yPhysicInTop(this.location.x,
                            this.location.y - 24));
                    this.changeStatus(BossStatus.CHAT_S);
                    st = System.currentTimeMillis();
                    timeLeave = Util.nextInt(100000, 300000);
                } else {
                    this.leaveMapNew();
                    return;
                }
            } catch (Exception e) {
                Logger.error(this.data[0].getName() + ": Lỗi đang tiến hành REST\n");
                this.changeStatus(BossStatus.REST);
            }
        } else {
            Logger.error(this.data[0].getName() + ": Lỗi map đang tiến hành RESPAWN\n");
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void chatM() {
        if (this.data[this.currentLevel].getTextM().length == 0) {
            return;
        }
        if (!Util.canDoWithTime(this.lastTimeChatM, this.timeChatM)) {
            return;
        }
        String textChat = this.data[this.currentLevel].getTextM()[Util.nextInt(0, this.data[this.currentLevel].getTextM().length - 1)];
        int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
        textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
        this.chat(prefix, textChat);
        this.lastTimeChatM = System.currentTimeMillis();
        this.timeChatM = Util.nextInt(3000, 20000);
    }

    @Override
    public void active() {
        this.attack();
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, timeLeave)) {
            this.leaveMapNew();
        }
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);

    }

    public void NhatXuong() {
        NguyenTanTai_KiemTraNhatXuong = true;
        NguyenTanTai_ThoiGianNhatXuong = System.currentTimeMillis();

    }

    public boolean NguyenTanTai_KiemTraNhatXuong() {
        return NguyenTanTai_KiemTraNhatXuong;
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100)) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.location == null) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(5, 20) && Util.getDistance(this, pl) > 50) {
                        if (Util.isTrue(5, 20)) {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                        } else {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)), pl.location.y);
                        }
                    } else if (Util.getDistance(this, pl) <= 50) {

                    }
                    checkPlayerDie(pl);
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
                if (NguyenTanTai_ThoiGianNhatXuong > 0) {
                    if (Util.canDoWithTime(NguyenTanTai_ThoiGianNhatXuong, 5000)) {
                        NguyenTanTai_ThoiGianNhatXuong = 0;
                        NguyenTanTai_KiemTraNhatXuong = false;
                    }
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        return 0;
    }

    @Override
    public void reward(Player plKill) {    
        BadgesTaskService.updateCountBagesTask(plKill, ConstTaskBadges.KE_THAO_TUNG_SOI, 1);
    }
}
