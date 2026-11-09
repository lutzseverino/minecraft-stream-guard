package com.lutzseverino.streamguard.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import com.lutzseverino.streamguard.application.LiveStreamMetadata;
import com.lutzseverino.streamguard.domain.StreamLink;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import org.junit.jupiter.api.Test;

final class TwitchStreamVerificationProviderTest {

  @Test
  void mapsHelixStreamMetadataIntoLiveFeedMetadata() {
    LiveStreamMetadata metadata =
        TwitchStreamVerificationProvider.metadataFromStream(
            new StreamLink(StreamProviderId.TWITCH, "fallback"),
            JsonParser.parseString(
                    """
                        {
                          "user_login": "redstonerhea",
                          "title": "Building a trading hall",
                          "viewer_count": 1284,
                          "started_at": "2026-06-26T10:24:00Z",
                          "thumbnail_url": "https://static-cdn.jtvnw.net/previews-ttv/live_user_redstonerhea-{width}x{height}.jpg"
                        }
                        """)
                .getAsJsonObject());

    assertEquals("Building a trading hall", metadata.title());
    assertEquals(
        "https://static-cdn.jtvnw.net/previews-ttv/live_user_redstonerhea-1280x720.jpg",
        metadata.thumbnailUrl());
    assertEquals(1284, metadata.viewerCount());
    assertEquals("2026-06-26T10:24:00Z", metadata.liveSince().toString());
    assertEquals("https://twitch.tv/redstonerhea", metadata.url());
    assertEquals("redstonerhea", metadata.channel());
  }
}
