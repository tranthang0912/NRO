import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Uses five disposable characters on an isolated test service with registration enabled. */
public class AutomationTest {
    public static void main(String[] args) throws Exception {
        List<SmokeTest.Bot> bots = new ArrayList<>();
        String prefix = UUID.randomUUID().toString().substring(0, 7);
        boolean sharedZone = false;
        try {
            for (int i = 0; i < 5; i++) {
                var bot = new SmokeTest.Bot();
                bots.add(bot);
                bot.waitFor("hello", 5);
                String name = "at" + prefix + i;
                bot.send(Map.of("type", "register", "username", name, "password", "Test-" + UUID.randomUUID(),
                        "name", name, "gender", 0, "invite", ""));
                bot.waitFor("joined", 20);
                bot.send(Map.of("type", "automation", "mode", "quest"));
                assertMode(bot, "quest");
            }
            var complete = new boolean[bots.size()];
            int remaining = bots.size();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(155);
            while (remaining > 0 && System.nanoTime() < deadline) {
                for (int i = 0; i < bots.size(); i++) {
                    if (complete[i]) continue;
                    var bot = bots.get(i);
                    JsonObject event;
                    while ((event = bot.queue.poll()) != null) {
                        switch (event.get("type").getAsString()) {
                            case "state" -> sharedZone |= event.get("map").getAsInt() == 0
                                    && event.get("zone").getAsInt() == 0 && event.getAsJsonArray("players").size() == 5;
                            case "automation" -> {
                                if (event.get("mode").getAsString().equals("off")) throw new AssertionError(event);
                            }
                            case "task" -> {
                                if (event.getAsJsonObject("task").get("id").getAsInt() == 2) {
                                    bot.send(Map.of("type", "automation", "mode", "off"));
                                    assertMode(bot, "off");
                                    complete[i] = true;
                                    remaining--;
                                }
                            }
                        }
                        if (complete[i]) break;
                    }
                }
                Thread.sleep(100);
            }
            if (remaining != 0 || !sharedZone) throw new AssertionError("Five-player quest run incomplete: remaining=" + remaining);
            System.out.println("PASS: five characters shared Lang Aru and each completed their own starter and five-dummy quests.");
        } finally {
            for (var bot : bots) try { bot.close(); } catch (Exception ignored) {}
        }
    }

    private static void assertMode(SmokeTest.Bot bot, String expected) throws Exception {
        var event = bot.waitFor("automation", 5);
        if (!event.get("mode").getAsString().equals(expected)) throw new AssertionError(event);
    }
}
