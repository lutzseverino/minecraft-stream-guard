package com.lutzseverino.streamguard.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutzseverino.streamguard.domain.StreamLink;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class StreamProviderRegistryTest {

    @Test
    void delegatesProviderSpecificLinkNormalization() {
        StreamProviderId discord = new StreamProviderId("discord");
        StreamProviderRegistry registry = new StreamProviderRegistry(List.of(
                new StreamProviderRegistration(
                        discord,
                        link -> VerificationResult.offline(link.providerId(), "offline"),
                        value -> value.trim().toLowerCase(java.util.Locale.ROOT)
                )
        ));

        assertTrue(registry.linkable(discord));
        assertEquals("someuser", registry.normalize(discord, " SomeUser "));
    }

    @Test
    void delegatesProviderSpecificMetadata() {
        StreamProviderId discord = new StreamProviderId("discord");
        StreamProviderRegistry registry = new StreamProviderRegistry(List.of(
                new StreamProviderRegistration(
                        discord,
                        link -> VerificationResult.live(link.providerId(), "live"),
                        link -> Optional.of(new LiveStreamMetadata(
                                "someuser",
                                "Stage",
                                "https://example.com/stage.jpg",
                                7,
                                null,
                                "https://discord.example/live"
                        )),
                        String::trim
                )
        ));

        LiveStreamMetadata metadata = registry.metadata(new StreamLink(
                discord,
                "someuser"
        )).orElseThrow();

        assertEquals("Stage", metadata.title());
        assertEquals("https://example.com/stage.jpg", metadata.thumbnailUrl());
        assertEquals(7, metadata.viewerCount());
    }
}
