package io.github.chi2l3s.nextlib.internal.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Scanner;

public class UpdateChecker {
    private final JavaPlugin plugin;
    private final String repo;

    public UpdateChecker(JavaPlugin plugin, String repo) {
        this.plugin = plugin;
        this.repo = repo;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.github.com/repos/" + repo + "/releases/latest");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                if(conn.getResponseCode() != 200) {
                    plugin.getLogger().warning("Не удалось получить данные об обновлении (код " + conn.getResponseCode() + ")");
                    return;
                }

                StringBuilder json = new StringBuilder();
                try (Scanner scanner = new Scanner(new InputStreamReader(conn.getInputStream()))) {
                    while (scanner.hasNext()) {
                        json.append(scanner.nextLine());
                    }
                }

                JSONParser parser = new JSONParser();
                JSONObject release = (JSONObject) parser.parse(json.toString());

                String latest = ((String) release.get("tag_name")).replace("v", "");
                String current = plugin.getDescription().getVersion();

                if (!latest.equalsIgnoreCase(current)) {
                    plugin.getLogger().warning("=============================================");
                    plugin.getLogger().warning("⚠ Доступна новая версия NextLib: v" + latest);
                    plugin.getLogger().warning("➡ Ваша версия: v" + current);
                    plugin.getLogger().warning("🔗 Скачайте обновление: " + release.get("html_url"));
                    plugin.getLogger().warning("=============================================");
                } else {
                    plugin.getLogger().info("✅ Вы используете последнюю версия NextLib (v" + current + ")");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось проверить обновления: " + e.getMessage());
            }
        });
    }
}
