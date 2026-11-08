package npc.list;

import boss.BossID;
import consts.ConstNpc;
import item.Item;
import java.io.IOException;
import network.Message;
import npc.Npc;
import player.Player;
import player.Service.InventoryService;
import services.Service;
import services.SkillService;
import combine.CombineService;
import Deputyhead.Service.TrainingService;
import shop.ShopService;
import skill.Skill;
import utils.SkillUtil;
import utils.Util;

public class Whis extends Npc {

    private static final int COST_HD = 50000000;

    public Whis(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 154) {
                Item Biky = InventoryService.gI().findItem(player.inventory.itemsBag, 1229);
                if (Biky != null) {
                    createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Thử đánh với ta xem nào.\nNgươi còn 1 lượt nữa cơ mà.",
                            "Nói chuyện", "Học\ntuyệt kỹ", "Top 100", "[LV:" + (player.traning.getTop() + 1) + "]");
                } else {
                    createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Thử đánh với ta xem nào.\nNgươi còn 1 lượt nữa cơ mà.",
                            "Nói chuyện", "Top 100", "[LV:" + (player.traning.getTop() + 1) + "]");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;

        if (player.idMark.isBaseMenu()) {
            if (this.mapId == 154) {
                Item Biky = InventoryService.gI().findItem(player.inventory.itemsBag, 1229);
                if (Biky != null) {
                    switch (select) {
                        case 0 -> this.createOtherMenu(player, 5, "Ta sẽ giúp ngươi chế tạo trang bị thiên sứ", "Shop thiên sứ", "Chế tạo", "Từ chối");
                        case 1 -> openLearnSkillMenu(player, Biky);
                        case 3 -> TrainingService.gI().callBoss(player, BossID.WHIS, false);
                        default -> {}
                    }
                } else {
                    switch (select) {
                        case 0 -> this.createOtherMenu(player, 5, "Ta sẽ giúp ngươi chế tạo trang bị thiên sứ", "Shop thiên sứ", "Chế tạo", "Từ chối");
                        case 2 -> TrainingService.gI().callBoss(player, BossID.WHIS, false);
                        default -> {}
                    }
                }
            }
        } else if (player.idMark.getIndexMenu() == 5) {
            switch (select) {
                case 0 -> ShopService.gI().opendShop(player, "THIEN_SU", false);
                case 1 -> {
                    if (!player.setClothes.checkSetDes()) {
                        this.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Ngươi hãy trang bị đủ 5 món trang bị Hủy Diệt rồi ta nói chuyện tiếp.", "OK");
                    } else {
                        CombineService.gI().openTabCombine(player, CombineService.CHE_TAO_TRANG_BI_THIEN_SU);
                    }
                }
            }
        } else if (player.idMark.getIndexMenu() == CombineService.CHE_TAO_TRANG_BI_THIEN_SU) {
            if (select == 0) CombineService.gI().startCombine(player);
        } else if (player.idMark.getIndexMenu() == 6) {
            if (select == 0) confirmLearnSkill(player);
        }
    }

    private void openLearnSkillMenu(Player player, Item Biky) {
        int idSkill = switch (player.gender) {
            case 0 -> Skill.SUPER_KAME;
            case 1 -> Skill.MA_PHONG_BA;
            case 2 -> Skill.LIEN_HOAN_CHUONG;
            default -> Skill.MA_PHONG_BA;
        };
        Skill currentSkill = SkillUtil.getSkillbyId(player, idSkill);
        boolean firstLearn = currentSkill == null || currentSkill.point == 0;
        int BikyRequire = firstLearn ? 9999 : 999; // Điều kiện yêu cầu bí kíp
        int skillLevel = firstLearn ? 1 : currentSkill.point + 1;

        String skillName = switch (player.gender) {
            case 0 -> "Super kamejoko";
            case 1 -> "Ma phong ba";
            case 2 -> "Ca đíc liên hoàn chưởng";
            default -> "Skill đặc biệt";
        };

        createOtherMenu(player, 6,
                "|1|Ta sẽ dạy ngươi tuyệt kỹ " + skillName + " " + skillLevel +
                        "\n|7|Bí kiếp tuyệt kỹ: " + Biky.quantity + "/" + BikyRequire +
                        "\n|2|Giá vàng: 10.000.000\n|2|Giá ngọc: 99",
                "Đồng ý", "Từ chối");
    }

    private void confirmLearnSkill(Player player) {
        Item Biky = InventoryService.gI().findItemBag(player, 1229);
        if (Biky == null) return;

        if (player.nPoint.power < 60000000000L) {
            Service.gI().sendThongBao(player, "Ngươi không đủ sức mạnh để học tuyệt kỹ");
            return;
        }
        if (player.inventory.gold < 10000000) {
            Service.gI().sendThongBao(player, "Hãy có đủ vàng thì quay lại gặp ta.");
            return;
        }
        if (player.inventory.gem <= 99) {
            Service.gI().sendThongBao(player, "Hãy có đủ ngọc xanh thì quay lại gặp ta.");
            return;
        }

        int idSkill = switch (player.gender) {
            case 0 -> Skill.SUPER_KAME;
            case 1 -> Skill.MA_PHONG_BA;
            case 2 -> Skill.LIEN_HOAN_CHUONG;
            default -> Skill.MA_PHONG_BA;
        };
        Skill currentSkill = SkillUtil.getSkillbyId(player, idSkill);
        boolean firstLearn = currentSkill == null || currentSkill.point == 0;
        int BikyRequire = firstLearn ? 9999 : 999;

        if (Biky.quantity < BikyRequire) {
            Service.gI().sendThongBao(player, "Ngươi còn thiếu " + (BikyRequire - Biky.quantity) + " bí kíp nữa.\nHãy tìm đủ rồi đến gặp ta.");
            return;
        }

        try {
            boolean success = firstLearn ? Util.isTrue(15, 15) : Util.isTrue(1, 30);
            int trubk = success ? BikyRequire : 99;
            int iconSkill = success ? switch (player.gender) {
                case 0 -> 11162;
                case 1 -> 11194;
                case 2 -> 11193;
                default -> 11194;
            } : 15313;
            String msg = success ? (firstLearn ? "Học skill thành công!" : "Nâng skill thành công!") : "Tư chất kém!";
            String msg2 = success ? "Chúc mừng con nhé!" : "Ngu dốt!";

            if (success) {
                if (firstLearn) {
                    SkillService.gI().learSkillSpecial(player, (byte) idSkill);
                } else {
                    currentSkill.point++;
                    currentSkill.currLevel = 0;
                    SkillService.gI().sendCurrLevelSpecial(player, currentSkill);
                }
            }

            Message ms = new Message(-81);
            ms.writer().writeByte(0);
            ms.writer().writeUTF("Skill 9");
            ms.writer().writeUTF("NgocRongBlackGoku");
            ms.writer().writeShort(tempId);
            player.sendMessage(ms);
            ms.cleanup();

            ms = new Message(-81);
            ms.writer().writeByte(1);
            ms.writer().writeByte(1);
            ms.writer().writeByte(InventoryService.gI().getIndexItemBag(player, Biky));
            player.sendMessage(ms);
            ms.cleanup();

            ms = new Message(-81);
            ms.writer().writeByte(trubk == 99 ? 8 : 7);
            ms.writer().writeShort(iconSkill);
            player.sendMessage(ms);
            ms.cleanup();

            npcChat(player, msg2);
            Service.gI().sendThongBao(player, msg);

            InventoryService.gI().subQuantityItemsBag(player, Biky, trubk);
            player.inventory.gold -= 10000000;
            player.inventory.gem -= 99;
            InventoryService.gI().sendItemBags(player);

        } catch (IOException e) {
        }
    }
}