package com.lutzseverino.streamguard.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import com.lutzseverino.streamguard.domain.StreamLink;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import com.lutzseverino.streamguard.domain.VerificationStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class StreamServiceTest {

  private static final Instant NOW = Instant.parse("2026-06-26T12:00:00Z");
  private static final UUID FIRST_PLAYER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID SECOND_PLAYER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000102");

  @Test
  void batchedVerificationDeduplicatesSharedStreamLinks() {
    StreamLink sharedLink = new StreamLink(StreamProviderId.TWITCH, "lutzseverino");
    InMemoryRepository repository =
        new InMemoryRepository(
            Map.of(
                FIRST_PLAYER_ID,
                PlayerAccessRecord.empty(FIRST_PLAYER_ID, "First").withStreamLink(sharedLink),
                SECOND_PLAYER_ID,
                PlayerAccessRecord.empty(SECOND_PLAYER_ID, "Second").withStreamLink(sharedLink)));
    CountingProvider provider = new CountingProvider();
    StreamService service =
        new StreamService(
            repository,
            provider,
            (providerId, linkReference) -> linkReference.trim(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    Map<UUID, VerificationStatus> statuses =
        service.verifyAll(
            java.util.List.of(
                new StreamVerificationTarget(FIRST_PLAYER_ID, "First"),
                new StreamVerificationTarget(SECOND_PLAYER_ID, "Second")));

    assertEquals(1, provider.calls.get());
    assertEquals(1, provider.linkCount.get());
    assertTrue(statuses.get(FIRST_PLAYER_ID).live());
    assertTrue(statuses.get(SECOND_PLAYER_ID).live());
    assertTrue(repository.find(FIRST_PLAYER_ID).orElseThrow().verificationStatus().live());
    assertTrue(repository.find(SECOND_PLAYER_ID).orElseThrow().verificationStatus().live());
  }

  @Test
  void batchedVerificationDoesNotOverwriteRelinkedRecord() {
    StreamLink oldLink = new StreamLink(StreamProviderId.TWITCH, "oldchannel");
    StreamLink newLink = new StreamLink(StreamProviderId.TWITCH, "newchannel");
    InMemoryRepository repository =
        new InMemoryRepository(
            Map.of(
                FIRST_PLAYER_ID,
                PlayerAccessRecord.empty(FIRST_PLAYER_ID, "First").withStreamLink(oldLink)));
    StreamVerificationProvider provider =
        new StreamVerificationProvider() {
          @Override
          public VerificationResult verify(StreamLink link) {
            repository.save(
                repository.getOrCreate(FIRST_PLAYER_ID, "First").withStreamLink(newLink));
            return VerificationResult.live(link.providerId(), "old link is live");
          }

          @Override
          public Map<StreamLink, VerificationResult> verifyAll(Collection<StreamLink> links) {
            repository.save(
                repository.getOrCreate(FIRST_PLAYER_ID, "First").withStreamLink(newLink));
            return links.stream()
                .collect(
                    Collectors.toMap(
                        link -> link,
                        link -> VerificationResult.live(link.providerId(), "old link is live")));
          }
        };
    StreamService service =
        new StreamService(
            repository,
            provider,
            (providerId, linkReference) -> linkReference.trim(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    service.verifyAll(java.util.List.of(new StreamVerificationTarget(FIRST_PLAYER_ID, "First")));

    PlayerAccessRecord currentRecord = repository.find(FIRST_PLAYER_ID).orElseThrow();
    assertEquals(newLink, currentRecord.streamLink());
    assertTrue(currentRecord.verificationStatusOptional().isEmpty());
  }

  @Test
  void batchedVerificationDoesNotOverwriteChangedStatusForSameLink() {
    StreamLink link = new StreamLink(StreamProviderId.TWITCH, "oldchannel");
    VerificationStatus manualStatus =
        VerificationStatus.live(StreamProviderId.MANUAL, NOW.plusSeconds(1), "manual");
    InMemoryRepository repository =
        new InMemoryRepository(
            Map.of(
                FIRST_PLAYER_ID,
                PlayerAccessRecord.empty(FIRST_PLAYER_ID, "First").withStreamLink(link)));
    StreamVerificationProvider provider =
        new StreamVerificationProvider() {
          @Override
          public VerificationResult verify(StreamLink link) {
            repository.save(
                repository
                    .getOrCreate(FIRST_PLAYER_ID, "First")
                    .withVerificationStatus(manualStatus));
            return VerificationResult.offline(link.providerId(), "provider says offline");
          }

          @Override
          public Map<StreamLink, VerificationResult> verifyAll(Collection<StreamLink> links) {
            repository.save(
                repository
                    .getOrCreate(FIRST_PLAYER_ID, "First")
                    .withVerificationStatus(manualStatus));
            return links.stream()
                .collect(
                    Collectors.toMap(
                        link -> link,
                        link ->
                            VerificationResult.offline(
                                link.providerId(), "provider says offline")));
          }
        };
    StreamService service =
        new StreamService(
            repository,
            provider,
            (providerId, linkReference) -> linkReference.trim(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    service.verifyAll(java.util.List.of(new StreamVerificationTarget(FIRST_PLAYER_ID, "First")));

    PlayerAccessRecord currentRecord = repository.find(FIRST_PLAYER_ID).orElseThrow();
    assertEquals(link, currentRecord.streamLink());
    assertEquals(manualStatus, currentRecord.verificationStatus());
  }

  @Test
  void unavailableProviderDoesNotReplaceTheLastObservedStatus() {
    StreamLink link = new StreamLink(StreamProviderId.TWITCH, "channel");
    VerificationStatus lastLive =
        VerificationStatus.live(
            StreamProviderId.TWITCH, NOW.minusSeconds(30), "last successful check");
    InMemoryRepository repository =
        new InMemoryRepository(
            Map.of(
                FIRST_PLAYER_ID,
                PlayerAccessRecord.empty(FIRST_PLAYER_ID, "First")
                    .withStreamLink(link)
                    .withVerificationStatus(lastLive)));
    StreamService service =
        new StreamService(
            repository,
            ignored ->
                VerificationResult.unavailable(StreamProviderId.TWITCH, "provider unavailable"),
            (providerId, linkReference) -> linkReference,
            Clock.fixed(NOW, ZoneOffset.UTC));

    VerificationStatus returned = service.verify(FIRST_PLAYER_ID, "First");

    assertEquals(lastLive, returned);
    assertEquals(lastLive, repository.find(FIRST_PLAYER_ID).orElseThrow().verificationStatus());
  }

  private static final class CountingProvider implements StreamVerificationProvider {

    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicInteger linkCount = new AtomicInteger();

    @Override
    public VerificationResult verify(StreamLink link) {
      calls.incrementAndGet();
      linkCount.incrementAndGet();
      return VerificationResult.live(link.providerId(), "live");
    }

    @Override
    public Map<StreamLink, VerificationResult> verifyAll(Collection<StreamLink> links) {
      calls.incrementAndGet();
      linkCount.addAndGet(links.size());
      return links.stream()
          .collect(
              Collectors.toMap(
                  link -> link, link -> VerificationResult.live(link.providerId(), "live")));
    }
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

    @Override
    public boolean saveIfUnchanged(PlayerAccessUpdate update) {
      return records.replace(update.expected().playerId(), update.expected(), update.updated());
    }
  }
}
