package com.lutzseverino.streamguard.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutzseverino.streamguard.domain.StreamProviderId;
import java.util.List;
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
}
