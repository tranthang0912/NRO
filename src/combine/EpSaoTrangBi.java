package combine;

import consts.ConstNpc;
import item.Item;
import item.Item.ItemOption;
import player.Player;
import player.Service.InventoryService;
import services.ItemService;
import services.Service;
import utils.Util;

public class EpSaoTrangBi {

    // Constants
    public static final int MAX_STAR = 9;
    public static final int STAR_8 = 8;
    public static final int STAR_9 = 9;
    public static final int MIN_GEM_REQUIRED = 1; 
    public static final int OPTION_STAR_ID = 102;
    public static final int OPTION_SLOT_ID = 107;
    public static final int GEM_OPTION_ID = 30;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item trangBi = null;
            Item daPhaLe = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (CombineSystem.isTrangBiPhaLeHoa(item)) {
                    trangBi = item;
                } else if (CombineSystem.isDaPhaLe(item)) {
                    daPhaLe = item;
                }
            }
            int star = 0;
            int starEmpty = 0;
            if (trangBi != null && daPhaLe != null) {
                for (ItemOption io : trangBi.itemOptions) {
                    if (io.optionTemplate.id == OPTION_STAR_ID) {
                        star = io.param;
                    } else if (io.optionTemplate.id == OPTION_SLOT_ID) {
                        starEmpty = io.param;
                    }
                }
                if (starEmpty <= MAX_STAR) {
                    if (starEmpty >= STAR_8 && !CombineService.gI().CheckSlot(trangBi, starEmpty)) {
                        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                "Cần cường hóa lỗ sao pha lê thứ " + (starEmpty == STAR_8 ? "8" : "9") + " trước khi ép vào", "Đóng");
                        return;
                    }

                    player.combineNew.gemCombine = CombineSystem.getGemEpSao(star);
                    String npcSay = trangBi.template.name + "\n|2|";
                    for (ItemOption io : trangBi.itemOptions) {
                        if (io.optionTemplate.id != OPTION_STAR_ID) {
                            npcSay += io.getOptionString() + "\n";
                        }
                    }

                    if (daPhaLe.template.type == GEM_OPTION_ID) {
                        for (ItemOption io : daPhaLe.itemOptions) {
                            npcSay += "|7|" + io.getOptionString() + "\n";
                        }
                    } else {
                        npcSay += "|7|" + ItemService.gI().getItemOptionTemplate(CombineSystem.getOptionDaPhaLe(daPhaLe)).name
                                .replaceAll("#", CombineSystem.getParamDaPhaLe(daPhaLe) + "") + "\n";
                    }
                    npcSay += "|1|Cần " + Util.numberToMoney(player.combineNew.gemCombine) + " ngọc";
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Nâng cấp\ncần " + player.combineNew.gemCombine + " ngọc");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Cần 1 trang bị có lỗ sao pha lê và 1 loại đá pha lê để ép vào, và lỗ sao tối đa là 9", "Đóng");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 trang bị có lỗ sao pha lê và 1 loại đá pha lê để ép vào", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 trang bị có lỗ sao pha lê và 1 loại đá pha lê để ép vào", "Đóng");
        }
    }

    public static void epSaoTrangBi(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {

            int gem = player.combineNew.gemCombine;

            if (player.inventory.gem < gem) {
                Service.gI().sendThongBao(player, "Không đủ ngọc để thực hiện");
                return;
            }

            Item trangBi = null;
            Item daPhaLe = null;

            for (Item item : player.combineNew.itemsCombine) {
                if (CombineSystem.isTrangBiPhaLeHoa(item)) {
                    trangBi = item; 
                } else if (CombineSystem.isDaPhaLe(item)) {
                    daPhaLe = item; 
                }
            }

            int star = 0;
            int starEmpty = 0;

            if (trangBi != null && daPhaLe != null) {
                ItemOption optionStar = null;

                for (ItemOption io : trangBi.itemOptions) {
                    if (io.optionTemplate.id == OPTION_STAR_ID) {
                        star = io.param;  
                        optionStar = io;
                    } else if (io.optionTemplate.id == OPTION_SLOT_ID) {
                        starEmpty = io.param;
                    }
                }

                if (star < starEmpty) {
                    if (starEmpty >= STAR_8 && !CombineService.gI().CheckSlot(trangBi, starEmpty)) {
                        Service.gI().sendThongBao(player, "Cần cường hóa lỗ sao pha lê thứ " + (starEmpty == STAR_8 ? "8" : "9") + " trước khi ép vào");
                        return;
                    }

                    player.inventory.subGem(gem);

                    int optionId = CombineSystem.getOptionDaPhaLe(daPhaLe);
                    int param = CombineSystem.getParamDaPhaLe(daPhaLe);

                    ItemOption option = null;
                    for (ItemOption io : trangBi.itemOptions) {
                        if (io.optionTemplate.id == optionId) {
                            option = io;
                            break;
                        }
                    }

                    if (optionStar != null && starEmpty >= STAR_8) {
                        ItemOption newOption = new ItemOption(optionId, param);
                        trangBi.itemOptions.add(newOption);  
                        if (starEmpty == STAR_8) {
                            optionStar.param = STAR_8;
                            Service.gI().sendThongBao(player, "Đã ép sao lên 8 thành công!");
                        } else if (starEmpty == STAR_9) {
                            optionStar.param = STAR_9;
                            Service.gI().sendThongBao(player, "Đã ép sao lên 9 thành công!");
                        }
                    } else {
                        if (option != null) {
                            option.param += param; 
                        } else {
                            trangBi.itemOptions.add(new ItemOption(optionId, param));
                        }

                        if (optionStar != null) {
                            optionStar.param++;
                        } else {
                            trangBi.itemOptions.add(new ItemOption(OPTION_STAR_ID, 1));  
                        }
                    }

                    InventoryService.gI().subQuantityItemsBag(player, daPhaLe, 1);
                    CombineService.gI().sendEffectSuccessCombine(player);
                    InventoryService.gI().sendItemBags(player);
                    Service.gI().sendMoney(player);
                    CombineService.gI().reOpenItemCombine(player);
                }
            }
        }
    }
}