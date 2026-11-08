package database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import data.ResultSetImpl;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;

/** PostgreSQL only. Secrets come from the environment (or ignored local .env). */
public final class DatabaseManager {
    public static final String DB_NAME = "PostgreSQL";
    public static final boolean LOG_QUERY = false;
    private static final Properties LOCAL = localEnvironment();
    private static HikariDataSource ds;

    public static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value != null ? value : LOCAL.getProperty(name, fallback);
    }
    private static Properties localEnvironment() {
        Properties p = new Properties();
        try {
            if (Files.exists(Path.of(".env"))) {
                for (String line : Files.readAllLines(Path.of(".env"), StandardCharsets.UTF_8)) {
                    int equals = line.indexOf('=');
                    if (equals > 0 && !line.stripLeading().startsWith("#")) {
                        String value = line.substring(equals + 1).trim();
                        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) value = value.substring(1, value.length() - 1);
                        p.setProperty(line.substring(0, equals).trim(), value);
                    }
                }
            }
        } catch (Exception e) { throw new IllegalStateException("Cannot read local .env", e); }
        return p;
    }
    public static synchronized void initialize() throws Exception {
        if (ds != null) return;
        String url = env("DATABASE_URL", "");
        if (url.isBlank()) throw new IllegalArgumentException("Set DATABASE_URL (PostgreSQL) before starting.");
        URI uri = URI.create(url.replaceFirst("^jdbc:", ""));
        if (!uri.getScheme().matches("postgres|postgresql")) throw new IllegalArgumentException("DATABASE_URL must be PostgreSQL.");
        String schema = env("DB_SCHEMA", "nro_web");
        if (!schema.matches("[a-z][a-z0-9_]{0,40}")) throw new IllegalArgumentException("Invalid DB_SCHEMA");
        Properties props = new Properties();
        if (uri.getRawQuery() != null) for (String param : uri.getRawQuery().split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) props.setProperty(decode(pair[0]), decode(pair[1]));
        }
        if (uri.getRawUserInfo() != null) {
            String[] auth = uri.getRawUserInfo().split(":", 2);
            props.setProperty("user", decode(auth[0]));
            if (auth.length == 2) props.setProperty("password", decode(auth[1]));
        }
        String ca = env("DB_SSL_ROOT_CERT", "");
        if (!ca.isBlank()) {
            if (!Files.isRegularFile(Path.of(ca))) throw new IllegalArgumentException("DB_SSL_ROOT_CERT file does not exist.");
            props.setProperty("sslrootcert", Path.of(ca).toAbsolutePath().toString());
            props.setProperty("sslmode", "verify-full");
        } else props.putIfAbsent("sslmode", "require");
        props.setProperty("ApplicationName", "nro-web");
        props.setProperty("connectTimeout", "10");
        props.setProperty("socketTimeout", "30");
        props.setProperty("tcpKeepAlive", "true");
        String host = uri.getHost().contains(":") ? "[" + uri.getHost() + "]" : uri.getHost();
        String jdbc = "jdbc:postgresql://" + host + ":" + (uri.getPort() < 0 ? 5432 : uri.getPort()) + uri.getRawPath();
        try (Connection con = DriverManager.getConnection(jdbc, props)) { DatabaseBootstrap.migrate(con, schema); }
        props.setProperty("currentSchema", schema);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbc);
        config.setDataSourceProperties(props);
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(Math.max(1, Math.min(6, Integer.parseInt(env("DB_POOL_MAX", "3")))));
        config.setMaxLifetime(600000);
        config.setIdleTimeout(60000);
        config.setConnectionTimeout(10000);
        config.setPoolName("nro-web");
        ds = new HikariDataSource(config);
    }
    private static String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
    public static Connection getConnection() throws SQLException {
        if (ds == null) throw new SQLException("Database has not been initialized");
        return ds.getConnection();
    }
    public static synchronized void close() { if (ds != null) { ds.close(); ds = null; } }
    static String sql(String query) { return query.replace('`', '"'); }
    public static DatabaseResultSet executeQuery(String query, Object... values) throws Exception {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql(query))) {
            bind(ps, values);
            try (ResultSet rs = ps.executeQuery()) { return new ResultSetImpl(rs); }
        }
    }
    public static int executeUpdate(String query, Object... values) throws Exception {
        if (query.stripLeading().toLowerCase().startsWith("insert") && query.endsWith("()")) {
            query = query.substring(0, query.length() - 2) + "(" + String.join(",", java.util.Collections.nCopies(values.length, "?")) + ")";
        }
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql(query))) {
            bind(ps, values); return ps.executeUpdate();
        }
    }
    private static void bind(PreparedStatement ps, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value instanceof Boolean b) value = b ? 1 : 0;
            ps.setObject(i + 1, value);
        }
    }
}
