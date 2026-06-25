package com.lutzseverino.streamguard.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutzseverino.streamguard.domain.StreamProviderId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class StreamGuardSettingsTest {

    @Test
    void loadsArbitraryProviderOptionsWithoutTypedProviderRecords() {
        MapSettingsReader reader = new MapSettingsReader(Map.of(
                "providers.discord.enabled", "true",
                "providers.discord.guild-id", "123",
                "providers.discord.activity-name", "Minecraft",
                "providers.twitch.enabled", "false",
                "providers.twitch.client-id", "abc"
        ));

        StreamGuardSettings settings = StreamGuardSettings.load(reader);

        StreamGuardSettings.ProviderSettings discord = settings.providers().get(new StreamProviderId("discord"));
        assertTrue(discord.enabled());
        assertEquals("123", discord.option("guild-id"));
        assertEquals("Minecraft", discord.option("activity-name"));
        assertFalse(settings.providers().get(StreamProviderId.TWITCH).enabled());
        assertEquals("abc", settings.providers().get(StreamProviderId.TWITCH).option("client-id"));
    }

    private static final class MapSettingsReader implements SettingsReader {

        private final Map<String, String> values;

        private MapSettingsReader(Map<String, String> values) {
            this.values = new HashMap<>(values);
        }

        @Override
        public String string(String path, String fallback) {
            return values.getOrDefault(path, fallback);
        }

        @Override
        public boolean bool(String path, boolean fallback) {
            return values.containsKey(path) ? Boolean.parseBoolean(values.get(path)) : fallback;
        }

        @Override
        public int integer(String path, int fallback) {
            if (!values.containsKey(path)) {
                return fallback;
            }
            return Integer.parseInt(values.get(path));
        }

        @Override
        public List<String> stringList(String path) {
            return List.of();
        }

        @Override
        public Set<String> keys(String path) {
            String prefix = path + ".";
            return values.keySet().stream()
                    .filter(key -> key.startsWith(prefix))
                    .map(key -> key.substring(prefix.length()))
                    .map(key -> key.split("\\.", 2)[0])
                    .collect(Collectors.toCollection(() -> new java.util.TreeSet<>()));
        }
    }
}
