package data;
import javax.sql.rowset.*;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ResultSetImplTest {
    @Test void readsForwardOnlyNumbersAndFirstRowOfMultipleResults() throws Exception {
        RowSetMetaDataImpl meta=new RowSetMetaDataImpl();meta.setColumnCount(2);
        meta.setColumnName(1,"id");meta.setColumnLabel(1,"id");meta.setColumnType(1,Types.SMALLINT);meta.setTableName(1,"account");
        meta.setColumnName(2,"value");meta.setColumnLabel(2,"value");meta.setColumnType(2,Types.BIGINT);meta.setTableName(2,"account");
        try(CachedRowSet rs=RowSetProvider.newFactory().createCachedRowSet()) {
            rs.setMetaData(meta);
            for(int i=1;i<=2;i++){rs.moveToInsertRow();rs.updateShort(1,(short)i);rs.updateLong(2,9000000000L+i);rs.insertRow();rs.moveToCurrentRow();}
            rs.beforeFirst();var result=new ResultSetImpl(rs);
            assertEquals(2,result.getRows());assertTrue(result.first());
            assertEquals(result.getInt("id"),result.getShort(1));
            assertEquals(result.getInt("id"),result.getInt("account.ID"));
            assertTrue(result.getLong("value")>Integer.MAX_VALUE);
            assertTrue(result.next());assertFalse(result.next());
            assertTrue(result.first());assertTrue(result.getBoolean("id"));
        }
    }
}
