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

    @Test
    void loadsConfigurableOnboardingProviderButtons() {
        MapSettingsReader reader = new MapSettingsReader(Map.ofEntries(
                Map.entry("onboarding.enabled", "true"),
                Map.entry("onboarding.provider-picker.title", "<gold>Pick</gold>"),
                Map.entry("onboarding.provider-picker.rows", "4"),
                Map.entry("onboarding.provider-picker.providers.twitch.enabled", "true"),
                Map.entry("onboarding.provider-picker.providers.twitch.slot", "13"),
                Map.entry("onboarding.provider-picker.providers.twitch.material", "PURPLE_WOOL"),
                Map.entry("onboarding.provider-picker.providers.twitch.name", "<light_purple>Twitch</light_purple>"),
                Map.entry("onboarding.provider-picker.providers.twitch.input-hint", "Twitch name"),
                Map.entry("onboarding.chat-input.timeout-seconds", "90"),
                Map.entry("onboarding.chat-input.max-length", "80"),
                Map.entry("onboarding.chat-input.cancel-keyword", "stop"),
                Map.entry("onboarding.chat-input.verify-after-link", "false")
        ));

        StreamGuardSettings settings = StreamGuardSettings.load(reader);

        assertTrue(settings.onboarding().enabled());
        assertEquals("<gold>Pick</gold>", settings.onboarding().providerPicker().title());
        assertEquals(4, settings.onboarding().providerPicker().rows());
        assertEquals(1, settings.onboarding().providerPicker().providers().size());
        StreamGuardSettings.ProviderButton button = settings.onboarding().providerPicker().providers().get(0);
        assertEquals(StreamProviderId.TWITCH, button.providerId());
        assertEquals(13, button.slot());
        assertEquals("PURPLE_WOOL", button.item().material());
        assertEquals("Twitch name", button.inputHint());
        assertEquals(90, settings.onboarding().chatInput().timeoutSeconds());
        assertEquals(80, settings.onboarding().chatInput().maxLength());
        assertEquals("stop", settings.onboarding().chatInput().cancelKeyword());
        assertFalse(settings.onboarding().chatInput().verifyAfterLink());
    }

    @Test
    void loadsLiveFeedWebSettings() {
        MapSettingsReader reader = new MapSettingsReader(Map.of(
                "web.live-feed.enabled", "true",
                "web.live-feed.bind-host", "0.0.0.0",
                "web.live-feed.port", "9000",
                "web.live-feed.path", "streams/live",
                "web.live-feed.update-interval-seconds", "20"
        ));

        StreamGuardSettings settings = StreamGuardSettings.load(reader);

        assertTrue(settings.web().liveFeed().enabled());
        assertEquals("0.0.0.0", settings.web().liveFeed().bindHost());
        assertEquals(9000, settings.web().liveFeed().port());
        assertEquals("/streams/live", settings.web().liveFeed().path());
        assertEquals(20, settings.web().liveFeed().updateIntervalSeconds());
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
