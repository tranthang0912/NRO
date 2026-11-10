package web;

import consts.ConstPlayer;
import map.Zone;
import mob.Mob;
import player.Player;
import services.EffectSkillService;
import services.SkillService;
import skill.Skill;
import utils.SkillUtil;
import utils.Util;

/** Skill choices for quest combat on the world thread. */
final class WebQuestCombat {
    private Skill preparing;
    private Object preparationTarget;
    private Zone preparationZone;
    private boolean startedCharge;

    Skill select(Player player, Player boss, Mob mob, Skill basic, double distance) {
        if (boss != null && !boss.isBoss || boss == null && mob == null) {
            reset(player);
            return basic;
        }
        Object target = boss != null ? boss : mob;
        if (preparing != null) {
            if (target != preparationTarget || player.zone != preparationZone || !isPrepared(player)
                    || player.nPoint.hp <= player.nPoint.hpMax / 2
                    || distance > range(preparing) || !ready(player, preparing)
                    || preparing.template.id == Skill.QUA_CAU_KENH_KHI && !alone(player)) {
                reset(player);
            } else {
                boolean laser = preparing.template.id == Skill.MAKANKOSAPPO;
                long started = laser ? player.playerSkill.lastTimePrepareLaze : player.playerSkill.lastTimePrepareQCKK;
                return Util.canDoWithTime(started, laser ? 3_000 : 4_000) ? preparing : null;
            }
        }
        Skill selected = player.playerSkill.skillSelect;
        if (player.effectSkill.isCharging && selected != null && selected.template.id == Skill.TAI_TAO_NANG_LUONG) {
            if (safeToCharge(player, distance)) return null;
            EffectSkillService.gI().stopCharge(player);
            startedCharge = false;
            return basic;
        }
        startedCharge = false;

        long hp = boss != null ? boss.nPoint.hp : mob.point.hp;
        long basicHit = estimateHit(player, boss, basic);
        boolean basicInRange = distance <= range(basic);
        Skill aid = recovery(player, boss, distance);
        if (aid != null) return aid;
        if (basicInRange && basicHit >= hp) return basic;

        boolean basicReady = basicInRange && Util.canDoWithTime(basic.lastTimeUseThisSkill, basic.coolDown - 50);
        Skill best = basic;
        long bestHit = basicReady ? basicHit : 0;
        long bestCost = basicReady ? kiCost(player, basic) : Long.MAX_VALUE;
        for (Skill skill : player.playerSkill.skills) {
            if (skill == basic || !learned(skill)) continue;
            int id = skill.template.id;
            if (id != Skill.KAMEJOKO && id != Skill.MASENKO && id != Skill.ANTOMIC && id != Skill.LIEN_HOAN
                    && id != Skill.KAIOKEN) continue;
            if (distance > range(skill) || !ready(player, skill)) continue;
            long hit = estimateHit(player, boss, skill);
            if (id == Skill.KAIOKEN && (player.nPoint.hp < (long) player.nPoint.hpMax * 4 / 5
                    || hp <= basicHit * 2 || hit < basicHit * 5 / 4)) continue;
            long cost = kiCost(player, skill);
            if (bestHit >= hp ? hit >= hp && cost < bestCost : hit > bestHit || hit == bestHit && cost < bestCost) {
                best = skill;
                bestHit = hit;
                bestCost = cost;
            }
        }
        if (bestHit >= hp || hp <= basicHit * 2) return best;

        Skill buff = sustainedFight(player, boss, mob, distance);
        if (buff != null) return buff;
        Skill burst = preparedAttack(player, boss, hp, basicHit, distance);
        if (burst != null) {
            preparing = burst;
            preparationTarget = target;
            preparationZone = player.zone;
            return burst;
        }
        return best;
    }

    void reset(Player player) {
        if (preparing != null && player != null) {
            if (preparing.template.id == Skill.MAKANKOSAPPO) player.playerSkill.prepareLaze = false;
            else player.playerSkill.prepareQCKK = false;
        }
        if (startedCharge && player != null && player.effectSkill.isCharging) EffectSkillService.gI().stopCharge(player);
        startedCharge = false;
        preparing = null;
        preparationTarget = null;
        preparationZone = null;
    }

    static boolean selfHeal(Skill skill) {
        return skill.template.id == Skill.TRI_THUONG;
    }

    static int range(Skill skill) {
        int webRange = Math.max(110, skill.dx + 40);
        return switch (skill.template.id) {
            case Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN ->
                    Math.min(webRange, Skill.RANGE_ATTACK_CHIEU_DAM);
            default -> webRange;
        };
    }

    private Skill recovery(Player player, Player boss, double distance) {
        boolean lowHp = player.nPoint.hp <= player.nPoint.hpMax / 2;
        boolean lowKi = player.nPoint.mp <= player.nPoint.mpMax / 4;
        if (lowHp) {
            Skill heal = usable(player, Skill.TRI_THUONG, 0);
            if (heal != null && canHealSelf(player, heal)) return heal;
            if (boss != null && distance <= 220 && !player.effectSkill.isShielding && !boss.effectSkill.isHaveEffectSkill()) {
                Skill shield = usable(player, Skill.KHIEN_NANG_LUONG, 0);
                if (shield != null) return shield;
            }
        }
        if (lowKi && player.nPoint.mpMax >= 100 || lowHp && player.nPoint.hpMax >= 100) {
            Skill charge = usable(player, Skill.TAI_TAO_NANG_LUONG, 0);
            if (charge != null && safeToCharge(player, distance)) {
                startedCharge = true;
                return charge;
            }
        }
        return null;
    }

