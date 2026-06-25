package com.lutzseverino.streamguard.domain;

import java.time.Instant;
import java.util.Optional;

public record VerificationStatus(
        boolean live,
        StreamProviderId providerId,
        Instant checkedAt,
        String detail
) {

    public static VerificationStatus unverified(Instant checkedAt, String detail) {
        return new VerificationStatus(false, null, checkedAt, detail == null ? "" : detail);
    }

    public static VerificationStatus live(StreamProviderId providerId, Instant checkedAt, String detail) {
        return new VerificationStatus(true, providerId, checkedAt, detail == null ? "" : detail);
    }

    public Optional<StreamProviderId> verifiedProviderId() {
        return Optional.ofNullable(providerId);
    }
}
