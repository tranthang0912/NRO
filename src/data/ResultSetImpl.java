package data;
import database.DatabaseResultSet;
import java.sql.*;
import java.util.*;
/** Disconnected, forward-only JDBC materialization; driver-independent numeric conversion. */
public final class ResultSetImpl implements DatabaseResultSet {
 private final List<Map<String,Object>> rows = new ArrayList<>();
 private final List<Object[]> values = new ArrayList<>();
 private int cursor = -1;
 public ResultSetImpl(ResultSet rs) throws SQLException {
  var meta=rs.getMetaData(); int count=meta.getColumnCount();
  while(rs.next()) {
   Map<String,Object> row=new HashMap<>(); Object[] vals=new Object[count];
   for(int i=1;i<=count;i++) {
    Object v=rs.getObject(i); String name=meta.getColumnLabel(i).toLowerCase(Locale.ROOT);
    row.put(name,v); row.put(meta.getTableName(i).toLowerCase(Locale.ROOT)+"."+name,v); vals[i-1]=v;
   }
   rows.add(row); values.add(vals);
  }
 }
 public void dispose(){ rows.clear(); values.clear(); cursor=-1; }
 public boolean next(){return ++cursor<rows.size();}
 public boolean first(){cursor=0;return !rows.isEmpty();}
 public boolean gotoResult(int index){if(index<0||index>=rows.size())throw new IndexOutOfBoundsException();cursor=index;return true;}
 public boolean gotoFirst(){return first();}
 public void gotoBeforeFirst(){cursor=-1;}
 public boolean gotoLast(){cursor=rows.size()-1;return !rows.isEmpty();}
 public int getRows(){return rows.size();}
 public Object getObject(int col){return values.get(cursor)[col-1];}
 public Object getObject(String col){return rows.get(cursor).get(col.toLowerCase(Locale.ROOT));}
 private static Number number(Object value){if(value==null)return 0;if(value instanceof Number n)return n;if(value instanceof Boolean b)return b?1:0;return Double.valueOf(value.toString());}
 private static boolean bool(Object value){return value instanceof Boolean b?b:value instanceof Number n?n.intValue()!=0:value!=null&&(value.toString().equalsIgnoreCase("true")||value.toString().equals("1"));}
 public byte getByte(int col){return number(getObject(col)).byteValue();}
 public byte getByte(String col){return number(getObject(col)).byteValue();}
 public short getShort(int col){return number(getObject(col)).shortValue();}
 public short getShort(String col){return number(getObject(col)).shortValue();}
 public int getInt(int col){return number(getObject(col)).intValue();}
 public int getInt(String col){return number(getObject(col)).intValue();}
 public long getLong(int col){return number(getObject(col)).longValue();}
 public long getLong(String col){return number(getObject(col)).longValue();}
 public float getFloat(int col){return number(getObject(col)).floatValue();}
 public float getFloat(String col){return number(getObject(col)).floatValue();}
 public double getDouble(int col){return number(getObject(col)).doubleValue();}
 public double getDouble(String col){return number(getObject(col)).doubleValue();}
 public String getString(int col){return String.valueOf(getObject(col));}
 public String getString(String col){return String.valueOf(getObject(col));}
 public boolean getBoolean(int col){return bool(getObject(col));}
 public boolean getBoolean(String col){return bool(getObject(col));}
 public Timestamp getTimestamp(int col){return (Timestamp)getObject(col);}
 public Timestamp getTimestamp(String col){return (Timestamp)getObject(col);}
}
