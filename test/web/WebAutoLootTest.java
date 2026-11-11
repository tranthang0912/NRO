package web;

import item.Item;
import item.Template.ItemOptionTemplate;
import item.Template.ItemTemplate;
import map.ItemMap;
import map.Zone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import player.Player;
import server.Manager;

import static org.junit.jupiter.api.Assertions.*;

class WebAutoLootTest {
    @Test void collectsInventoryDragonBallsWithoutStartingCarriedPvpEventsThatLockTravel() {
        ItemMap ordinary = drop(14, 12, 250, -1);
        drop(353, 12, 5, -1);
        drop(372, 12, 6, -1);
        drop(362, 12, 7, -1);
        assertSame(ordinary, loot.select(player, 1_000));
        zone.removeItemMap(ordinary);
        assertNull(loot.select(player, 1_100));
    }

    private final Player player = new Player();
    private final Zone zone = new Zone(null, 0, 5);
    private final WebAutoLoot loot = new WebAutoLoot();

    WebAutoLootTest() {
        player.id = 42;
        player.zone = zone;
        player.inventory.itemsBag.add(new Item());
    }

    @BeforeAll static void provideDefaultOption() {
        while (Manager.ITEM_OPTION_TEMPLATES.size() <= 73) Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate());
        Manager.ITEM_OPTION_TEMPLATES.get(73).id = 73;
    }

    @Test void walksToDragonBallsEquipmentAndMaterialsInsteadOfOnlyNearbyMoney() {
        ItemMap ball = drop(14, 12, 250, player.id);
        ItemMap equipment = drop(0, 0, 450, -1);
        ItemMap material = drop(220, 14, 600, 1_000_000_000 - player.id);
        drop(15, 12, 10, 99);
        assertSame(ball, loot.select(player, 1_000));
        zone.removeItemMap(ball);
        assertSame(equipment, loot.select(player, 1_100));
        zone.removeItemMap(equipment);
        assertSame(material, loot.select(player, 1_200));
    }

    @Test void fullBagStillCollectsMoneyAndStackableBallsWithoutChangingTheBagDuringSelection() {
        ItemMap ball = drop(14, 12, 10, player.id);
        ball.itemTemplate.isUpToUp = true;
        Item stored = new Item();
        stored.template = ball.itemTemplate;
        stored.quantity = 2;
        stored.itemOptions.add(new Item.ItemOption(73, 0));
        player.inventory.itemsBag.set(0, stored);
        assertSame(ball, loot.select(player, 1_000));
        assertEquals(2, stored.quantity);
        assertTrue(ball.options.isEmpty());
        zone.removeItemMap(ball);
        ItemMap equipment = drop(0, 0, 5, player.id);
        ItemMap gold = drop(76, 9, 20, player.id);
        assertSame(gold, loot.select(player, 1_100));
        zone.removeItemMap(gold);
        assertNull(loot.select(player, 1_200));
        player.inventory.itemsBag.add(new Item());
        assertSame(equipment, loot.select(player, 1_300));
    }

    @Test void skipsAStackThatCannotFitCompletelySoFailedPickupCannotPartiallyAddIt() {
        ItemMap ball = drop(14, 12, 10, player.id);
        ball.quantity = 2;
        ball.itemTemplate.isUpToUp = true;
        Item stored = new Item();
        stored.template = ball.itemTemplate;
        stored.quantity = 99_998;
        stored.itemOptions.add(new Item.ItemOption(73, 0));
        player.inventory.itemsBag.set(0, stored);
        assertNull(loot.select(player, 1_000));
        assertEquals(99_998, stored.quantity);
        assertEquals(2, ball.quantity);
    }

    @Test void skipsPersistentQuestObjectsAndDeployedSatellitesAndBacksOffFailedPickups() {
        drop(74, 7, 5, -1);
        drop(78, 7, 6, -1);
        drop(342, 22, 7, -1);
        ItemMap ball = drop(14, 12, 20, -1);
        ItemMap material = drop(220, 14, 40, -1);
        assertSame(ball, loot.select(player, 1_000));
        loot.defer(ball, 1_000);
        assertSame(material, loot.select(player, 1_100));
        zone.removeItemMap(material);
        assertNull(loot.select(player, 2_000));
        assertSame(ball, loot.select(player, 16_001));
    }

    @Test void abandonsUnreachableDropsAndDropsTakenByAnotherPlayer() {
        ItemMap stuck = drop(14, 12, 400, -1);
        ItemMap reachable = drop(220, 14, 500, -1);
        assertSame(stuck, loot.select(player, 1_000));
        assertSame(stuck, loot.select(player, 8_999));
        assertSame(reachable, loot.select(player, 9_000));
        zone.removeItemMap(reachable);
        assertNull(loot.select(player, 9_001));
        assertSame(stuck, loot.select(player, 24_001));
        stuck.playerId = 99;
        assertNull(loot.select(player, 24_002));
    }

    private ItemMap drop(int id, int type, int x, long owner) {
        var template = new ItemTemplate();
        template.id = (short) id;
        template.type = (byte) type;
        return new ItemMap(zone, template, 1, x, 0, owner);
    }
}
