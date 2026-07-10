package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.StreamLink;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    public Map<StreamLink, VerificationResult> verifyAll(Collection<StreamLink> links) {
        Map<StreamProviderId, Map<StreamLink, StreamLink>> linksByProvider = new LinkedHashMap<>();
        for (StreamLink link : links) {
            linksByProvider
                    .computeIfAbsent(link.providerId(), ignored -> new LinkedHashMap<>())
                    .put(link, link);
        }

        Map<StreamLink, VerificationResult> results = new LinkedHashMap<>();
        for (Map.Entry<StreamProviderId, Map<StreamLink, StreamLink>> entry : linksByProvider.entrySet()) {
            StreamProviderRegistration registration = registrations.get(entry.getKey());
            if (registration == null) {
                for (StreamLink link : entry.getValue().keySet()) {
                    results.put(link, VerificationResult.offline(
                            link.providerId(),
                            "No verifier is configured for " + link.providerId().displayName() + "."
                    ));
                }
                continue;
            }

            Map<StreamLink, VerificationResult> providerResults = registration.verificationProvider()
                    .verifyAll(entry.getValue().keySet());
            for (StreamLink link : entry.getValue().keySet()) {
                results.put(link, providerResults.getOrDefault(
                        link,
                        VerificationResult.unavailable(link.providerId(), "Verification provider returned no result.")
                ));
            }
        }
        return Map.copyOf(results);
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
