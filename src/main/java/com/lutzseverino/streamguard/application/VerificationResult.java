package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.StreamProviderId;

public record VerificationResult(boolean live, StreamProviderId providerId, String detail) {

    public static VerificationResult live(StreamProviderId providerId, String detail) {
        return new VerificationResult(true, providerId, detail == null ? "" : detail);
    }

    public static VerificationResult offline(StreamProviderId providerId, String detail) {
        return new VerificationResult(false, providerId, detail == null ? "" : detail);
    }
}
