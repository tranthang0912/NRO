package web;

import item.Item;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import map.ItemMap;
import player.Player;
import player.Inventory;
import player.Service.InventoryService;
import services.ItemService;

/** Chooses ground loot for either automation mode on the world thread. */
final class WebAutoLoot {
    private final Map<ItemMap, Long> blocked = new IdentityHashMap<>();
    private ItemMap current;
    private long lastProgress;
    private double bestDistance;

    void clear() {
        current = null;
        blocked.clear();
    }

    ItemMap select(Player player, long now) {
        blocked.entrySet().removeIf(entry -> entry.getValue() <= now || entry.getKey().zone != player.zone
                || !player.zone.items.contains(entry.getKey()));
        if (current != null && player.zone.items.contains(current) && eligible(player, current)) {
            double distance = distance(player, current);
            if (distance + 8 < bestDistance) { bestDistance = distance; lastProgress = now; }
            if (now - lastProgress < 8_000) return current;
            defer(current, now);
        }
        current = null;
        bestDistance = Double.POSITIVE_INFINITY;
        for (ItemMap item : player.zone.items) {
            if (blocked.containsKey(item) || !eligible(player, item)) continue;
            double distance = distance(player, item);
            if (distance < bestDistance) { current = item; bestDistance = distance; }
        }
        if (current != null) lastProgress = now;
        return current;
    }

    void defer(ItemMap item, long now) {
        blocked.put(item, now + 15_000);
        if (current == item) current = null;
    }

    private static boolean eligible(Player player, ItemMap item) {
        if (item == null || item.zone != player.zone || item.itemTemplate == null || item.isPickedUp) return false;
        int id = item.itemTemplate.id;
        // Carrying these starts a PvP event and imposes map-transfer / resurrection rules.
        // Keep event participation manual so it cannot interrupt either automation mode.
        if (item.isNamecBall || item.isBlackBall || id == 362) return false;
        // These are scenery / personal quest interactions, handled by their own quest step.
        if (id == 74 || id == 78 || item.itemTemplate.type == 22) return false;
        int nativeOwner = Math.abs(item.playerId > 100_000_000
                ? 1_000_000_000 - (int) item.playerId : (int) item.playerId);
        if (item.playerId != -1 && item.playerId != player.id && nativeOwner != player.id) return false;
        if (!player.zone.isItemVisibleTo(player, item)) return false;
        if (id == 648 && !InventoryService.gI().findItemTatVoGiangSinh(player)) return false;
        return canFit(player, item);
    }

    private static boolean canFit(Player player, ItemMap item) {
        int id = item.itemTemplate.id, type = item.itemTemplate.type;
        if (type == 9) return player.inventory.gold <= Inventory.LIMIT_GOLD - item.quantity;
        if (type == 10 || type == 34 || type == 13 || id == 453) return true;
        if (id == 517) return player.inventory.itemsBag.size() < Inventory.MAX_ITEMS_BAG;
        if (id == 518) return player.inventory.itemsBox.size() < Inventory.MAX_ITEMS_BOX;
        if (InventoryService.gI().getCountEmptyBag(player) > 0) return true;
        // Reuse native stacking rules on isolated copies. A failed native add can otherwise
        // partially fill a stack while leaving the original ground drop unchanged.
        var stacks = new ArrayList<Item>();
        for (Item stored : player.inventory.itemsBag) {
            if (stored.isNotNullItem() && stored.template.id == id) stacks.add(ItemService.gI().copyItem(stored));
        }
        if (stacks.isEmpty()) return false;
        var incoming = new Item();
        incoming.template = item.itemTemplate;
        incoming.quantity = item.quantity;
        for (var option : item.options) incoming.itemOptions.add(new Item.ItemOption(option));
        return InventoryService.gI().addItemList(stacks, incoming);
    }

    private static double distance(Player player, ItemMap item) {
        return GameWorld.distance(player.location.x, player.location.y, item.x, item.y);
    }
}
