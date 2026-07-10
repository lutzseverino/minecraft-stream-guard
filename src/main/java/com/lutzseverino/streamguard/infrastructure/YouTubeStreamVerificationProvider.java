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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YouTubeStreamVerificationProvider implements StreamVerificationProvider, StreamMetadataProvider {

    private static final Pattern YOUTUBE_WATCH_URL = Pattern.compile("[?&]v=([^&]+)");
    private static final Pattern YOUTUBE_SHORT_URL = Pattern.compile("youtu\\.be/([^/?#]+)");
    private static final Pattern YOUTUBE_LIVE_URL = Pattern.compile("youtube\\.com/live/([^/?#]+)");
    private static final Pattern YOUTUBE_CHANNEL_URL = Pattern.compile("youtube\\.com/channel/([^/?#]+)");
    private static final Pattern YOUTUBE_HANDLE_URL = Pattern.compile("youtube\\.com/@([^/?#]+)");
    private static final int MAX_VIDEO_IDS_PER_REQUEST = 50;
    private final boolean enabled;
    private final String apiKey;
    private final HttpClient httpClient;
    private final Logger logger;
    private final Map<String, String> channelIdsByHandle = new ConcurrentHashMap<>();

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
        return verifyAll(List.of(link)).get(link);
    }

    @Override
    public Map<StreamLink, VerificationResult> verifyAll(Collection<StreamLink> links) {
        if (!enabled) {
            return offlineResults(links, "YouTube verification is disabled.");
        }
        if (apiKey.isBlank()) {
            return offlineResults(links, "YouTube API key is missing.");
        }

        Map<StreamLink, VerificationResult> results = new LinkedHashMap<>();
        Map<StreamLink, String> directVideoIds = new LinkedHashMap<>();
        for (StreamLink link : links) {
            Optional<String> videoId = videoIdFromReference(link.channel());
            if (videoId.isPresent()) {
                directVideoIds.put(link, videoId.get());
            }
        }

        if (!directVideoIds.isEmpty()) {
            verifyDirectVideoLinks(directVideoIds, results);
        }

        for (StreamLink link : links) {
            if (results.containsKey(link)) {
                continue;
            }
            try {
                results.put(link, liveVideoForChannel(resolveChannelId(link.channel())).isPresent()
                        ? VerificationResult.live(link.providerId(), "YouTube channel is live.")
                        : VerificationResult.offline(link.providerId(), "YouTube channel is not live."));
            } catch (IOException exception) {
                results.put(link, VerificationResult.unavailable(
                        link.providerId(),
                        "YouTube verification failed: " + exception.getMessage()
                ));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                results.put(link, VerificationResult.unavailable(link.providerId(), "YouTube verification was interrupted."));
            } catch (RuntimeException exception) {
                results.put(link, VerificationResult.unavailable(link.providerId(), "YouTube response could not be read."));
            }
        }
        return Map.copyOf(results);
    }

    @Override
    public Optional<LiveStreamMetadata> metadata(StreamLink link) {
        if (!enabled || apiKey.isBlank()) {
            return Optional.empty();
        }
        try {
            Optional<String> directVideoId = videoIdFromReference(link.channel());
            if (directVideoId.isPresent()) {
                return videoDetails(directVideoId.get())
                        .filter(YouTubeStreamVerificationProvider::isLiveVideo)
                        .map(details -> metadataFromVideo(directVideoId.get(), details, details));
            }
            Optional<JsonObject> liveVideo = liveVideo(link.channel());
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

    private Optional<JsonObject> liveVideo(String reference) throws IOException, InterruptedException {
        Optional<String> videoId = videoIdFromReference(reference);
        if (videoId.isPresent()) {
            return videoDetails(videoId.get()).filter(YouTubeStreamVerificationProvider::isLiveVideo);
        }
        return liveVideoForChannel(resolveChannelId(reference));
    }

    private Optional<JsonObject> liveVideoForChannel(String channelId) throws IOException, InterruptedException {
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
        return Optional.ofNullable(videoDetails(List.of(videoId)).get(videoId));
    }

    private Map<String, JsonObject> videoDetails(Collection<String> videoIds) throws IOException, InterruptedException {
        Map<String, String> uniqueVideoIds = new LinkedHashMap<>();
        for (String videoId : videoIds) {
            if (videoId != null && !videoId.isBlank()) {
                uniqueVideoIds.putIfAbsent(videoId, videoId);
            }
        }
        if (uniqueVideoIds.isEmpty()) {
            return Map.of();
        }
        Map<String, JsonObject> detailsByVideoId = new LinkedHashMap<>();
        List<String> ids = List.copyOf(uniqueVideoIds.keySet());
        for (int index = 0; index < ids.size(); index += MAX_VIDEO_IDS_PER_REQUEST) {
            List<String> chunk = ids.subList(index, Math.min(index + MAX_VIDEO_IDS_PER_REQUEST, ids.size()));
            detailsByVideoId.putAll(fetchVideoDetails(chunk));
        }
        return Map.copyOf(detailsByVideoId);
    }

    private Map<String, JsonObject> fetchVideoDetails(List<String> videoIds) throws IOException, InterruptedException {
        URI uri = URI.create("https://www.googleapis.com/youtube/v3/videos"
                + "?part=snippet,liveStreamingDetails,statistics"
                + "&id=" + encode(String.join(",", videoIds))
                + "&key=" + encode(apiKey));
        HttpResponse<String> response = send(uri);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("YouTube video lookup returned HTTP " + response.statusCode());
        }
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!body.has("items") || body.getAsJsonArray("items").isEmpty()) {
            return Map.of();
        }
        Map<String, JsonObject> detailsByVideoId = new LinkedHashMap<>();
        for (int index = 0; index < body.getAsJsonArray("items").size(); index++) {
            JsonObject item = body.getAsJsonArray("items").get(index).getAsJsonObject();
            string(item, "id").ifPresent(videoId -> detailsByVideoId.put(videoId, item));
        }
        return Map.copyOf(detailsByVideoId);
    }

    private String resolveChannelId(String configuredChannel) throws IOException, InterruptedException {
        String channel = configuredChannel.trim();
        Optional<String> channelIdFromUrl = channelIdFromUrl(channel);
        if (channelIdFromUrl.isPresent()) {
            return channelIdFromUrl.get();
        }
        Optional<String> handleFromUrl = handleFromUrl(channel);
        if (handleFromUrl.isPresent()) {
            channel = "@" + handleFromUrl.get();
        }
        if (!channel.startsWith("@")) {
            return channel;
        }
        String cachedChannelId = channelIdsByHandle.get(channel);
        if (cachedChannelId != null) {
            return cachedChannelId;
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
        String channelId = body.getAsJsonArray("items").get(0).getAsJsonObject().get("id").getAsString();
        channelIdsByHandle.put(channel, channelId);
        return channelId;
    }

    private HttpResponse<String> send(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(REQUEST_TIMEOUT).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void verifyDirectVideoLinks(
            Map<StreamLink, String> directVideoIds,
            Map<StreamLink, VerificationResult> results
    ) {
        try {
            Map<String, JsonObject> detailsByVideoId = videoDetails(directVideoIds.values());
            for (Map.Entry<StreamLink, String> entry : directVideoIds.entrySet()) {
                JsonObject details = detailsByVideoId.get(entry.getValue());
                results.put(entry.getKey(), details != null && isLiveVideo(details)
                        ? VerificationResult.live(entry.getKey().providerId(), "YouTube stream is live.")
                        : VerificationResult.offline(entry.getKey().providerId(), "YouTube stream is not live."));
            }
        } catch (IOException exception) {
            directVideoIds.keySet().forEach(link -> results.put(link, VerificationResult.unavailable(
                    link.providerId(),
                    "YouTube verification failed: " + exception.getMessage()
            )));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            directVideoIds.keySet().forEach(link -> results.put(
                    link,
                    VerificationResult.unavailable(link.providerId(), "YouTube verification was interrupted.")
            ));
        } catch (RuntimeException exception) {
            directVideoIds.keySet().forEach(link -> results.put(
                    link,
                    VerificationResult.unavailable(link.providerId(), "YouTube response could not be read.")
            ));
        }
    }

    private static Map<StreamLink, VerificationResult> offlineResults(Collection<StreamLink> links, String detail) {
        Map<StreamLink, VerificationResult> results = new LinkedHashMap<>();
        for (StreamLink link : links) {
            results.put(link, VerificationResult.offline(link.providerId(), detail));
        }
        return Map.copyOf(results);
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

    private static boolean isLiveVideo(JsonObject video) {
        Optional<String> broadcastState = object(video, "snippet").flatMap(snippet -> string(snippet, "liveBroadcastContent"));
        if (broadcastState.filter("live"::equalsIgnoreCase).isPresent()) {
            return true;
        }
        Optional<JsonObject> liveDetails = object(video, "liveStreamingDetails");
        return liveDetails.flatMap(details -> string(details, "actualStartTime")).isPresent()
                && liveDetails.flatMap(details -> string(details, "actualEndTime")).isEmpty();
    }

    private static Optional<String> videoIdFromReference(String value) {
        return firstMatch(YOUTUBE_WATCH_URL, value)
                .or(() -> firstMatch(YOUTUBE_SHORT_URL, value))
                .or(() -> firstMatch(YOUTUBE_LIVE_URL, value));
    }

    private static Optional<String> channelIdFromUrl(String value) {
        return firstMatch(YOUTUBE_CHANNEL_URL, value);
    }

    private static Optional<String> handleFromUrl(String value) {
        return firstMatch(YOUTUBE_HANDLE_URL, value);
    }

    private static Optional<String> firstMatch(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
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
