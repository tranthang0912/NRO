// package boss.Chilled;

// import boss.BossID;
// import boss.Boss;
// import boss.BossesData;
// import item.Item;
// import java.util.List;
// import map.ItemMap;
// import player.Player;
// import services.EffectSkillService;
// import services.ItemService;
// import services.Service;
// import services.TaskService;
// import utils.Util;

// public class Chilled extends Boss {

//     private long st;

//     public Chilled() throws Exception {
//         super(BossID.CHILLED, BossesData.CHILLED, BossesData.CHILLED_2);
//     }

//     @Override
//     public void reward(Player plKill) {
//         int x = this.location.x;
//         int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
//         int drop = 190;
//         int quantity = Util.nextInt(20000, 30000);
//         if (Util.isTrue(5 , 100)) {
//         ItemMap it = ItemService.gI().randDoTLBoss(this.zone, 1, x, y, plKill.id);
//         if (it != null) {
//         Service.gI().dropItemMap(zone, it);
//         }
//         }
//         ItemMap itemMap = new ItemMap(this.zone, drop, quantity, x, y, plKill.id);
//         Item item = ItemService.gI().createNewItem((short) drop);
//         Service.gI().dropItemMap(zone, itemMap);
//         if (Util.isTrue(5, 100)) {
//             int group = Util.nextInt(1, 100) <= 70 ? 0 : 1;
//             int[][] drops = {
//                 {230, 231, 232, 234, 235, 236, 238, 239, 240, 242, 243, 244, 246, 247, 248, 250, 251, 252, 266, 267, 268, 270, 271, 272, 274, 275, 276},
//                 {254, 255, 256, 258, 259, 260, 262, 263, 264, 278, 279, 280}
//             };
//             int dropOptional = drops[group][Util.nextInt(0, drops[group].length - 1)];
//             ItemMap optionalItemMap = new ItemMap(this.zone, dropOptional, 1, x, y, plKill.id);
//             Item optionalItem = ItemService.gI().createNewItem((short) dropOptional);
//             List<Item.ItemOption> optionalOps = ItemService.gI().getListOptionItemShop((short) dropOptional);
//             optionalOps.forEach(option -> option.param = (int) (option.param * Util.nextInt(100, 115) / 100.0));
//             optionalItemMap.options.addAll(optionalOps);
//             int rand = Util.nextInt(1, 100);
//             int value = 0;
//             if (rand <= 80) {
//                 value = Util.nextInt(1, 3);
//             } else if (rand <= 97) {
//                 value = Util.nextInt(4, 5);
//             } else {
//                 value = 6;
//             }
//             optionalItemMap.options.add(new Item.ItemOption(107, value));
//             Service.gI().dropItemMap(zone, optionalItemMap);
//         }
//         if (Util.isTrue(80, 100)) {
//             int[] dropItems = {15,16,17,18,19,20};
//             int dropOptional = dropItems[Util.nextInt(0, dropItems.length - 1)];
//             ItemMap optionalItemMap = new ItemMap(this.zone, dropOptional, Util.nextInt(1, 3), x, y, plKill.id);
//             Item optionalItem = ItemService.gI().createNewItem((short) dropOptional);
//             Service.gI().dropItemMap(zone, optionalItemMap);
//         }
//     }

//     @Override
//     public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
//         if (!this.isDie()) {
//             if (Util.isTrue(10, 1000)) {
//                 this.chat("Xí hụt");
//                 return 0;
//             }
//             damage = this.nPoint.subDameInjureWithDeff(damage);
//             this.nPoint.subHP(damage);
//             if (isDie()) {
//                 this.setDie(plAtt);
//                 die(plAtt);
//             }
//             return (int) damage;
//         } else {
//             return 0;
//         }
//     }
//     @Override
//     public void joinMap() {
//         super.joinMap();
//         st = System.currentTimeMillis();
//     }

//     @Override
//     public void autoLeaveMap() {
//         if (Util.canDoWithTime(st, 900000)) {
//             this.leaveMapNew();
//         }
//         if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
//             st = System.currentTimeMillis();
//         }
//     }

// }
