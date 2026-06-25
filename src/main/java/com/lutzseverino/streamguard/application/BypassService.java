package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.BypassGrant;
import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class BypassService {

    private final PlayerAccessRepository repository;
    private final Clock clock;

    public BypassService(PlayerAccessRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public BypassGrant grant(UUID playerId, String playerName, UUID grantedBy, Duration duration, String reason) {
        Instant now = clock.instant();
        Instant expiresAt = duration == null ? null : now.plus(duration);
        BypassGrant grant = new BypassGrant(playerId, grantedBy, now, expiresAt, reason);
        PlayerAccessRecord record = repository.getOrCreate(playerId, playerName).withBypassGrant(grant);
        repository.save(record);
        return grant;
    }

    public void revoke(UUID playerId, String playerName) {
        PlayerAccessRecord record = repository.getOrCreate(playerId, playerName);
        repository.save(record.withoutBypassGrant());
    }
}
