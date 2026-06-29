package com.lutzseverino.streamguard.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import com.lutzseverino.streamguard.domain.StreamLink;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import com.lutzseverino.streamguard.domain.VerificationStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

final class LiveFeedServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-26T12:00:00Z");
    private static final UUID LIVE_PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OFFLINE_PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Test
    void snapshotContainsOnlyOnlinePlayersWithLiveVerification() {
        InMemoryRepository repository = new InMemoryRepository(Map.of(
                LIVE_PLAYER_ID,
                PlayerAccessRecord.empty(LIVE_PLAYER_ID, "LiveLutz")
                        .withStreamLink(new StreamLink(StreamProviderId.TWITCH, "lutzseverino"))
                        .withVerificationStatus(VerificationStatus.live(StreamProviderId.TWITCH, NOW, "live")),
                OFFLINE_PLAYER_ID,
                PlayerAccessRecord.empty(OFFLINE_PLAYER_ID, "OfflineLutz")
                        .withStreamLink(new StreamLink(StreamProviderId.YOUTUBE, "UC_x5XG1OV2P6uZZ5FSM9Ttw"))
                        .withVerificationStatus(VerificationStatus.unverified(NOW, "not live"))
        ));
        LiveFeedService service = new LiveFeedService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

        LiveFeedSnapshot snapshot = service.snapshot(List.of(
                new LiveFeedPlayer(LIVE_PLAYER_ID, "CurrentLiveLutz"),
                new LiveFeedPlayer(OFFLINE_PLAYER_ID, "OfflineLutz")
        ));

        assertEquals(NOW, snapshot.updatedAt());
        assertEquals(1, snapshot.streamers().size());
        LiveStreamer streamer = snapshot.streamers().get(0);
        assertEquals("CurrentLiveLutz", streamer.playerName());
        assertEquals("twitch", streamer.provider());
        assertEquals("lutzseverino", streamer.channel());
        assertEquals("https://twitch.tv/lutzseverino", streamer.url());
        assertEquals("twitch", streamer.embed().kind());
        assertEquals("lutzseverino", streamer.embed().channel());
    }

    @Test
    void snapshotEnrichesVerifiedPlayersWithProviderMetadata() {
        InMemoryRepository repository = new InMemoryRepository(Map.of(
                LIVE_PLAYER_ID,
                PlayerAccessRecord.empty(LIVE_PLAYER_ID, "LiveLutz")
                        .withStreamLink(new StreamLink(StreamProviderId.TWITCH, "lutzseverino"))
                        .withVerificationStatus(VerificationStatus.live(StreamProviderId.TWITCH, NOW, "live"))
        ));
        StreamMetadataProvider metadataProvider = link -> Optional.of(new LiveStreamMetadata(
                "live-lutz",
                "Building a base",
                "https://example.com/thumb.jpg",
                42,
                NOW.minusSeconds(600),
                "https://twitch.tv/live-lutz"
        ));
        LiveFeedService service = new LiveFeedService(repository, metadataProvider, Clock.fixed(NOW, ZoneOffset.UTC));

        LiveFeedSnapshot snapshot = service.snapshot(List.of(new LiveFeedPlayer(LIVE_PLAYER_ID, "CurrentLiveLutz")));

        LiveStreamer streamer = snapshot.streamers().get(0);
        assertEquals("https://twitch.tv/live-lutz", streamer.url());
        assertEquals("Building a base", streamer.title());
        assertEquals("https://example.com/thumb.jpg", streamer.thumbnailUrl());
        assertEquals(42, streamer.viewerCount());
        assertEquals("2026-06-26T11:50:00Z", streamer.liveSince());
        assertEquals("live-lutz", streamer.channel());
        assertEquals("live-lutz", streamer.embed().channel());
    }

    private static final class InMemoryRepository implements PlayerAccessRepository {

        private final Map<UUID, PlayerAccessRecord> records = new ConcurrentHashMap<>();

        private InMemoryRepository(Map<UUID, PlayerAccessRecord> records) {
            this.records.putAll(records);
        }

        @Override
        public PlayerAccessRecord getOrCreate(UUID playerId, String playerName) {
            return records.getOrDefault(playerId, PlayerAccessRecord.empty(playerId, playerName));
        }

        @Override
        public Optional<PlayerAccessRecord> find(UUID playerId) {
            return Optional.ofNullable(records.get(playerId));
        }

        @Override
        public void save(PlayerAccessRecord accessRecord) {
            records.put(accessRecord.playerId(), accessRecord);
        }
    }
}
