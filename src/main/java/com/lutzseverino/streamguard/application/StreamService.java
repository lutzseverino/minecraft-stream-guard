package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import com.lutzseverino.streamguard.domain.StreamLink;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import com.lutzseverino.streamguard.domain.VerificationStatus;
import java.time.Clock;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class StreamService {

    private final PlayerAccessRepository repository;
    private final StreamVerificationProvider verificationProvider;
    private final StreamLinkNormalizer linkNormalizer;
    private final Clock clock;

    public StreamService(
            PlayerAccessRepository repository,
            StreamVerificationProvider verificationProvider,
            StreamLinkNormalizer linkNormalizer,
            Clock clock
    ) {
        this.repository = repository;
        this.verificationProvider = verificationProvider;
        this.linkNormalizer = linkNormalizer;
        this.clock = clock;
    }

    public PlayerAccessRecord link(UUID playerId, String playerName, StreamProviderId providerId, String channel) {
        PlayerAccessRecord accessRecord = repository.getOrCreate(playerId, playerName)
                .withStreamLink(new StreamLink(providerId, linkNormalizer.normalize(providerId, channel)))
                .withVerificationStatus(VerificationStatus.unverified(clock.instant(), "Stream link changed."));
        repository.save(accessRecord);
        return accessRecord;
    }

    public VerificationStatus verify(UUID playerId, String playerName) {
        PlayerAccessRecord accessRecord = repository.getOrCreate(playerId, playerName);
        Optional<StreamLink> link = accessRecord.streamLinkOptional();
        if (link.isEmpty()) {
            VerificationStatus status = VerificationStatus.unverified(clock.instant(), "No stream link configured.");
            repository.save(accessRecord.withVerificationStatus(status));
            return status;
        }

        StreamLink expectedLink = link.get();
        VerificationResult result = verificationProvider.verify(expectedLink);
        if (!result.available()) {
            return accessRecord.verificationStatusOptional().orElseGet(() ->
                    VerificationStatus.unverified(clock.instant(), result.detail()));
        }
        VerificationStatus status = statusFromResult(result);
        if (repository.saveIfUnchanged(new PlayerAccessUpdate(
                accessRecord,
                accessRecord.withVerificationStatus(status)
        ))) {
            return status;
        }
        return repository.getOrCreate(playerId, playerName)
                .verificationStatusOptional()
                .orElseGet(() -> VerificationStatus.unverified(clock.instant(), "Player stream state changed."));
    }

    public Map<UUID, VerificationStatus> verifyAll(Collection<StreamVerificationTarget> targets) {
        Map<UUID, PlayerAccessRecord> expectedRecords = new LinkedHashMap<>();
        Map<StreamLink, StreamLink> links = new LinkedHashMap<>();
        for (StreamVerificationTarget target : targets) {
            PlayerAccessRecord accessRecord = repository.getOrCreate(target.playerId(), target.playerName());
            expectedRecords.put(target.playerId(), accessRecord);
            accessRecord.streamLinkOptional().ifPresent(link -> links.put(link, link));
        }

        Map<StreamLink, VerificationResult> verificationResults = verificationProvider.verifyAll(links.keySet());
        Map<UUID, VerificationStatus> statuses = new LinkedHashMap<>();
        Collection<PlayerAccessUpdate> updates = new ArrayList<>();
        for (StreamVerificationTarget target : targets) {
            PlayerAccessRecord expected = expectedRecords.get(target.playerId());
            StreamLink link = expected.streamLink();
            VerificationStatus status;
            boolean shouldPersist = true;
            if (link == null) {
                status = VerificationStatus.unverified(clock.instant(), "No stream link configured.");
            } else {
                VerificationResult result = verificationResults.getOrDefault(
                        link,
                        VerificationResult.unavailable(link.providerId(), "Verification provider returned no result.")
                );
                shouldPersist = result.available();
                status = shouldPersist
                        ? statusFromResult(result)
                        : expected.verificationStatusOptional().orElseGet(() ->
                                VerificationStatus.unverified(clock.instant(), result.detail()));
            }
            if (shouldPersist) {
                updates.add(new PlayerAccessUpdate(expected, expected.withVerificationStatus(status)));
            }
            statuses.put(target.playerId(), status);
        }
        repository.saveAllIfUnchanged(updates);
        return Map.copyOf(statuses);
    }

    public void manuallyVerify(
            UUID playerId,
            String playerName,
            String detail
    ) {
        VerificationStatus status = VerificationStatus.live(StreamProviderId.MANUAL, clock.instant(), detail);
        repository.save(repository.getOrCreate(playerId, playerName).withVerificationStatus(status));
    }

    public void unverify(UUID playerId, String playerName, String detail) {
        VerificationStatus status = VerificationStatus.unverified(clock.instant(), detail);
        repository.save(repository.getOrCreate(playerId, playerName).withVerificationStatus(status));
    }

    public PlayerAccessRecord status(UUID playerId, String playerName) {
        return repository.getOrCreate(playerId, playerName);
    }

    private VerificationStatus statusFromResult(VerificationResult result) {
        return result.live()
                ? VerificationStatus.live(result.providerId(), clock.instant(), result.detail())
                : VerificationStatus.unverified(clock.instant(), result.detail());
    }
}
