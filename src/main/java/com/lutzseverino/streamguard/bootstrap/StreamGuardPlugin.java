package com.lutzseverino.streamguard.bootstrap;

import com.lutzseverino.streamguard.application.AccessService;
import com.lutzseverino.streamguard.application.BypassService;
import com.lutzseverino.streamguard.application.CachingStreamMetadataProvider;
import com.lutzseverino.streamguard.application.CachingStreamVerificationProvider;
import com.lutzseverino.streamguard.application.LiveFeedService;
import com.lutzseverino.streamguard.application.SessionRegistry;
import com.lutzseverino.streamguard.application.StreamService;
import com.lutzseverino.streamguard.application.StreamProviderRegistry;
import com.lutzseverino.streamguard.application.StreamProviderRegistration;
import com.lutzseverino.streamguard.application.StreamVerificationProvider;
import com.lutzseverino.streamguard.application.StreamVerificationTarget;
import com.lutzseverino.streamguard.config.StreamGuardSettings;
import com.lutzseverino.streamguard.domain.StreamGuardPolicy;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import com.lutzseverino.streamguard.i18n.MessageService;
import com.lutzseverino.streamguard.infrastructure.TwitchStreamVerificationProvider;
import com.lutzseverino.streamguard.infrastructure.YamlPlayerAccessRepository;
import com.lutzseverino.streamguard.infrastructure.YouTubeStreamVerificationProvider;
import com.lutzseverino.streamguard.platform.bukkit.BukkitLiveFeedServer;
import com.lutzseverino.streamguard.platform.bukkit.BukkitSettingsReader;
import com.lutzseverino.streamguard.platform.bukkit.BukkitOnboardingFlow;
import com.lutzseverino.streamguard.platform.bukkit.BukkitStreamVerificationRunner;
import com.lutzseverino.streamguard.platform.bukkit.StreamCommand;
import com.lutzseverino.streamguard.platform.bukkit.StreamGuardAdminCommand;
import com.lutzseverino.streamguard.platform.bukkit.StreamGuardListener;
import java.io.File;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Bukkit entrypoint and composition root for StreamGuard.
 */
public final class StreamGuardPlugin extends JavaPlugin {

