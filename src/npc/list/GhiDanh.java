package npc.list;

import consts.ConstNpc;
import item.Item;
import matches.The23rdMartialArtCongress.The23rdMartialArtCongressService;
import matches.The23rdMartialArtCongress.WorldMartialArtsTournamentService;
import npc.Npc;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;
import map.Service.NpcService;
import player.Service.PlayerService;
import services.Service;
import map.Service.ChangeMapService;
import utils.Util;

public class GhiDanh extends Npc {

    String[] menuSelect = new String[]{};

    public GhiDanh(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player pl) {
        if (canOpenNpc(pl)) {
            switch (this.mapId) {
                case 52 -> WorldMartialArtsTournamentService.menu(this, pl);
                case 129 -> {
                    if (Util.isAfterMidnight(pl.lastTimePKDHVT23)) {
                        pl.goldChallenge = 50_000;
                        pl.rubyChallenge = 2;
                        pl.levelWoodChest = 0;
                    }
                    if (pl.levelWoodChest == 0) {
                        menuSelect = new String[]{
                            "Hướng\ndẫn\nthêm",
                            "Thi đấu\n" + Util.numberToMoney(pl.rubyChallenge) + " ngọc",
                            "Thi đấu\n" + Util.numberToMoney(pl.goldChallenge) + " vàng",
                            "Về\nĐại Hội\nVõ Thuật"
                        };
                    } else {
                        menuSelect = new String[]{
                            "Hướng\ndẫn\nthêm",
                            "Thi đấu\n" + Util.numberToMoney(pl.rubyChallenge) + " ngọc",
                            "Thi đấu\n" + Util.numberToMoney(pl.goldChallenge) + " vàng",
                            "Nhận\nthưởng\nRương Cấp\n" + pl.levelWoodChest,
                            "Về\nĐại Hội\nVõ Thuật"
                        };
                    }
                    this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                        "Đại hội võ thuật lần thứ 23\nDiễn ra bất kể ngày đêm, ngày nghỉ, ngày lễ\nPhần thưởng vô cùng quý giá\nNhanh chóng tham gia nào",
                        menuSelect, "Từ chối");
                }

                default -> super.openBaseMenu(pl);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;

        if (this.mapId == 52) {
            WorldMartialArtsTournamentService.confirm(this, player, select);
            return;
        }

        if (this.mapId == 129) {
            switch (player.idMark.getIndexMenu()) {
                case ConstNpc.BASE_MENU -> handleBaseMenu(player, select);
                case 1 -> handleRewardMenu(player, select);
            }
        }
    }

    private void handleBaseMenu(Player player, int select) {
        long goldChallenge = player.goldChallenge;
        long rubyChallenge = player.rubyChallenge;

        if (player.levelWoodChest == 0) {
            switch (select) {
                case 0 -> NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.NPC_DHVT23);

                case 1, 2 -> tryChallenge(player, select == 1, rubyChallenge, goldChallenge);

                case 3 -> ChangeMapService.gI().changeMapNonSpaceship(player, 52, player.location.x, 336);
            }
        } else {
            switch (select) {
                case 0 -> NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.NPC_DHVT23);

                case 1, 2 -> tryChallenge(player, select == 1, rubyChallenge, goldChallenge);

                case 3 -> this.createOtherMenu(player, 1,
                    "Phần thưởng của bạn đang ở cấp " + player.levelWoodChest + " / 12\n"
                        + "Mỗi ngày chỉ được nhận phần thưởng 1 lần\n"
                        + "Bạn có chắc sẽ nhận phần thưởng ngay bây giờ?",
                    "OK", "Từ chối");

                case 4 -> ChangeMapService.gI().changeMapNonSpaceship(player, 52, player.location.x, 336);
            }
        }
    }

    private void tryChallenge(Player player, boolean useRuby, long rubyChallenge, long goldChallenge) {
        if (player.levelWoodChest == 12) {
            Service.gI().sendThongBao(player, "Bạn đã vô địch giải. Vui lòng chờ đến ngày mai");
            return;
        }

        if (!InventoryService.gI().finditemWoodChest(player)) {
            Service.gI().sendThongBao(player, "Hãy mở rương báu vật trước");
            return;
        }

        if (useRuby) {
            if (player.inventory.gem >= rubyChallenge) {
                The23rdMartialArtCongressService.gI().startChallenge(player);
                player.inventory.gem -= rubyChallenge;
                afterChallengeSuccess(player);
            } else {
                Service.gI().sendThongBao(player,
                    "Bạn không đủ ngọc, còn thiếu " + Util.numberToMoney(rubyChallenge - player.inventory.gem) + " ngọc nữa");
            }
        } else {
            if (player.inventory.gold >= goldChallenge) {
                The23rdMartialArtCongressService.gI().startChallenge(player);
                player.inventory.gold -= goldChallenge;
                afterChallengeSuccess(player);
            } else {
                Service.gI().sendThongBao(player,
                    "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(goldChallenge - player.inventory.gold) + " vàng nữa");
            }
        }
    }

    private void afterChallengeSuccess(Player player) {
        PlayerService.gI().sendInfoHpMpMoney(player);
        player.goldChallenge *= 2;
        player.rubyChallenge += 2;
    }

    private void handleRewardMenu(Player player, int select) {
        if (select == 0) {
            if (!InventoryService.gI().finditemWoodChest(player)) {
                Service.gI().sendThongBao(player, "Hãy mở rương báu vật trước");
                return;
            }
            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                Item chest = ItemService.gI().createNewItem((short) 570);
                chest.itemOptions.add(new Item.ItemOption(72, player.levelWoodChest));
                chest.itemOptions.add(new Item.ItemOption(30, 0));
                chest.createTime = System.currentTimeMillis();

                InventoryService.gI().addItemBag(player, chest);
                InventoryService.gI().sendItemBags(player);

                player.levelWoodChest = 0;
                player.lastTimeRewardWoodChest = System.currentTimeMillis();

                NpcService.gI().createMenuConMeo(player, -1, -1,
                    "Bạn nhận được\n|1|Rương Gỗ\n|2|Giấu bên trong nhiều vật phẩm quý giá", "OK");
            } else {
                this.npcChat(player, "Hành trang đã đầy, cần một ô trống trong hành trang để nhận vật phẩm");
            }
        }
    }
}