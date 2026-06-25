package com.lutzseverino.streamguard.infrastructure;

import com.lutzseverino.streamguard.application.StreamVerificationProvider;
import com.lutzseverino.streamguard.application.VerificationResult;
import com.lutzseverino.streamguard.domain.StreamLink;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public final class YouTubeStreamVerificationProvider implements StreamVerificationProvider {

    private final boolean enabled;
    private final String apiKey;
    private final HttpClient httpClient;

    public YouTubeStreamVerificationProvider(boolean enabled, String apiKey) {
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.httpClient = HttpClient.newHttpClient();
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
            String channelId = resolveChannelId(link.channel());
            URI uri = URI.create("https://www.googleapis.com/youtube/v3/search"
                    + "?part=snippet"
                    + "&type=video"
                    + "&eventType=live"
                    + "&maxResults=1"
                    + "&channelId=" + encode(channelId)
                    + "&key=" + encode(apiKey));
            HttpResponse<String> response = send(uri);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return VerificationResult.offline(link.providerId(), "YouTube returned HTTP " + response.statusCode() + ".");
            }
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            boolean live = body.has("items") && !body.getAsJsonArray("items").isEmpty();
            return live
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
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
