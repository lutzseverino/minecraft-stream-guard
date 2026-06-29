package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.StreamProviderId;
import java.util.function.UnaryOperator;

public record StreamProviderRegistration(
        StreamProviderId providerId,
        StreamVerificationProvider verificationProvider,
        StreamMetadataProvider metadataProvider,
        UnaryOperator<String> linkNormalizer
) {

    public StreamProviderRegistration(
            StreamProviderId providerId,
            StreamVerificationProvider verificationProvider,
            UnaryOperator<String> linkNormalizer
    ) {
        this(providerId, verificationProvider, StreamMetadataProvider.none(), linkNormalizer);
    }

    public StreamProviderRegistration {
        if (providerId == null) {
            throw new IllegalArgumentException("providerId cannot be null");
        }
        if (verificationProvider == null) {
            throw new IllegalArgumentException("verificationProvider cannot be null");
        }
        if (metadataProvider == null) {
            metadataProvider = StreamMetadataProvider.none();
        }
        if (linkNormalizer == null) {
            linkNormalizer = String::trim;
        }
    }

    public static StreamProviderRegistration of(
            StreamProviderId providerId,
            StreamVerificationProvider verificationProvider
    ) {
        return new StreamProviderRegistration(
                providerId,
                verificationProvider,
                StreamMetadataProvider.none(),
                String::trim
        );
    }
}
