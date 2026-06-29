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
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class YouTubeStreamVerificationProvider implements StreamVerificationProvider, StreamMetadataProvider {

    private final boolean enabled;
    private final String apiKey;
    private final HttpClient httpClient;
    private final Logger logger;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    public YouTubeStreamVerificationProvider(boolean enabled, String apiKey) {
        this(enabled, apiKey, Logger.getLogger(YouTubeStreamVerificationProvider.class.getName()));
    }

    public YouTubeStreamVerificationProvider(boolean enabled, String apiKey, Logger logger) {
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        this.logger = logger;
    }

    @Override
    public VerificationResult verify(StreamLink link) {
        if (!enabled) {
            return VerificationResult.offline(link.providerId(), "YouTube verification is disabled.");
        }
        if (apiKey.isBlank()) {
            return VerificationResult.offline(link.providerId(), "YouTube API key is missing.");
        }
        try {
            return liveVideo(resolveChannelId(link.channel())).isPresent()
                    ? VerificationResult.live(link.providerId(), "YouTube channel is live.")
                    : VerificationResult.offline(link.providerId(), "YouTube channel is not live.");
        } catch (IOException exception) {
            return VerificationResult.offline(link.providerId(), "YouTube verification failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return VerificationResult.offline(link.providerId(), "YouTube verification was interrupted.");
        } catch (RuntimeException exception) {
            return VerificationResult.offline(link.providerId(), "YouTube response could not be read.");
        }
    }

    @Override
    public Optional<LiveStreamMetadata> metadata(StreamLink link) {
        if (!enabled || apiKey.isBlank()) {
            return Optional.empty();
        }
        try {
            Optional<JsonObject> liveVideo = liveVideo(resolveChannelId(link.channel()));
            if (liveVideo.isEmpty()) {
                return Optional.empty();
            }
            JsonObject searchItem = liveVideo.get();
            Optional<String> videoId = videoId(searchItem);
            if (videoId.isEmpty()) {
                return Optional.empty();
            }
            JsonObject details = videoDetails(videoId.get()).orElse(searchItem);
            return Optional.of(metadataFromVideo(videoId.get(), searchItem, details));
        } catch (IOException | RuntimeException exception) {
            logger.log(Level.WARNING, "YouTube metadata lookup failed for " + link.channel() + ".", exception);
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Optional<JsonObject> liveVideo(String channelId) throws IOException, InterruptedException {
        URI uri = URI.create("https://www.googleapis.com/youtube/v3/search"
                + "?part=snippet"
                + "&type=video"
                + "&eventType=live"
                + "&maxResults=1"
                + "&channelId=" + encode(channelId)
                + "&key=" + encode(apiKey));
        HttpResponse<String> response = send(uri);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("YouTube returned HTTP " + response.statusCode());
        }
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!body.has("items") || body.getAsJsonArray("items").isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(body.getAsJsonArray("items").get(0).getAsJsonObject());
    }

    private Optional<JsonObject> videoDetails(String videoId) throws IOException, InterruptedException {
        URI uri = URI.create("https://www.googleapis.com/youtube/v3/videos"
                + "?part=snippet,liveStreamingDetails,statistics"
                + "&id=" + encode(videoId)
                + "&key=" + encode(apiKey));
        HttpResponse<String> response = send(uri);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("YouTube video lookup returned HTTP " + response.statusCode());
        }
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!body.has("items") || body.getAsJsonArray("items").isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(body.getAsJsonArray("items").get(0).getAsJsonObject());
    }

    private String resolveChannelId(String configuredChannel) throws IOException, InterruptedException {
        String channel = configuredChannel.trim();
        if (!channel.startsWith("@")) {
            return channel;
        }
        URI uri = URI.create("https://www.googleapis.com/youtube/v3/channels"
                + "?part=id"
                + "&forHandle=" + encode(channel.substring(1))
                + "&key=" + encode(apiKey));
        HttpResponse<String> response = send(uri);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("handle lookup returned HTTP " + response.statusCode());
        }
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!body.has("items") || body.getAsJsonArray("items").isEmpty()) {
            throw new IOException("handle was not found");
        }
        return body.getAsJsonArray("items").get(0).getAsJsonObject().get("id").getAsString();
    }

    private HttpResponse<String> send(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static LiveStreamMetadata metadataFromVideo(String videoId, JsonObject searchItem, JsonObject details) {
        JsonObject snippet = object(details, "snippet")
                .or(() -> object(searchItem, "snippet"))
                .orElse(new JsonObject());
        JsonObject liveStreamingDetails = object(details, "liveStreamingDetails").orElse(new JsonObject());
        return new LiveStreamMetadata(
                null,
                string(snippet, "title").orElse(null),
                thumbnail(snippet).orElse(null),
                integerString(liveStreamingDetails, "concurrentViewers").orElse(null),
                instant(liveStreamingDetails, "actualStartTime").orElse(null),
                "https://youtube.com/watch?v=" + videoId
        );
    }

    private static Optional<String> videoId(JsonObject searchItem) {
        return object(searchItem, "id").flatMap(id -> string(id, "videoId"));
    }

    private static Optional<String> thumbnail(JsonObject snippet) {
        return object(snippet, "thumbnails")
                .flatMap(thumbnails -> object(thumbnails, "maxres")
                        .or(() -> object(thumbnails, "high"))
                        .or(() -> object(thumbnails, "medium"))
                        .or(() -> object(thumbnails, "default")))
                .flatMap(thumbnail -> string(thumbnail, "url"));
    }

    private static Optional<JsonObject> object(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull() || !object.get(name).isJsonObject()) {
            return Optional.empty();
        }
        return Optional.of(object.getAsJsonObject(name));
    }

    private static Optional<String> string(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            return Optional.empty();
        }
        String value = object.get(name).getAsString();
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static Optional<Integer> integerString(JsonObject object, String name) {
        return string(object, name).flatMap(value -> {
            try {
                return Optional.of(Integer.parseInt(value));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        });
    }

    private static Optional<Instant> instant(JsonObject object, String name) {
        try {
            return string(object, name).map(Instant::parse);
        } catch (DateTimeParseException exception) {
            return Optional.empty();
        }
    }
}
