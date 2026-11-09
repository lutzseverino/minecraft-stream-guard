package com.lutzseverino.streamguard.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import com.lutzseverino.streamguard.application.LiveStreamMetadata;
import org.junit.jupiter.api.Test;

final class YouTubeStreamVerificationProviderTest {

  @Test
  void mapsVideoDetailsIntoLiveFeedMetadata() {
    LiveStreamMetadata metadata =
        YouTubeStreamVerificationProvider.metadataFromVideo(
            "jfKfPfyJRdk",
            JsonParser.parseString(
                    """
                        {
                          "id": { "videoId": "jfKfPfyJRdk" },
                          "snippet": {
                            "title": "Fallback title",
                            "thumbnails": {
                              "medium": { "url": "https://example.com/fallback.jpg" }
                            }
                          }
                        }
                        """)
                .getAsJsonObject(),
            JsonParser.parseString(
                    """
                        {
                          "snippet": {
                            "title": "Hardcore live",
                            "thumbnails": {
                              "high": { "url": "https://example.com/high.jpg" }
                            }
                          },
                          "liveStreamingDetails": {
                            "actualStartTime": "2026-06-26T10:24:00Z",
                            "concurrentViewers": "612"
                          }
                        }
                        """)
                .getAsJsonObject());

    assertEquals("Hardcore live", metadata.title());
    assertEquals("https://example.com/high.jpg", metadata.thumbnailUrl());
    assertEquals(612, metadata.viewerCount());
    assertEquals("2026-06-26T10:24:00Z", metadata.liveSince().toString());
    assertEquals("https://youtube.com/watch?v=jfKfPfyJRdk", metadata.url());
  }
}
