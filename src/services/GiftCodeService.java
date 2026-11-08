package services;
import managers.GiftCodeManager;
import player.GiftCodeSystem;
import item.Item;
import java.util.Set;
import player.Player;
import player.Service.InventoryService;
import map.Service.NpcService;
import shop.ItemShop;
import shop.Shop;

public class GiftCodeService {

    private static GiftCodeService instance;

    public static GiftCodeService gI() {
        if (instance == null) {
            instance = new GiftCodeService();
        }
        return instance;
    }

    public void giftCode(Player player, String code) {
        GiftCodeSystem giftcode = GiftCodeManager.gI().checkUseGiftCode(player, code);
        if (giftcode == null) {
            Service.gI().sendThongBao(player, "Code không chính xác!");
        } else if (giftcode.timeCode()) {
            Service.gI().sendThongBao(player, "Code đã hết hạn");
        } else {
            Set<Integer> keySet = giftcode.detail.keySet();
            String textGift = "|0|Bạn vừa nhận được:\b";
            for (Integer key : keySet) {
                int idItem = key;
                int quantity = giftcode.detail.get(key);

                switch (idItem) {
                    case -1 -> {
                        player.inventory.gold = Math.min(player.inventory.gold + (long) quantity, 2000000000L);
                        textGift += "|2|" + quantity + " vàng\b";
                    }
                    case -2 -> {
                        player.inventory.gem = Math.min(player.inventory.gem + quantity, 200000000);
                        textGift += "|3|" + quantity + " ngọc\b";
                    }
                    case -3 -> {
                        player.inventory.ruby = Math.min(player.inventory.ruby + quantity, 200000000);
                        textGift += "|4|" + quantity + " ngọc khóa\b";
                    }
                    default -> {
                        Item itemGiftTemplate = ItemService.gI().createNewItem((short) idItem);
                        if (itemGiftTemplate != null) {
                            Item itemGift = new Item((short) idItem);

                            if (itemGift.template.type == 0 || itemGift.template.type == 1 || itemGift.template.type == 2 || itemGift.template.type == 3
                                    || itemGift.template.type == 4 || itemGift.template.type == 5) {
                                if (itemGift.template.id == 457) {
                                    itemGift.itemOptions.add(new Item.ItemOption(30, 0));
                                } else {
                                    itemGift.itemOptions = giftcode.option.get(key);
                                    itemGift.quantity = quantity;
                                    InventoryService.gI().addItemBag(player, itemGift);
                                }
                            } else {
                                itemGift.itemOptions = giftcode.option.get(key);
                                itemGift.quantity = quantity;
                                InventoryService.gI().addItemBag(player, itemGift);
                            }
                            textGift += "|1|x" + quantity + " " + itemGift.template.name + "\b";
                        }
                    }
                }
            }
            InventoryService.gI().sendItemBags(player);
            NpcService.gI().createTutorial(player, 1139, textGift);
        }
    }

}