    private Skill sustainedFight(Player player, Player boss, Mob mob, double distance) {
        if (distance > 250) return null;
        if (!player.effectSkill.isMonkey) {
            Skill monkey = usable(player, Skill.BIEN_KHI, 0);
            if (monkey != null) return monkey;
        }
        if (player.mobMe == null) {
            Skill summon = usable(player, Skill.DE_TRUNG, 0);
            if (summon != null) return summon;
        }
        if (boss != null && player.effectSkill.tiLeHPHuytSao == 0) {
            Skill whistle = usable(player, Skill.HUYT_SAO, 0);
            if (whistle != null && player.zone.getHumanoids().stream().allMatch(other -> other == player
                    || other.isBoss && !other.effectSkill.useTroi)) return whistle;
        }
        boolean controlled = boss != null ? boss.effectSkill.isHaveEffectSkill() || boss.effectSkill.isSocola
                : mob.effectSkill.isHaveEffectSkill() || mob.effectSkill.isSocola || mob.effectSkill.isBinh;
        if (controlled || boss == null && player.nPoint.hp > (long) player.nPoint.hpMax * 3 / 5) return null;

        Skill stun = usable(player, Skill.THAI_DUONG_HA_SAN, 0);
        if (stun != null && distance <= SkillUtil.getRangeStun(stun.point) && safeStun(player, stun)) return stun;
        if (boss != null && !player.effectSkill.useTroi
                && !boss.playerSkill.prepareQCKK && !boss.playerSkill.prepareLaze && !boss.playerSkill.prepareTuSat) {
            Skill bind = usable(player, Skill.TROI, distance);
            if (bind != null) return bind;
        }
        Skill sleep = usable(player, Skill.THOI_MIEN, distance);
        if (sleep != null) return sleep;
        Skill chocolate = usable(player, Skill.SOCOLA, distance);
        if (chocolate != null) return chocolate;
        return usable(player, Skill.DICH_CHUYEN_TUC_THOI, distance);
    }

    private Skill preparedAttack(Player player, Player boss, long hp, long basicHit, double distance) {
        if (hp < basicHit * 6 || player.nPoint.hp < (long) player.nPoint.hpMax * 3 / 4) return null;
        if (!player.playerSkill.prepareLaze && player.nPoint.mp >= (long) player.nPoint.mpMax * 3 / 4) {
            Skill laser = usable(player, Skill.MAKANKOSAPPO, distance);
            if (laser != null && estimateHit(player, boss, laser) >= basicHit * 6) return laser;
        }
        return !player.playerSkill.prepareQCKK && alone(player) ? usable(player, Skill.QUA_CAU_KENH_KHI, distance) : null;
    }

    private boolean isPrepared(Player player) {
        return preparing.template.id == Skill.MAKANKOSAPPO ? player.playerSkill.prepareLaze : player.playerSkill.prepareQCKK;
    }

    private static boolean alone(Player player) {
        return player.zone.getNotBosses().stream().allMatch(other -> other == player);
    }

    private static boolean safeToCharge(Player player, double distance) {
        int safeDistance = 180;
        return distance >= safeDistance && player.zone.mobs.stream().noneMatch(mob -> !mob.isDie()
                && (mob.temporaryEnemies.contains(player) || mob.tempId > 18 || mob.tempId > 9 && mob.type == 4)
                && Util.getDistance(player, mob) < safeDistance)
                && player.zone.getBosses().stream().noneMatch(boss -> Util.getDistance(player, boss) < safeDistance
                        && SkillService.gI().canAttackPlayer(player, boss));
    }

    private static boolean canHealSelf(Player player, Skill heal) {
        if (player.typePk == ConstPlayer.PK_ALL || player.typePk == ConstPlayer.PK_PVP) return false;
        return heal.point <= 1 || player.zone.getNotBosses().stream()
                .noneMatch(other -> other != player && Util.getDistance(player, other) <= 300);
    }

    private static boolean safeStun(Player player, Skill stun) {
        int range = SkillUtil.getRangeStun(stun.point);
        return player.zone.getHumanoids().stream().noneMatch(other -> other != player && !other.isBoss
                && other.nPoint != null && !other.nPoint.khangTDHS && Util.getDistance(player, other) <= range
                && SkillService.gI().canAttackPlayer(player, other));
    }

    private static Skill usable(Player player, int id, double distance) {
        for (Skill skill : player.playerSkill.skills) {
            if (learned(skill) && skill.template.id == id && distance <= range(skill) && ready(player, skill)) return skill;
        }
        return null;
    }

    private static boolean learned(Skill skill) {
        return skill != null && skill.template != null && skill.skillId != -1 && skill.point > 0;
    }

    private static boolean ready(Player player, Skill skill) {
        Skill previous = player.playerSkill.skillSelect;
        player.playerSkill.skillSelect = skill;
        try {
            var service = SkillService.gI();
            return service.canUseSkillWithCooldown(player) && service.canUseSkillWithMana(player);
        } finally {
            player.playerSkill.skillSelect = previous;
        }
    }

    private static long kiCost(Player player, Skill skill) {
        return switch (skill.template.manaUseType) {
            case 1 -> (long) player.nPoint.mpMax * skill.manaUse / 100;
            case 2 -> player.nPoint.mp;
            default -> skill.manaUse;
        };
    }

    private static long estimateHit(Player player, Player boss, Skill skill) {
        int base = skill.template.id == Skill.MAKANKOSAPPO ? player.nPoint.mpMax : player.nPoint.dame;
        long hit = (long) base * Math.max(1, skill.damage) / 100;
        return Math.max(1, Math.min(2_000_000_000L, hit - (boss != null ? boss.nPoint.def : 0)));
    }
}
