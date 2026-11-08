package npc.list;

import consts.ConstNpc;
import item.Item;
import matches.The23rdMartialArtCongress.DeathOrAliveArena;
import matches.tournament.DeathOrAliveArenaManager;
import matches.The23rdMartialArtCongress.DeathOrAliveArenaService;
import npc.Npc;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;
import services.Service;
import map.Service.ChangeMapService;
import combine.CombineService;
import combine.CheTaoCuonSachCu;
import combine.DoiSachTuyetKy;
import combine.NangCapVatPham;
import consts.ConstDailyGift;
import dailyGift.DailyGiftService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import shop.ShopService;
import utils.Util;

public class BaHatMit extends Npc {

    public BaHatMit(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (this.mapId) {
                case 5 ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi tìm ta có việc gì?",
                            "Chức năng\npha lê",
                            // "Chức năng\nđệ tử",
                            // "Chức năng\nSét Kích Hoạt",
                            // "Chức năng\nItem cấp 2",
                            "võ dài sinh tử"
                    );

                case 112 -> {
                    if (Util.isAfterMidnight(player.lastTimePKVoDaiSinhTu)) {
                        player.haveRewardVDST = false;
                        player.thoiVangVoDaiSinhTu = 0;
                    }
                    if (player.haveRewardVDST) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Đây là phần thưởng cho con.",
                                "1 vệ tinh\nngẫu nhiên");
                        return;
                    }
                    if (DeathOrAliveArenaManager.gI().getVDST(player.zone) != null) {
                        if (DeathOrAliveArenaManager.gI().getVDST(player.zone).getPlayer().equals(player)) {
                            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                    "Ngươi muốn hủy đăng ký thi đấu võ đài?",
                                    "Top 100", "Đồng ý\n" + player.thoiVangVoDaiSinhTu + " thỏi vàng", "Từ chối", "Về\nđảo rùa");
                            return;
                        }
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Ngươi muốn đăng ký thi đấu võ đài?\nnhiều phần thưởng giá trị đang đợi ngươi đó",
                                "Top 100", "Bình chọn", "Đồng ý\n" + player.thoiVangVoDaiSinhTu + " thỏi vàng", "Từ chối", "Về\nđảo rùa");
                        return;
                    }
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi muốn đăng ký thi đấu võ đài?\nnhiều phần thưởng giá trị đang đợi ngươi đó",
                            "Top 100", "Đồng ý\n" + player.thoiVangVoDaiSinhTu + " thỏi vàng", "Từ chối", "Về\nđảo rùa");
                }
                default -> {
                    List<String> menu = new ArrayList<>(Arrays.asList(
                        // "Sách\nTuyệt Kỹ", 
                    "Cửa hàng\nBùa", "Nâng cấp\nVật phẩm", "Làm phép\nNhập đá", "Nhập\nNgọc Rồng"));
                    // if (InventoryService.gI().findItem(player, 454) || InventoryService.gI().findItem(player, 921)) {
                    //     menu = new ArrayList<>(Arrays.asList(
                    //         // "Sách\nTuyệt Kỹ", 
                    //     "Cửa hàng\nBùa", "Nâng cấp\nVật phẩm", InventoryService.gI().findItemBongTaiCap2(player) ? "Mở chỉ số\nBông tai\nPorata cấp\n2" : "Nâng cấp\nBông tai\nPorata", "Làm phép\nNhập đá", "Nhập\nNgọc Rồng"));
                    // }
                    // if (DailyGiftService.checkDailyGift(player, ConstDailyGift.NHAN_BUA_MIEN_PHI)) {
                    //     menu.add(0, "Thưởng\nBùa 1h\nngẫu nhiên");
                    // }
                    String[] menus = menu.toArray(new String[0]);
                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?", menus);
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (this.mapId) {
                case 5 -> {
                    if (player.idMark.isBaseMenu()) {
                        switch (select) {
                            case 0 ->
                                createOtherMenu(player, 3,
                                        "Ta có thể giúp gì cho ngươi ?",
                                        "Ép sao\ntrang bị", 
                                        "Pha lê\nhóa\ntrang bị"
                                        // , 
                                        // "Nâng cấp\nSao pha lê",
                                        // "Đánh bóng\nSao pha lê",
                                        // "Cường hóa\nlỗ sao\npha lê", 
                                        // "Tạo đá\nHematite",
                                        // "Tạo\nDùi Đục",
                                        // "Tạo\nđá mài"
                                    );
                            // case 1 ->
                            //     createOtherMenu(player, 4,
                            //             "Ta có thể giúp gì cho ngươi ?",
                            //             "Nâng cấp\nđệ blackgoku",
                            //             "Nâng cấp\nđệ blackgoku rose");
                            // case 2 ->
                            //     createOtherMenu(player, 5,
                            //             "Ta có thể giúp gì cho ngươi ?",
                            //             "Nâng cấp\nSét Kích Hoạt",
                            //             "Nâng cấp\nSét Kích Hoạt Vip");
                            // case 3 ->
                            //     createOtherMenu(player, 6,
                            //             "Ta có thể giúp gì cho ngươi ?",
                            //             "Nâng cấp\nItem Cấp 2");
                            case 1 ->
                                ChangeMapService.gI().changeMapNonSpaceship(player, 112, 200 + Util.nextInt(-100, 100), 408);
                        }
                    } else if (player.idMark.getIndexMenu() == 3) {
                        switch (select) {
                            case 0:
                                CombineService.gI().openTabCombine(player, CombineService.EP_SAO_TRANG_BI);
                                break;
                            case 1:
                                CombineService.gI().openTabCombine(player, CombineService.PHA_LE_HOA_TRANG_BI);
                                break;
                        //     case 2:
                        //         CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_SAO_PHA_LE);
                        //         break;
                        //     case 3:
                        //         CombineService.gI().openTabCombine(player, CombineService.DANH_BONG_SAO_PHA_LE);
                        //         break;
                        //     case 4:
                        //         CombineService.gI().openTabCombine(player, CombineService.CUONG_HOA_LO_SAO_PHA_LE);
                        //         break;
                        //     case 5:
                        //         CombineService.gI().openTabCombine(player, CombineService.TAO_DA_HEMATITE);
                        //         break;
                        //     case 6:
                        //         CombineService.gI().openTabCombine(player, CombineService.DUI_DUC);
                        //         break;
                        //     case 7:
                        //         CombineService.gI().openTabCombine(player, CombineService.DA_MAI);
                        //         break;
                         }
                    } else if (player.idMark.getIndexMenu() == 4) {
                        switch (select) {
                            case 0:
                                CombineService.gI().openTabCombine(player, CombineService.NANG_DE_DE_BLACK_GOKU);
                                break;
                            case 1:
                                CombineService.gI().openTabCombine(player,CombineService.NANG_CAP_DE_TU_BLACK_GOKU_ROSE);
                        }
                    } else if (player.idMark.getIndexMenu() == 5) {
                        switch (select) {
                            case 0:
                                CombineService.gI().openTabCombine(player, CombineService.DAP_SET_KICH_HOAT);
                                break;
                            case 1:
                                CombineService.gI().openTabCombine(player, CombineService.DAP_SET_KICH_HOAT_VIP);
                                break;
                        }
                    } else if (player.idMark.getIndexMenu() == 6) {
                        switch (select) {
                            case 0:
                                CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_ITEM_CAP_2);
                                break;
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                        switch (player.combineNew.typeCombine) {
                            case CombineService.EP_SAO_TRANG_BI,
                                    CombineService.PHA_LE_HOA_TRANG_BI, 
                                    CombineService.NANG_CAP_SAO_PHA_LE, 
                                    CombineService.DANH_BONG_SAO_PHA_LE,
                                    CombineService.CUONG_HOA_LO_SAO_PHA_LE, 
                                    CombineService.TAO_DA_HEMATITE,
                                    CombineService.DUI_DUC,
                                    CombineService.DA_MAI,
                                    CombineService.NANG_DE_DE_BLACK_GOKU,
                                    CombineService.DAP_SET_KICH_HOAT,
                                    CombineService.NANG_CAP_ITEM_CAP_2,
                                    CombineService.NANG_CAP_DE_TU_BLACK_GOKU_ROSE,
                                    CombineService.DAP_SET_KICH_HOAT_VIP -> {
                                switch (select) {
                                    case 0 ->
                                        CombineService.gI().startCombine(player);
                                    case 1 ->
                                        CombineService.gI().startCombineVip(player, 10);
                                    case 2 ->
                                        CombineService.gI().startCombineVip(player, 100);
                                    default -> {
                                    }
                                }
                            }
                        }
                    }
                }
                case 112 -> {
                    if (player.idMark.isBaseMenu()) {
                        if (player.haveRewardVDST) {
                            switch (select) {
                                case 0 -> {
                                    if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                        Item item = ItemService.gI().createNewItem((short) (Util.nextInt(342, 345)));
                                        item.itemOptions.add(new Item.ItemOption(93, 30));
                                        InventoryService.gI().addItemBag(player, item);
                                        InventoryService.gI().sendItemBags(player);
                                        Service.gI().sendThongBao(player, "Bạn nhận được " + item.template.name);
                                        player.haveRewardVDST = false;
                                    } else {
                                        Service.gI().sendThongBao(player, "Hành trang không còn chỗ trống, không thể nhặt thêm");
                                    }
                                }
                            }
                            return;
                        }
                        if (DeathOrAliveArenaManager.gI().getVDST(player.zone) != null) {
                            if (DeathOrAliveArenaManager.gI().getVDST(player.zone).getPlayer().equals(player)) {
                                switch (select) {
                                    case 0 -> {
                                    }
                                    case 1 ->
                                        this.npcChat("Không thể thực hiện");
                                    case 2 -> {
                                    }
                                    case 3 ->
                                        ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                                }
                                return;
                            }
                            switch (select) {
                                case 0 -> {
                                }
                                case 1 ->
                                    this.createOtherMenu(player, ConstNpc.DAT_CUOC_HAT_MIT,
                                            "Phí bình chọn là 1 triệu vàng\nkhi trận đấu kết thúc\n90% tổng tiền bình chọn sẽ chia đều cho phe bình chọn chính xác",
                                            "Bình chọn cho " + DeathOrAliveArenaManager.gI().getVDST(player.zone).getPlayer().name + " (" + DeathOrAliveArenaManager.gI().getVDST(player.zone).getCuocPlayer() + ")",
                                            "Bình chọn cho hạt mít (" + DeathOrAliveArenaManager.gI().getVDST(player.zone).getCuocBaHatMit() + ")");
                                case 2 ->
                                    DeathOrAliveArenaService.gI().startChallenge(player);
                                case 3 -> {
                                }
                                case 4 ->
                                    ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                            }
                            return;
                        }
                        switch (select) {
                            case 0 -> {
                            }
                            case 1 ->
                                DeathOrAliveArenaService.gI().startChallenge(player);
                            case 2 -> {
                            }
                            case 3 ->
                                ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.DAT_CUOC_HAT_MIT) {
                        if (DeathOrAliveArenaManager.gI().getVDST(player.zone) != null) {
                            switch (select) {
                                case 0 -> {
                                    if (player.inventory.gold >= 1_000_000) {
                                        DeathOrAliveArena vdst = DeathOrAliveArenaManager.gI().getVDST(player.zone);
                                        vdst.setCuocPlayer(vdst.getCuocPlayer() + 1);
                                        vdst.addBinhChon(player);
                                        player.binhChonPlayer++;
                                        player.zoneBinhChon = player.zone;
                                        player.inventory.gold -= 1_000_000;
                                        Service.gI().sendMoney(player);
                                    } else {
                                        Service.gI().sendThongBao(player, "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(1_000_000 - player.inventory.gold) + " vàng nữa");
                                    }
                                }
                                case 1 -> {
                                    if (player.inventory.gold >= 1_000_000) {
                                        DeathOrAliveArena vdst = DeathOrAliveArenaManager.gI().getVDST(player.zone);
                                        vdst.setCuocBaHatMit(vdst.getCuocBaHatMit() + 1);
                                        vdst.addBinhChon(player);
                                        player.binhChonHatMit++;
                                        player.zoneBinhChon = player.zone;
                                        player.inventory.gold -= 1_000_000;
                                        Service.gI().sendMoney(player);
                                    } else {
                                        Service.gI().sendThongBao(player, "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(1_000_000 - player.inventory.gold) + " vàng nữa");
                                    }
                                }
                            }
                        }
                    }
                }
                case 42, 43, 44, 84 -> {
                    if (player.idMark.isBaseMenu()) {
                        if (!DailyGiftService.checkDailyGift(player, ConstDailyGift.NHAN_BUA_MIEN_PHI)) {
                            select++;
                        }
                        if (!InventoryService.gI().findItem(player, 454) && !InventoryService.gI().findItem(player, 921)) {
                            if (select >= 4) {
                                select++;
                            }
                        }
                        switch (select) {
                            // case 0:
                            //     if (DailyGiftService.checkDailyGift(player, ConstDailyGift.NHAN_BUA_MIEN_PHI)) {
                            //         int idItem = Util.nextInt(213, 219);
                            //         player.charms.addTimeCharms(idItem, 60);
                            //         Item bua = ItemService.gI().createNewItem((short) idItem);
                            //         Service.gI().sendThongBao(player, "Bạn vừa nhận thưởng " + bua.template.name);
                            //         DailyGiftService.updateDailyGift(player, ConstDailyGift.NHAN_BUA_MIEN_PHI);
                            //     } else {
                            //         Service.gI().sendThongBao(player, "Hôm nay bạn đã nhận bùa miễn phí rồi!!!");
                            //     }
                            //     break;
                            // case 1:
                            //     createOtherMenu(player, ConstNpc.MENU_SACH_TUYET_KY, "Ta có thể giúp gì cho ngươi ?",
                            //             "Đóng thành\nSách cũ",
                            //             "Đổi Sách\nTuyệt kỹ",
                            //             "Giám định\nSách",
                            //             "Tẩy\nSách",
                            //             "Nâng cấp\nSách\nTuyệt kỹ",
                            //             "Hồi phục\nSách",
                            //             "Phân rã\nSách");
                            //     break;
                            case 0:
                                createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA, "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để " + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                        // "Bùa\n1 giờ",
                                        "Bùa\n8 giờ"
                                        // ,
                                        // "Bùa\n1 tháng"
                                        , "Đóng");
                                break;
                            case 1:
                                CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_VAT_PHAM);
                                break;
                            // case 3:
                            //     if (InventoryService.gI().findItemBongTaiCap2(player)) {
                            //         CombineService.gI().openTabCombine(player, CombineService.NANG_CHI_SO_BONG_TAI);
                            //     } else {
                            //         CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_BONG_TAI);
                            //     }
                            //     break;
                            case 2:
                                CombineService.gI().openTabCombine(player, CombineService.LAM_PHEP_NHAP_DA);
                                break;
                            case 3:
                                CombineService.gI().openTabCombine(player, CombineService.NHAP_NGOC_RONG);
                                break;
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_SACH_TUYET_KY) {
                        switch (select) {
                            case 0:
                                CheTaoCuonSachCu.showCombine(player);
                                break;
                            case 1:
                                DoiSachTuyetKy.showCombine(player);
                                break;
                            case 2:
                                CombineService.gI().openTabCombine(player, CombineService.GIAM_DINH_SACH);
                                break;
                            case 3:
                                CombineService.gI().openTabCombine(player, CombineService.TAY_SACH);
                                break;
                            case 4:
                                CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_SACH_TUYET_KY);
                                break;
                            case 5:
                                CombineService.gI().openTabCombine(player, CombineService.HOI_PHUC_SACH);
                                break;
                            case 6:
                                CombineService.gI().openTabCombine(player, CombineService.PHAN_RA_SACH);
                                break;
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.DONG_THANH_SACH_CU) {
                        CheTaoCuonSachCu.cheTaoCuonSachCu(player);
                    } else if (player.idMark.getIndexMenu() == ConstNpc.DOI_SACH_TUYET_KY) {
                        DoiSachTuyetKy.doiSachTuyetKy(player);
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_OPTION_SHOP_BUA) {
                        switch (select) {
                            // case 0 ->
                            //     ShopService.gI().opendShop(player, "BUA_1H", true);
                            case 0 ->
                                ShopService.gI().opendShop(player, "BUA_8H", true);
                            // case 2 ->
                            //     ShopService.gI().opendShop(player, "BUA_1M", true);
                        }
                    } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                        switch (player.combineNew.typeCombine) {
                            case CombineService.NANG_CAP_BONG_TAI, 
                                    CombineService.NANG_CHI_SO_BONG_TAI, 
                                    CombineService.LAM_PHEP_NHAP_DA, 
                                    CombineService.NHAP_NGOC_RONG, 
                                    CombineService.GIAM_DINH_SACH, 
                                    CombineService.TAY_SACH, 
                                    CombineService.NANG_CAP_SACH_TUYET_KY,
                                    CombineService.HOI_PHUC_SACH, 
                                    CombineService.PHAN_RA_SACH -> {
                                if (select == 0) {
                                    CombineService.gI().startCombine(player);
                                }
                            }
                            case CombineService.NANG_CAP_VAT_PHAM -> {
                                if (select == 0) {
                                    CombineService.gI().startCombine(player);
                                } else if (select == 1) {
                                    NangCapVatPham.nangCapVatPham(player);
                                }
                            }
                        }
                    }
                }
                default -> {
                }
            }
        }
    }
}