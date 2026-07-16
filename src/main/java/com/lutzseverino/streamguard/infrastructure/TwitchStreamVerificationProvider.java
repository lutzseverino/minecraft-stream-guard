package com.lutzseverino.streamguard.infrastructure;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lutzseverino.streamguard.application.LiveStreamMetadata;
import com.lutzseverino.streamguard.application.StreamMetadataProvider;
import com.lutzseverino.streamguard.application.StreamVerificationProvider;
import com.lutzseverino.streamguard.application.VerificationResult;
import com.lutzseverino.streamguard.domain.StreamLink;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("UastIncorrectHttpHeaderInspection")
public final class TwitchStreamVerificationProvider
    implements StreamVerificationProvider, StreamMetadataProvider {

  private static final URI TOKEN_URI = URI.create("https://id.twitch.tv/oauth2/token");
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
  private static final int MAX_STREAMS_PER_REQUEST = 100;

  private final boolean enabled;
  private final String clientId;
  private final String clientSecret;
  private final HttpClient httpClient;
  private final Logger logger;
  private String accessToken = "";
  private Instant tokenExpiresAt = Instant.EPOCH;

  public TwitchStreamVerificationProvider(boolean enabled, String clientId, String clientSecret) {
    this(
        enabled,
        clientId,
        clientSecret,
        Logger.getLogger(TwitchStreamVerificationProvider.class.getName()));
  }

  public TwitchStreamVerificationProvider(
      boolean enabled, String clientId, String clientSecret, Logger logger) {
    this.enabled = enabled;
    this.clientId = clientId == null ? "" : clientId;
    this.clientSecret = clientSecret == null ? "" : clientSecret;
    this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    this.logger = logger;
  }

  @Override
  public VerificationResult verify(StreamLink link) {
    return verifyAll(List.of(link)).get(link);
  }

  @Override
  public Map<StreamLink, VerificationResult> verifyAll(Collection<StreamLink> links) {
    if (!enabled) {
      return offlineResults(links, "Twitch verification is disabled.");
    }
    if (clientId.isBlank() || clientSecret.isBlank()) {
      return offlineResults(links, "Twitch credentials are missing.");
    }
    try {
      Map<String, JsonObject> streamsByLogin = liveStreamsByLogin(links);
      Map<StreamLink, VerificationResult> results = new LinkedHashMap<>();
      for (StreamLink link : links) {
        results.put(
            link,
            streamsByLogin.containsKey(normalizeLogin(link.channel()))
                ? VerificationResult.live(link.providerId(), "Twitch channel is live.")
                : VerificationResult.offline(link.providerId(), "Twitch channel is not live."));
      }
      return Map.copyOf(results);
    } catch (IOException exception) {
      return unavailableResults(links, "Twitch verification failed: " + exception.getMessage());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return unavailableResults(links, "Twitch verification was interrupted.");
    } catch (RuntimeException exception) {
      return unavailableResults(links, "Twitch response could not be read.");
    }
  }

  @Override
  public Optional<LiveStreamMetadata> metadata(StreamLink link) {
    if (!enabled || clientId.isBlank() || clientSecret.isBlank()) {
      return Optional.empty();
    }
    try {
      return liveStream(link).map(stream -> metadataFromStream(link, stream));
    } catch (IOException | RuntimeException exception) {
      logger.log(
          Level.WARNING, "Twitch metadata lookup failed for " + link.channel() + ".", exception);
      return Optional.empty();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }

  private Optional<JsonObject> liveStream(StreamLink link)
      throws IOException, InterruptedException {
    return Optional.ofNullable(
        liveStreamsByLogin(List.of(link)).get(normalizeLogin(link.channel())));
  }

  private Map<String, JsonObject> liveStreamsByLogin(Collection<StreamLink> links)
      throws IOException, InterruptedException {
    Map<String, String> uniqueLogins = new LinkedHashMap<>();
    for (StreamLink link : links) {
      String login = normalizeLogin(link.channel());
      if (!login.isBlank()) {
        uniqueLogins.putIfAbsent(login, login);
      }
    }
    if (uniqueLogins.isEmpty()) {
      return Map.of();
    }

    String token = accessToken();
    Map<String, JsonObject> streamsByLogin = new LinkedHashMap<>();
    List<String> logins = List.copyOf(uniqueLogins.keySet());
    for (int index = 0; index < logins.size(); index += MAX_STREAMS_PER_REQUEST) {
      List<String> chunk =
          logins.subList(index, Math.min(index + MAX_STREAMS_PER_REQUEST, logins.size()));
      JsonObject body = fetchStreams(token, chunk);
      if (!body.has("data") || body.getAsJsonArray("data").isEmpty()) {
        continue;
      }
      for (int streamIndex = 0; streamIndex < body.getAsJsonArray("data").size(); streamIndex++) {
        JsonObject stream = body.getAsJsonArray("data").get(streamIndex).getAsJsonObject();
        string(stream, "user_login")
            .map(TwitchStreamVerificationProvider::normalizeLogin)
            .ifPresent(login -> streamsByLogin.put(login, stream));
      }
    }
    return streamsByLogin;
  }

  private JsonObject fetchStreams(String token, List<String> logins)
      throws IOException, InterruptedException {
    StringBuilder query = new StringBuilder("type=live&first=").append(logins.size());
    for (String login : logins) {
      query.append("&user_login=").append(encode(login));
    }
    URI uri = URI.create("https://api.twitch.tv/helix/streams?" + query);
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("Client-Id", clientId)
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("Twitch returned HTTP " + response.statusCode());
    }
    return JsonParser.parseString(response.body()).getAsJsonObject();
  }

  private synchronized String accessToken() throws IOException, InterruptedException {
    if (!accessToken.isBlank() && tokenExpiresAt.isAfter(Instant.now().plusSeconds(60))) {
      return accessToken;
    }
    String body =
        "client_id="
            + encode(clientId)
            + "&client_secret="
            + encode(clientSecret)
            + "&grant_type=client_credentials";
    HttpRequest request =
        HttpRequest.newBuilder(TOKEN_URI)
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("token request returned HTTP " + response.statusCode());
    }
    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
    accessToken = json.get("access_token").getAsString();
    int expiresIn = json.has("expires_in") ? json.get("expires_in").getAsInt() : 3600;
    tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn));
    return accessToken;
  }

  static LiveStreamMetadata metadataFromStream(StreamLink link, JsonObject stream) {
    String channel = string(stream, "user_login").orElse(link.channel());
    return new LiveStreamMetadata(
        channel,
        string(stream, "title").orElse(null),
        string(stream, "thumbnail_url")
            .map(TwitchStreamVerificationProvider::thumbnailUrl)
            .orElse(null),
        integer(stream, "viewer_count").orElse(null),
        instant(stream, "started_at").orElse(null),
        "https://twitch.tv/" + channel);
  }

  private static String thumbnailUrl(String template) {
    return template.replace("{width}", "1280").replace("{height}", "720");
  }

  private static Optional<String> string(JsonObject object, String name) {
    if (!object.has(name) || object.get(name).isJsonNull()) {
      return Optional.empty();
    }
    String value = object.get(name).getAsString();
    return value.isBlank() ? Optional.empty() : Optional.of(value);
  }

  private static Optional<Integer> integer(JsonObject object, String name) {
    if (!object.has(name) || object.get(name).isJsonNull()) {
      return Optional.empty();
    }
    return Optional.of(object.get(name).getAsInt());
  }

  private static Optional<Instant> instant(JsonObject object, String name) {
    try {
      return string(object, name).map(Instant::parse);
    } catch (DateTimeParseException exception) {
      return Optional.empty();
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static String normalizeLogin(String value) {
    return value.trim().toLowerCase(Locale.ROOT);
  }

  private static Map<StreamLink, VerificationResult> offlineResults(
      Collection<StreamLink> links, String detail) {
    Map<StreamLink, VerificationResult> results = new LinkedHashMap<>();
    for (StreamLink link : links) {
      results.put(link, VerificationResult.offline(link.providerId(), detail));
    }
    return Map.copyOf(results);
  }

  private static Map<StreamLink, VerificationResult> unavailableResults(
      Collection<StreamLink> links, String detail) {
    Map<StreamLink, VerificationResult> results = new LinkedHashMap<>();
    for (StreamLink link : links) {
      results.put(link, VerificationResult.unavailable(link.providerId(), detail));
    }
    return Map.copyOf(results);
  }
}
