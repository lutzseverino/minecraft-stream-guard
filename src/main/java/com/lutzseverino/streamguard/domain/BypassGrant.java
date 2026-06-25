package com.lutzseverino.streamguard.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record BypassGrant(
        UUID playerId,
        UUID grantedBy,
        Instant grantedAt,
        Instant expiresAt,
        String reason
) {

    public BypassGrant {
        if (playerId == null) {
            throw new IllegalArgumentException("playerId cannot be null");
        }
        if (grantedAt == null) {
            throw new IllegalArgumentException("grantedAt cannot be null");
        }
        reason = reason == null ? "" : reason.trim();
    }

    public boolean activeAt(Instant now) {
        return expiresAt == null || expiresAt.isAfter(now);
    }

    public boolean temporary() {
        return expiresAt != null;
    }

    public Optional<Instant> expiresAtOptional() {
        return Optional.ofNullable(expiresAt);
    }
}
