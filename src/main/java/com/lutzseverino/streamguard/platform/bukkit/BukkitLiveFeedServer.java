package com.lutzseverino.streamguard.platform.bukkit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.lutzseverino.streamguard.application.LiveFeedPlayer;
import com.lutzseverino.streamguard.application.LiveFeedService;
import com.lutzseverino.streamguard.config.StreamGuardSettings;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class BukkitLiveFeedServer {

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapter(
              Instant.class,
              (com.google.gson.JsonSerializer<Instant>)
                  (instant, type, context) -> new JsonPrimitive(instant.toString()))
          .create();

  private final JavaPlugin plugin;
  private final LiveFeedService liveFeedService;
  private final StreamGuardSettings.LiveFeed settings;
  private HttpServer server;
  private ExecutorService executor;
  private BukkitTask refreshTask;
  private final AtomicBoolean refreshInProgress = new AtomicBoolean();
  private volatile boolean running;
  private volatile String cachedJson = GSON.toJson(new CachedFeed(List.of()));

  public BukkitLiveFeedServer(
      JavaPlugin plugin, LiveFeedService liveFeedService, StreamGuardSettings.LiveFeed settings) {
    this.plugin = plugin;
    this.liveFeedService = liveFeedService;
    this.settings = settings;
  }

  public void start() {
    if (!settings.enabled()) {
      return;
    }
    try {
      server = HttpServer.create(new InetSocketAddress(settings.bindHost(), settings.port()), 0);
      server.createContext(settings.path(), this::handle);
      executor =
          Executors.newSingleThreadExecutor(
              runnable -> {
                Thread thread = new Thread(runnable, "StreamGuard live-feed HTTP");
                thread.setDaemon(true);
                return thread;
              });
      server.setExecutor(executor);
      server.start();
      running = true;
      scheduleRefresh();
      plugin
          .getLogger()
          .info(
              "StreamGuard live feed listening on http://"
                  + settings.bindHost()
                  + ":"
                  + settings.port()
                  + settings.path());
    } catch (IOException exception) {
      plugin
          .getLogger()
          .log(Level.WARNING, "Could not start StreamGuard live feed API.", exception);
      stop();
    }
  }

  public void stop() {
    if (refreshTask != null) {
      refreshTask.cancel();
      refreshTask = null;
    }
    running = false;
    if (server != null) {
      server.stop(0);
      server = null;
    }
    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }
  }

  private void scheduleRefresh() {
    refresh();
    long intervalTicks = Math.max(20L, settings.updateIntervalSeconds() * 20L);
    refreshTask =
        plugin
            .getServer()
            .getScheduler()
            .runTaskTimer(plugin, this::refresh, intervalTicks, intervalTicks);
  }

  private void refresh() {
    if (!running) {
      return;
    }
    List<LiveFeedPlayer> players =
        plugin.getServer().getOnlinePlayers().stream()
            .map(player -> new LiveFeedPlayer(player.getUniqueId(), player.getName()))
            .toList();
    if (!refreshInProgress.compareAndSet(false, true)) {
      return;
    }
    plugin
        .getServer()
        .getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              try {
                String nextJson = GSON.toJson(liveFeedService.snapshot(players));
                if (running) {
                  cachedJson = nextJson;
                }
              } finally {
                refreshInProgress.set(false);
              }
            });
  }

  private void handle(HttpExchange exchange) throws IOException {
    try {
      addCorsHeaders(exchange);
      if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(204, -1);
        return;
      }
      if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        byte[] body = "Method not allowed".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(405, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
          output.write(body);
        }
        return;
      }
      byte[] body = cachedJson.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
      exchange.getResponseHeaders().set("Cache-Control", "no-store");
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream output = exchange.getResponseBody()) {
        output.write(body);
      }
    } finally {
      exchange.close();
    }
  }

  private void addCorsHeaders(HttpExchange exchange) {
    List<String> allowedOrigins = settings.corsAllowedOrigins();
    String origin = exchange.getRequestHeaders().getFirst("Origin");
    if (origin == null || !allowedOrigins.contains(origin)) {
      return;
    }
    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
    exchange.getResponseHeaders().set("Vary", "Origin");
    exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
    exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Accept, Content-Type");
  }

  private record CachedFeed(List<Object> streamers) {}
}
