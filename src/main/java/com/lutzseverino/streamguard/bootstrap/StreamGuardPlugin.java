package com.lutzseverino.streamguard.bootstrap;

import com.lutzseverino.streamguard.application.AccessService;
import com.lutzseverino.streamguard.application.BypassService;
import com.lutzseverino.streamguard.application.CachingStreamMetadataProvider;
import com.lutzseverino.streamguard.application.LiveFeedService;
import com.lutzseverino.streamguard.application.SessionRegistry;
import com.lutzseverino.streamguard.application.StreamService;
import com.lutzseverino.streamguard.application.StreamProviderRegistry;
import com.lutzseverino.streamguard.application.StreamProviderRegistration;
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
import java.util.Objects;
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
        saveResource("lang/en_US.yml", false);
        saveResource("lang/es_ES.yml", false);
        repository = new YamlPlayerAccessRepository(new File(getDataFolder(), "data.yml"), getLogger());
        sessionRegistry = new SessionRegistry(clock);
        wire();
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
        repository.reload();
        wire();
    }

    private void wire() {
        if (onboardingFlow != null) {
            onboardingFlow.shutdown();
            onboardingFlow = null;
        }
        if (liveFeedServer != null) {
            liveFeedServer.stop();
            liveFeedServer = null;
        }
        HandlerList.unregisterAll(this);
        StreamGuardSettings settings = StreamGuardSettings.load(new BukkitSettingsReader(getConfig()));
        MessageService messages = new MessageService(
                new File(getDataFolder(), "lang"),
                settings.language().defaultLocale(),
                settings.language().fallbackLocale()
        );
        StreamProviderRegistry verificationProvider = verificationProvider(settings);
        StreamGuardPolicy policy = new StreamGuardPolicy(
                settings.enforcement().guardedActions(),
                settings.enforcement().gracePeriod()
        );
        AccessService accessService = new AccessService(repository, sessionRegistry, policy, clock);
        BypassService bypassService = new BypassService(repository, clock);
        StreamService streamService = new StreamService(repository, verificationProvider, verificationProvider, clock);
        CachingStreamMetadataProvider liveFeedMetadataProvider = new CachingStreamMetadataProvider(
                verificationProvider,
                clock,
                Duration.ofSeconds(settings.web().liveFeed().metadataCacheSeconds())
        );
        LiveFeedService liveFeedService = new LiveFeedService(repository, liveFeedMetadataProvider, clock);
        BukkitStreamVerificationRunner verificationRunner = new BukkitStreamVerificationRunner(this, streamService, messages);
        onboardingFlow = new BukkitOnboardingFlow(
                this,
                streamService,
                verificationProvider,
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
                verificationProvider,
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
        recheckTask = getServer().getScheduler().runTaskTimer(this, () -> {
            List<OnlinePlayerSnapshot> players = getServer().getOnlinePlayers().stream()
                    .map(player -> new OnlinePlayerSnapshot(player.getUniqueId(), player.getName()))
                    .toList();
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                for (OnlinePlayerSnapshot player : players) {
                    if (streamService.status(player.playerId(), player.playerName()).streamLinkOptional().isPresent()) {
                        streamService.verify(player.playerId(), player.playerName());
                    }
                }
            });
        }, intervalTicks, intervalTicks);
    }

    private StreamProviderRegistry verificationProvider(StreamGuardSettings settings) {
        List<StreamProviderRegistration> providers = new ArrayList<>();
        StreamGuardSettings.ProviderSettings twitch = settings.providers().get(StreamProviderId.TWITCH);
        TwitchStreamVerificationProvider twitchProvider = new TwitchStreamVerificationProvider(
                twitch.enabled(),
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
        StreamGuardSettings.ProviderSettings youtube = settings.providers().get(StreamProviderId.YOUTUBE);
        YouTubeStreamVerificationProvider youtubeProvider = new YouTubeStreamVerificationProvider(
                youtube.enabled(),
                youtube.option("api-key"),
                getLogger()
        );
        providers.add(new StreamProviderRegistration(
                StreamProviderId.YOUTUBE,
                youtubeProvider,
                youtubeProvider,
                String::trim
        ));
        return new StreamProviderRegistry(providers);
    }

    private static String normalizeTwitchLogin(String value) {
        String normalized = value.trim();
        return normalized.startsWith("@") ? normalized.substring(1) : normalized;
    }

    private record OnlinePlayerSnapshot(java.util.UUID playerId, String playerName) {
    }
}
