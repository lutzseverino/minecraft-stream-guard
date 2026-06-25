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
import java.time.Instant;

@SuppressWarnings("UastIncorrectHttpHeaderInspection")
public final class TwitchStreamVerificationProvider implements StreamVerificationProvider {

    private static final URI TOKEN_URI = URI.create("https://id.twitch.tv/oauth2/token");

    private final boolean enabled;
    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private String accessToken = "";
    private Instant tokenExpiresAt = Instant.EPOCH;

    public TwitchStreamVerificationProvider(boolean enabled, String clientId, String clientSecret) {
        this.enabled = enabled;
        this.clientId = clientId == null ? "" : clientId;
        this.clientSecret = clientSecret == null ? "" : clientSecret;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public VerificationResult verify(StreamLink link) {
        if (!enabled) {
            return VerificationResult.offline(link.providerId(), "Twitch verification is disabled.");
        }
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return VerificationResult.offline(link.providerId(), "Twitch credentials are missing.");
        }
        try {
            String token = accessToken();
            String channel = encode(link.channel());
            URI uri = URI.create("https://api.twitch.tv/helix/streams"
                    + "?user_login=" + channel
                    + "&type=live"
                    + "&first=1");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Client-Id", clientId)
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return VerificationResult.offline(link.providerId(), "Twitch returned HTTP " + response.statusCode() + ".");
            }
            JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
            boolean live = body.has("data") && !body.getAsJsonArray("data").isEmpty();
            return live
                    ? VerificationResult.live(link.providerId(), "Twitch channel is live.")
                    : VerificationResult.offline(link.providerId(), "Twitch channel is not live.");
        } catch (IOException exception) {
            return VerificationResult.offline(link.providerId(), "Twitch verification failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return VerificationResult.offline(link.providerId(), "Twitch verification was interrupted.");
        } catch (RuntimeException exception) {
            return VerificationResult.offline(link.providerId(), "Twitch response could not be read.");
        }
    }

    private synchronized String accessToken() throws IOException, InterruptedException {
        if (!accessToken.isBlank() && tokenExpiresAt.isAfter(Instant.now().plusSeconds(60))) {
            return accessToken;
        }
        String body = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&grant_type=client_credentials";
        HttpRequest request = HttpRequest.newBuilder(TOKEN_URI)
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

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
