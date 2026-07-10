package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.StreamProviderId;
import java.util.Objects;

public record VerificationResult(Outcome outcome, StreamProviderId providerId, String detail) {

    public VerificationResult {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(providerId, "providerId");
        detail = detail == null ? "" : detail;
    }

    public static VerificationResult live(StreamProviderId providerId, String detail) {
        return new VerificationResult(Outcome.LIVE, providerId, detail);
    }

    public static VerificationResult offline(StreamProviderId providerId, String detail) {
        return new VerificationResult(Outcome.OFFLINE, providerId, detail);
    }

    public static VerificationResult unavailable(StreamProviderId providerId, String detail) {
        return new VerificationResult(Outcome.UNAVAILABLE, providerId, detail);
    }

    public boolean live() {
        return outcome == Outcome.LIVE;
    }

    public boolean available() {
        return outcome != Outcome.UNAVAILABLE;
    }

    public enum Outcome {
        LIVE,
        OFFLINE,
        UNAVAILABLE
    }
}
