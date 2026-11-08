package database;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DatabaseBootstrapTest {
    @Test void semicolonsQuotesAndCommentsInsideGameText() {
        var statements=DatabaseBootstrap.splitStatements("-- migration\nINSERT INTO x VALUES ('a;b', 'it''s -- text'); SELECT 1;");
        assertEquals(2,statements.size());assertTrue(statements.get(0).contains("a;b"));
        assertThrows(IllegalArgumentException.class,()->DatabaseBootstrap.splitStatements("SELECT 'oops"));
    }
}
