package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import com.lutzseverino.streamguard.domain.StreamLink;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import com.lutzseverino.streamguard.domain.VerificationStatus;
import java.time.Clock;
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
        PlayerAccessRecord record = repository.getOrCreate(playerId, playerName)
                .withStreamLink(new StreamLink(providerId, linkNormalizer.normalize(providerId, channel)))
                .withVerificationStatus(VerificationStatus.unverified(clock.instant(), "Stream link changed."));
        repository.save(record);
        return record;
    }

    public VerificationStatus verify(UUID playerId, String playerName) {
        PlayerAccessRecord record = repository.getOrCreate(playerId, playerName);
        Optional<StreamLink> link = record.streamLinkOptional();
        VerificationStatus status;
        if (link.isEmpty()) {
            status = VerificationStatus.unverified(clock.instant(), "No stream link configured.");
        } else {
            VerificationResult result = verificationProvider.verify(link.get());
            status = result.live()
                    ? VerificationStatus.live(result.providerId(), clock.instant(), result.detail())
                    : VerificationStatus.unverified(clock.instant(), result.detail());
        }
        repository.save(record.withVerificationStatus(status));
        return status;
    }

    public void manuallyVerify(
            UUID playerId,
            String playerName,
            StreamProviderId providerId,
            String detail
    ) {
        VerificationStatus status = VerificationStatus.live(providerId, clock.instant(), detail);
        repository.save(repository.getOrCreate(playerId, playerName).withVerificationStatus(status));
    }

    public void unverify(UUID playerId, String playerName, String detail) {
        VerificationStatus status = VerificationStatus.unverified(clock.instant(), detail);
        repository.save(repository.getOrCreate(playerId, playerName).withVerificationStatus(status));
    }

    public PlayerAccessRecord status(UUID playerId, String playerName) {
        return repository.getOrCreate(playerId, playerName);
    }
}
