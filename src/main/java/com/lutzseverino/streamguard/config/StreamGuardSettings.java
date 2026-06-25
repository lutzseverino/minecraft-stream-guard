package com.lutzseverino.streamguard.config;

import com.lutzseverino.streamguard.domain.GuardedAction;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record StreamGuardSettings(
        Language language,
        Enforcement enforcement,
        CommandSafety commandSafety,
        Bypass bypass,
        Providers providers
) {

    public static StreamGuardSettings load(SettingsReader reader) {
        EnumSet<GuardedAction> guardedActions = EnumSet.noneOf(GuardedAction.class);
        for (GuardedAction action : GuardedAction.values()) {
            if (reader.bool("enforcement.blocked-actions." + action.configKey(), true)) {
                guardedActions.add(action);
            }
        }
        boolean defaultMovement = reader.bool("enforcement.allow-movement-while-unverified", true);
        boolean defaultChat = reader.bool("enforcement.allow-chat-while-unverified", true);
        return new StreamGuardSettings(
                new Language(
                        reader.string("language.default-locale", "en_US"),
                        reader.string("language.fallback-locale", "en_US"),
                        reader.bool("language.per-player-locale", false),
                        reader.bool("language.use-client-locale", false)
                ),
                new Enforcement(
                        Duration.ofSeconds(Math.max(0, reader.integer("enforcement.grace-period-seconds", 0))),
                        Duration.ofSeconds(Math.max(5, reader.integer("enforcement.recheck-interval-seconds", 60))),
                        defaultMovement,
                        defaultChat,
                        guardedActions,
                        StateRules.load(reader, "enforcement.unlinked", defaultMovement, defaultChat),
                        StateRules.load(reader, "enforcement.not-live", defaultMovement, defaultChat)
                ),
                new CommandSafety(reader.stringList("commands.safe-while-unverified")),
                new Bypass(
                        reader.bool("bypass.ops-bypass-by-default", true),
                        reader.bool("bypass.allow-temporary-bypass", true),
                        Math.max(0, reader.integer("bypass.max-temporary-bypass-minutes", 240)),
                        reader.bool("bypass.log-bypass-changes", true)
                ),
                Providers.load(reader)
        );
    }

    public record Language(
            String defaultLocale,
            String fallbackLocale,
            boolean perPlayerLocale,
            boolean useClientLocale
    ) {
    }

    public record Enforcement(
            Duration gracePeriod,
            Duration recheckInterval,
            boolean allowMovementWhileUnverified,
            boolean allowChatWhileUnverified,
            EnumSet<GuardedAction> guardedActions,
            StateRules unlinked,
            StateRules notLive
    ) {
        public Enforcement {
            guardedActions = guardedActions.isEmpty()
                    ? EnumSet.noneOf(GuardedAction.class)
                    : EnumSet.copyOf(guardedActions);
        }
    }

    public record StateRules(
            boolean kickOnJoin,
            int kickDelaySeconds,
            boolean allowMovement,
            boolean allowChat,
            boolean allowCommands
    ) {
        private static StateRules load(
                SettingsReader reader,
                String path,
                boolean defaultMovement,
                boolean defaultChat
        ) {
            return new StateRules(
                    reader.bool(path + ".kick-on-join", false),
                    Math.max(0, reader.integer(path + ".kick-delay-seconds", 0)),
                    reader.bool(path + ".allow-movement", defaultMovement),
                    reader.bool(path + ".allow-chat", defaultChat),
                    reader.bool(path + ".allow-commands", true)
            );
        }
    }

    public record CommandSafety(List<String> safeWhileUnverified) {
        public CommandSafety {
            safeWhileUnverified = List.copyOf(safeWhileUnverified);
        }
    }

    public record Bypass(
            boolean opsBypassByDefault,
            boolean allowTemporaryBypass,
            int maxTemporaryBypassMinutes,
            boolean logBypassChanges
    ) {
    }

    public record Providers(Map<StreamProviderId, ProviderSettings> all) {
        private static Providers load(SettingsReader reader) {
            Map<StreamProviderId, ProviderSettings> providers = new HashMap<>();
            for (String rawKey : reader.keys("providers")) {
                StreamProviderId.parse(rawKey).ifPresent(providerId -> {
                    String path = "providers." + rawKey;
                    Map<String, String> options = new HashMap<>();
                    for (String optionKey : reader.keys(path)) {
                        if (!"enabled".equals(optionKey)) {
                            options.put(optionKey, reader.string(path + "." + optionKey, ""));
                        }
                    }
                    providers.put(providerId, new ProviderSettings(
                            providerId,
                            reader.bool(path + ".enabled", false),
                            options
                    ));
                });
            }
            return new Providers(providers);
        }

        public Providers {
            all = Map.copyOf(all);
        }

        public ProviderSettings get(StreamProviderId providerId) {
            return all.getOrDefault(providerId, ProviderSettings.disabled(providerId));
        }
    }

    public record ProviderSettings(StreamProviderId providerId, boolean enabled, Map<String, String> options) {
        public ProviderSettings {
            options = Map.copyOf(options);
        }

        public static ProviderSettings disabled(StreamProviderId providerId) {
            return new ProviderSettings(providerId, false, Map.of());
        }

        public String option(String key) {
            return options.getOrDefault(key, "");
        }
    }
}