    private final Clock clock = Clock.systemUTC();
    private SessionRegistry sessionRegistry;
    private YamlPlayerAccessRepository repository;
    private BukkitTask recheckTask;
    private BukkitOnboardingFlow onboardingFlow;
    private BukkitLiveFeedServer liveFeedServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveBundledResourceIfMissing("lang/en_US.yml");
        saveBundledResourceIfMissing("lang/es_ES.yml");
        repository = new YamlPlayerAccessRepository(new File(getDataFolder(), "data.yml"), getLogger());
        sessionRegistry = new SessionRegistry(clock);
        wire(loadSettings());
        getLogger().info("StreamGuard initialized.");
    }

    @Override
    public void onDisable() {
        if (recheckTask != null) {
            recheckTask.cancel();
            recheckTask = null;
        }
        if (onboardingFlow != null) {
            onboardingFlow.shutdown();
            onboardingFlow = null;
        }
        if (liveFeedServer != null) {
            liveFeedServer.stop();
            liveFeedServer = null;
        }
        getLogger().info("StreamGuard disabled.");
    }

    private void reloadStreamGuard() {
        reloadConfig();
        StreamGuardSettings settings = loadSettings();
        repository.reload();
        wire(settings);
    }

    private StreamGuardSettings loadSettings() {
        return StreamGuardSettings.load(new BukkitSettingsReader(getConfig()));
    }

    private void wire(StreamGuardSettings settings) {
        if (onboardingFlow != null) {
            onboardingFlow.shutdown();
            onboardingFlow = null;
        }
        if (liveFeedServer != null) {
            liveFeedServer.stop();
            liveFeedServer = null;
        }
        HandlerList.unregisterAll(this);
        MessageService messages = new MessageService(
                new File(getDataFolder(), "lang"),
                settings.language().defaultLocale(),
                settings.language().fallbackLocale()
        );
        StreamProviderRegistry providerRegistry = verificationProvider(settings);
        StreamVerificationProvider verificationProvider = new CachingStreamVerificationProvider(
                providerRegistry,
                clock,
                settings.verification().cache().liveTimeToLive(),
                settings.verification().cache().offlineTimeToLive()
        );
        StreamGuardPolicy policy = new StreamGuardPolicy(
                settings.enforcement().guardedActions(),
                settings.enforcement().gracePeriod(),
                settings.verification().maximumStatusAge()
        );
        AccessService accessService = new AccessService(repository, sessionRegistry, policy, clock);
        BypassService bypassService = new BypassService(repository, clock);
        StreamService streamService = new StreamService(repository, verificationProvider, providerRegistry, clock);
        CachingStreamMetadataProvider liveFeedMetadataProvider = new CachingStreamMetadataProvider(
                providerRegistry,
                clock,
                Duration.ofSeconds(settings.web().liveFeed().metadataCacheSeconds())
        );
        LiveFeedService liveFeedService = new LiveFeedService(
                repository,
                liveFeedMetadataProvider,
                clock,
                settings.verification().maximumStatusAge()
        );
        BukkitStreamVerificationRunner verificationRunner = new BukkitStreamVerificationRunner(this, streamService, messages);
        onboardingFlow = new BukkitOnboardingFlow(
                this,
                streamService,
                providerRegistry,
                verificationRunner,
                messages,
                settings.onboarding()
        );
        liveFeedServer = new BukkitLiveFeedServer(this, liveFeedService, settings.web().liveFeed());
        liveFeedServer.start();

        getServer().getPluginManager().registerEvents(
                new StreamGuardListener(accessService, sessionRegistry, messages, settings, this),
                this
        );
        getServer().getPluginManager().registerEvents(onboardingFlow, this);
        StreamCommand streamCommand = new StreamCommand(
                streamService,
                providerRegistry,
                onboardingFlow,
                verificationRunner,
                messages
        );
        Objects.requireNonNull(getCommand("stream"), "stream command is missing from plugin.yml")
                .setExecutor(streamCommand);
        Objects.requireNonNull(getCommand("stream")).setTabCompleter(streamCommand);

        StreamGuardAdminCommand adminCommand = new StreamGuardAdminCommand(
                bypassService,
                streamService,
                messages,
                settings,
                this::reloadStreamGuard
        );
        Objects.requireNonNull(getCommand("streamguard"), "streamguard command is missing from plugin.yml")
                .setExecutor(adminCommand);
        Objects.requireNonNull(getCommand("streamguard")).setTabCompleter(adminCommand);
        scheduleRechecks(settings, streamService);
    }

    private void scheduleRechecks(StreamGuardSettings settings, StreamService streamService) {
        if (recheckTask != null) {
            recheckTask.cancel();
            recheckTask = null;
        }
        long intervalTicks = Math.max(20L, settings.enforcement().recheckInterval().toSeconds() * 20L);
        AtomicBoolean recheckInProgress = new AtomicBoolean();
        recheckTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (!recheckInProgress.compareAndSet(false, true)) {
                return;
            }
            List<OnlinePlayerSnapshot> players = getServer().getOnlinePlayers().stream()
                    .map(player -> new OnlinePlayerSnapshot(player.getUniqueId(), player.getName()))
                    .toList();
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    List<StreamVerificationTarget> targets = players.stream()
                            .filter(player -> streamService.status(player.playerId(), player.playerName())
                                    .streamLinkOptional()
                                    .isPresent())
                            .map(player -> new StreamVerificationTarget(player.playerId(), player.playerName()))
                            .toList();
                    streamService.verifyAll(targets);
                } finally {
                    recheckInProgress.set(false);
                }
            });
        }, intervalTicks, intervalTicks);
    }

    private StreamProviderRegistry verificationProvider(StreamGuardSettings settings) {
        List<StreamProviderRegistration> providers = new ArrayList<>();
        StreamGuardSettings.ProviderSettings twitch = settings.providers().get(StreamProviderId.TWITCH);
        if (twitch.enabled() && credentialsPresent(twitch, "client-id", "client-secret")) {
            TwitchStreamVerificationProvider twitchProvider = new TwitchStreamVerificationProvider(
                    true,
                    twitch.option("client-id"),
                    twitch.option("client-secret"),
                    getLogger()
            );
            providers.add(new StreamProviderRegistration(
                    StreamProviderId.TWITCH,
                    twitchProvider,
                    twitchProvider,
                    StreamGuardPlugin::normalizeTwitchLogin
            ));
        } else if (twitch.enabled()) {
            getLogger().warning("Twitch is enabled but has incomplete credentials; it will not be linkable.");
        }
        StreamGuardSettings.ProviderSettings youtube = settings.providers().get(StreamProviderId.YOUTUBE);
        if (youtube.enabled() && credentialsPresent(youtube, "api-key")) {
            YouTubeStreamVerificationProvider youtubeProvider = new YouTubeStreamVerificationProvider(
                    true,
                    youtube.option("api-key"),
                    getLogger()
            );
            providers.add(new StreamProviderRegistration(
                    StreamProviderId.YOUTUBE,
                    youtubeProvider,
                    youtubeProvider,
                    String::trim
            ));
        } else if (youtube.enabled()) {
            getLogger().warning("YouTube is enabled but has no API key; it will not be linkable.");
        }
        return new StreamProviderRegistry(providers);
    }

    private static boolean credentialsPresent(
            StreamGuardSettings.ProviderSettings provider,
            String... keys
    ) {
        return java.util.Arrays.stream(keys).allMatch(key -> !provider.option(key).isBlank());
    }

    private static String normalizeTwitchLogin(String value) {
        String normalized = value.trim();
        normalized = normalized.startsWith("@") ? normalized.substring(1) : normalized;
        return normalized.toLowerCase(Locale.ROOT);
    }

    private void saveBundledResourceIfMissing(String resourcePath) {
        if (!new File(getDataFolder(), resourcePath).exists()) {
            saveResource(resourcePath, false);
        }
    }

    private record OnlinePlayerSnapshot(java.util.UUID playerId, String playerName) {
    }
}
