package web;
import database.DatabaseManager;
public final class WebConfig {
    private WebConfig() {}
    public static final boolean ENABLED = true;
    public static final int MAX_PLAYERS = Math.max(1, Math.min(5, Integer.parseInt(DatabaseManager.env("MAX_PLAYERS", "5"))));
    public static final int START_MAP = 0;
    public static final int TICK_MS = 50;
}
