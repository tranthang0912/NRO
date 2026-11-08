package database;
import item.Item;
import shop.*;
import services.ItemService;
import java.sql.*;
import java.util.*;

/** Four bulk reads instead of one remote query for each item and option. */
public final class ShopDAO {
    public static List<Shop> getShops(Connection con) {
        Map<Integer,Shop> shops=new LinkedHashMap<>();
        Map<Integer,List<TabShop>> tabs=new HashMap<>();
        Map<Integer,List<Item.ItemOption>> options=new HashMap<>();
        try(Statement s=con.createStatement()) {
            try(ResultSet r=s.executeQuery("SELECT * FROM shop ORDER BY npc_id,id")) {
                while(r.next()) {Shop shop=new Shop();shop.id=r.getInt("id");shop.npcId=r.getByte("npc_id");shop.tagName=r.getString("tag_name");shop.typeShop=r.getByte("type_shop");shops.put(shop.id,shop);}
            }
            try(ResultSet r=s.executeQuery("SELECT * FROM tab_shop ORDER BY id")) {
                while(r.next()) {
                    Shop shop=shops.get(r.getInt("shop_id"));if(shop==null)continue;
                    TabShop tab=new TabShop();tab.shop=shop;tab.id=r.getInt("id");tab.name=r.getString("name").replace("<>","\n");
                    shop.tabShops.add(tab);int source=tab.id>=41&&tab.id<=43?tab.id-31:tab.id;
                    tabs.computeIfAbsent(source,k->new ArrayList<>()).add(tab);
                }
            }
            try(ResultSet r=s.executeQuery("SELECT * FROM item_shop_option ORDER BY id")) {
                while(r.next())options.computeIfAbsent(r.getInt("item_shop_id"),k->new ArrayList<>()).add(new Item.ItemOption(r.getInt("option_id"),r.getInt("param")));
            }
            try(ResultSet r=s.executeQuery("SELECT * FROM item_shop WHERE is_sell=1 ORDER BY create_time DESC,id")) {
                while(r.next())for(TabShop tab:tabs.getOrDefault(r.getInt("tab_id"),List.of())) {
                    ItemShop i=new ItemShop();i.tabShop=tab;i.id=r.getInt("id");i.temp=ItemService.gI().getTemplate(r.getShort("temp_id"));
                    if(i.temp==null)throw new IllegalStateException("Unknown shop item template "+r.getInt("temp_id"));
                    i.isNew=r.getBoolean("is_new");i.cost=r.getInt("cost");i.iconSpec=r.getInt("icon_spec");i.typeSell=r.getByte("type_sell");
                    for(var o:options.getOrDefault(i.id,List.of()))i.options.add(new Item.ItemOption(o));
                    tab.itemShops.add(i);
                }
            }
            return new ArrayList<>(shops.values());
        }catch(Exception e){throw new IllegalStateException("Cannot load game shops",e);}
    }
}
