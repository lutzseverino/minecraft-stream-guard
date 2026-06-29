package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.StreamLink;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StreamProviderRegistry implements StreamVerificationProvider, StreamLinkNormalizer, StreamMetadataProvider {

    private final Map<StreamProviderId, StreamProviderRegistration> registrations;

    public StreamProviderRegistry(List<StreamProviderRegistration> registrations) {
        this.registrations = new HashMap<>();
        for (StreamProviderRegistration registration : registrations) {
            this.registrations.put(registration.providerId(), registration);
        }
    }

    @Override
    public VerificationResult verify(StreamLink link) {
        StreamProviderRegistration registration = registrations.get(link.providerId());
        if (registration == null) {
            return VerificationResult.offline(
                    link.providerId(),
                    "No verifier is configured for " + link.providerId().displayName() + "."
            );
        }
        return registration.verificationProvider().verify(link);
    }

    @Override
    public String normalize(StreamProviderId providerId, String linkReference) {
        StreamProviderRegistration registration = registrations.get(providerId);
        if (registration == null) {
            return linkReference.trim();
        }
        return registration.linkNormalizer().apply(linkReference);
    }

    @Override
    public Optional<LiveStreamMetadata> metadata(StreamLink link) {
        StreamProviderRegistration registration = registrations.get(link.providerId());
        if (registration == null) {
            return Optional.empty();
        }
        return registration.metadataProvider().metadata(link);
    }

    public boolean linkable(StreamProviderId providerId) {
        return registrations.containsKey(providerId);
    }

    public List<String> linkableProviderIds() {
        return registrations.keySet().stream()
                .map(StreamProviderId::value)
                .sorted()
                .toList();
    }
}
