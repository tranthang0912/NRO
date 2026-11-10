package web;

import database.DatabaseManager;
import database.DatabaseResultSet;
import database.PlayerDAO;
import java.sql.*;
import java.util.Locale;
import java.util.concurrent.*;

/** Hashing runs outside the world loop. Database writes are parameterized. */
final class AuthService implements AutoCloseable {
    record Login(int accountId, String username, long playerId, long saveVersion, DatabaseResultSet account,
                 DatabaseResultSet player, DatabaseResultSet rank) {}
    private static final String DUMMY_HASH = dummyHash();

    private static String dummyHash() {
        try {
            return Passwords.hash("dummy-password");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize password verification", e);
        }
    }
    private final ExecutorService workers = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8), r -> { Thread t = new Thread(r, "authentication"); t.setDaemon(true); return t; });
    CompletableFuture<Login> authenticate(String user, String password, boolean register, String name, int gender, String invite) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String username = user.toLowerCase(Locale.ROOT).trim();
                if (!username.matches("[a-z0-9_]{3,20}") || password.length() < 6 || password.length() > 72) throw new IllegalArgumentException("Tài khoản 3–20 ký tự a-z, 0-9, _. Mật khẩu 6–72 ký tự.");
                if (register) {
                    String code = DatabaseManager.env("INVITE_CODE", "");
                    if (!code.isEmpty() && !java.security.MessageDigest.isEqual(code.getBytes(java.nio.charset.StandardCharsets.UTF_8), invite.getBytes(java.nio.charset.StandardCharsets.UTF_8))) throw new IllegalArgumentException("Mã nhóm không đúng.");
                    if (!name.matches("[a-zA-Z0-9]{5,10}") || gender < 0 || gender > 2) throw new IllegalArgumentException("Tên nhân vật gồm 5–10 chữ cái hoặc chữ số.");
                    String hash = Passwords.hash(password);
                    int id;
                    try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO account(username,password) VALUES (?,?) RETURNING id")) {
                        ps.setString(1, username); ps.setString(2, hash);
                        try (ResultSet rs = ps.executeQuery()) { rs.next(); id = rs.getInt(1); }
                    }
                    int hair = gender == 0 ? 64 : gender == 1 ? 9 : 6;
                    if (!PlayerDAO.createNewPlayer(id, name.toLowerCase(Locale.ROOT), (byte)gender, hair)) {
                        DatabaseManager.executeUpdate("DELETE FROM account WHERE id=?", id);
                        throw new IllegalArgumentException("Không tạo được nhân vật. Tên có thể đã được sử dụng.");
                    }
                    var account = DatabaseManager.executeQuery("SELECT * FROM account WHERE id = ?", id);
                    if (!account.first()) throw new IllegalStateException("Không tải được tài khoản vừa tạo.");
                    return loadLogin(username, account);
                }
                var account = DatabaseManager.executeQuery("SELECT * FROM account WHERE username = ?", username);
                boolean exists = account.first();
                boolean verified = Passwords.verify(password, exists ? account.getString("password") : DUMMY_HASH);
                if (exists && verified) {
                    if (account.getBoolean("ban")) throw new IllegalArgumentException("Tài khoản đã bị khóa.");
                    return loadLogin(username, account);
                }
                throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không đúng.");
            } catch (SQLException e) {
                if ("23505".equals(e.getSQLState())) throw new IllegalArgumentException("Tài khoản hoặc tên nhân vật đã tồn tại.");
                System.err.println("Authentication database failure: " + e.getSQLState());
                throw new IllegalStateException("Cơ sở dữ liệu chưa sẵn sàng, hãy thử lại.");
            } catch (IllegalArgumentException e) { throw e; }
            catch (Exception e) { throw new CompletionException(e); }
        }, workers);
    }

    private static Login loadLogin(String username, DatabaseResultSet account) throws Exception {
        int id = account.getInt("id");
        long version = WebPersistence.saveVersion();
        var player = DatabaseManager.executeQuery("SELECT * FROM player WHERE account_id = ? LIMIT 1", id);
        if (!player.first()) throw new IllegalArgumentException("Không tải được nhân vật.");
        long playerId = player.getLong("id");
        if (WebPersistence.playerSavePending(playerId)) throw new IllegalArgumentException("Đang lưu nhân vật. Hãy thử đăng nhập lại sau ít giây.");
        var rank = DatabaseManager.executeQuery("SELECT * FROM super_rank WHERE player_id = ?", playerId);
        return new Login(id, username, playerId, version, account, player, rank);
    }
    @Override public void close() { workers.shutdownNow(); }
}
