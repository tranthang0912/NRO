package database;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseBootstrap {
    private DatabaseBootstrap() {}
    public static void migrate(Connection c, String schema) throws Exception {
        c.setAutoCommit(false);
        try (Statement s = c.createStatement()) {
            s.execute("SELECT pg_advisory_xact_lock(724619032)");
            s.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
            s.execute("SET LOCAL search_path TO " + schema);
            s.execute("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY, installed_at TIMESTAMPTZ NOT NULL DEFAULT now())");
            String[] migrations={"V1__game.sql","V2__web_game.sql"};
            for(int i=0;i<migrations.length;i++){
                int version=i+1;
                try(ResultSet rs=s.executeQuery("SELECT 1 FROM schema_version WHERE version="+version)){if(rs.next())continue;}
                try (var input = DatabaseBootstrap.class.getResourceAsStream("/db/"+migrations[i])) {
                    if (input == null) throw new IllegalStateException("Missing bundled PostgreSQL migration");
                    for (String sql : splitStatements(new String(input.readAllBytes(), StandardCharsets.UTF_8))) s.execute(sql);
                }
                s.execute("INSERT INTO schema_version(version) VALUES ("+version+")");
            }
            c.commit();
            System.out.println("PostgreSQL game schema ready: " + schema);
        } catch (Exception e) { c.rollback(); throw e; }
        finally { c.setAutoCommit(true); }
    }
    static List<String> splitStatements(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!quote && c == '-' && i + 1 < input.length() && input.charAt(i + 1) == '-') {
                while (i < input.length() && input.charAt(i) != '\n') i++;
                current.append('\n'); continue;
            }
            if (c == '\'') {
                if (quote && i + 1 < input.length() && input.charAt(i + 1) == '\'') { current.append("''"); i++; continue; }
                quote = !quote;
            }
            if (c == ';' && !quote) {
                if (!current.toString().isBlank()) result.add(current.toString().trim());
                current.setLength(0);
            } else current.append(c);
        }
        if (quote) throw new IllegalArgumentException("Unclosed SQL literal");
        if (!current.toString().isBlank()) result.add(current.toString().trim());
        return result;
    }
}
