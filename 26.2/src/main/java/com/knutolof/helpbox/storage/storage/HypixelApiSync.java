package com.knutolof.helpbox.storage.storage;
import com.knutolof.helpbox.storage.StorageInitializer;


import com.knutolof.helpbox.storage.config.SackConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HypixelApiSync {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "EnhancedStorage-HypixelApiSync");
        t.setDaemon(true);
        return t;
    });

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static boolean started = false;

    public static synchronized void start() {
        if (started) return;
        started = true;
        scheduler.scheduleAtFixedRate(HypixelApiSync::fetchAndUpdate, 5, 60, TimeUnit.SECONDS);
    }

    public static void fetchAndUpdate() {
        if (!SackConfig.enableHypixelApiSync) return;
        String apiKey = SackConfig.hypixelApiKey.trim();
        if (apiKey.isBlank()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getUser() == null) return;
        String uuid = client.getUser().getProfileId().toString().replace("-", "");

        String url = "https://api.hypixel.net/v2/skyblock/profiles?key=" + apiKey + "&uuid=" + uuid;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "EnhancedStorageMod/1.0")
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                            if (root.has("success") && root.get("success").getAsBoolean() && root.has("profiles")) {
                                JsonElement profilesElem = root.get("profiles");
                                if (profilesElem != null && profilesElem.isJsonArray()) {
                                    parseProfiles(profilesElem.getAsJsonArray(), uuid);
                                }
                            }
                        } catch (Exception e) {
                            StorageInitializer.LOGGER.error("Failed to parse Hypixel API response", e);
                        }
                    } else {
                        StorageInitializer.LOGGER.warn("Hypixel API response status: {}", response.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    StorageInitializer.LOGGER.error("Error fetching Hypixel API sacks_counts", ex);
                    return null;
                });
    }

    private static void parseProfiles(JsonArray profiles, String playerUuid) {
        for (JsonElement elem : profiles) {
            if (!elem.isJsonObject()) continue;
            JsonObject profile = elem.getAsJsonObject();
            boolean selected = profile.has("selected") && profile.get("selected").getAsBoolean();
            if (!selected) continue;

            if (profile.has("members")) {
                JsonObject members = profile.getAsJsonObject("members");
                for (Map.Entry<String, JsonElement> entry : members.entrySet()) {
                    if (entry.getKey().replace("-", "").equalsIgnoreCase(playerUuid)) {
                        JsonObject member = entry.getValue().getAsJsonObject();
                        if (member.has("sacks_counts")) {
                            JsonObject sacksCounts = member.getAsJsonObject("sacks_counts");
                            updateSackCountsFromApi(sacksCounts);
                        }
                    }
                }
            }
        }
    }

    private static void updateSackCountsFromApi(JsonObject sacksCounts) {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : sacksCounts.entrySet()) {
            try {
                counts.put(entry.getKey(), entry.getValue().getAsInt());
            } catch (Exception ignored) {
            }
        }

        if (!counts.isEmpty()) {
            SackCache.getInstance().updateFromApiCounts(counts);
        }
    }
}
