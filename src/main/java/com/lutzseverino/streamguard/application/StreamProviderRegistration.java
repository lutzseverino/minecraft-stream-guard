package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.StreamProviderId;
import java.util.function.UnaryOperator;

public record StreamProviderRegistration(
        StreamProviderId providerId,
        StreamVerificationProvider verificationProvider,
        UnaryOperator<String> linkNormalizer
) {

    public StreamProviderRegistration {
        if (providerId == null) {
            throw new IllegalArgumentException("providerId cannot be null");
        }
        if (verificationProvider == null) {
            throw new IllegalArgumentException("verificationProvider cannot be null");
        }
        if (linkNormalizer == null) {
            linkNormalizer = String::trim;
        }
    }

    public static StreamProviderRegistration of(
            StreamProviderId providerId,
            StreamVerificationProvider verificationProvider
    ) {
        return new StreamProviderRegistration(providerId, verificationProvider, String::trim);
    }
}
