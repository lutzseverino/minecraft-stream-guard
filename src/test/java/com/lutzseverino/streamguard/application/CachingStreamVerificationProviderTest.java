package com.lutzseverino.streamguard.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lutzseverino.streamguard.domain.StreamLink;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class CachingStreamVerificationProviderTest {

    private static final Instant NOW = Instant.parse("2026-06-26T12:00:00Z");

    @Test
    void reusesCachedBatchResultsForRepeatedLinks() {
        StreamLink twitch = new StreamLink(StreamProviderId.TWITCH, "lutzseverino");
        CountingProvider delegate = new CountingProvider();
        CachingStreamVerificationProvider provider = new CachingStreamVerificationProvider(
                delegate,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(60),
                Duration.ofSeconds(60)
        );

        provider.verifyAll(java.util.List.of(twitch, twitch));
        provider.verify(twitch);

        assertEquals(1, delegate.calls.get());
    }

    @Test
    void doesNotCacheUnavailableResults() {
        StreamLink twitch = new StreamLink(StreamProviderId.TWITCH, "lutzseverino");
        AtomicInteger calls = new AtomicInteger();
        StreamVerificationProvider delegate = new StreamVerificationProvider() {
            @Override
            public VerificationResult verify(StreamLink link) {
                calls.incrementAndGet();
                return VerificationResult.unavailable(link.providerId(), "provider unavailable");
            }

            @Override
            public Map<StreamLink, VerificationResult> verifyAll(Collection<StreamLink> links) {
                calls.incrementAndGet();
                return links.stream().collect(java.util.stream.Collectors.toMap(
                        link -> link,
                        link -> VerificationResult.unavailable(link.providerId(), "provider unavailable")
                ));
            }
        };
        CachingStreamVerificationProvider provider = new CachingStreamVerificationProvider(
                delegate,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(60),
                Duration.ofSeconds(60)
        );

        provider.verify(twitch);
        provider.verify(twitch);

        assertEquals(2, calls.get());
    }

    @Test
    void coalescesConcurrentMissesForSameLink() throws Exception {
        StreamLink twitch = new StreamLink(StreamProviderId.TWITCH, "lutzseverino");
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        StreamVerificationProvider delegate = new StreamVerificationProvider() {
            @Override
            public VerificationResult verify(StreamLink link) {
                return VerificationResult.live(link.providerId(), "live");
            }

            @Override
            public Map<StreamLink, VerificationResult> verifyAll(Collection<StreamLink> links) {
                calls.incrementAndGet();
                started.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                return links.stream().collect(java.util.stream.Collectors.toMap(
                        link -> link,
                        link -> VerificationResult.live(link.providerId(), "live")
                ));
            }
        };
        CachingStreamVerificationProvider provider = new CachingStreamVerificationProvider(
                delegate,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(60),
                Duration.ofSeconds(60)
        );
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<VerificationResult> first = executor.submit(() -> provider.verify(twitch));
            started.await(5, TimeUnit.SECONDS);
            Future<VerificationResult> second = executor.submit(() -> provider.verify(twitch));
            release.countDown();

            assertEquals("live", first.get(5, TimeUnit.SECONDS).detail());
            assertEquals("live", second.get(5, TimeUnit.SECONDS).detail());
            assertEquals(1, calls.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class CountingProvider implements StreamVerificationProvider {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public VerificationResult verify(StreamLink link) {
            calls.incrementAndGet();
            return VerificationResult.live(link.providerId(), "live");
        }

        @Override
        public Map<StreamLink, VerificationResult> verifyAll(Collection<StreamLink> links) {
            calls.incrementAndGet();
            return links.stream().collect(java.util.stream.Collectors.toMap(
                    link -> link,
                    link -> VerificationResult.live(link.providerId(), "live")
            ));
        }
    }
}
