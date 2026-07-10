package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.StreamLink;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public final class CachingStreamVerificationProvider implements StreamVerificationProvider {

    private final StreamVerificationProvider delegate;
    private final Clock clock;
    private final Duration liveTimeToLive;
    private final Duration offlineTimeToLive;
    private final Map<StreamLink, CachedVerification> cache = new ConcurrentHashMap<>();
    private final Map<StreamLink, CompletableFuture<VerificationResult>> inFlight = new ConcurrentHashMap<>();

    public CachingStreamVerificationProvider(
            StreamVerificationProvider delegate,
            Clock clock,
            Duration liveTimeToLive,
            Duration offlineTimeToLive
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.clock = Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(liveTimeToLive, "liveTimeToLive");
        Objects.requireNonNull(offlineTimeToLive, "offlineTimeToLive");
        this.liveTimeToLive = liveTimeToLive.isNegative() ? Duration.ZERO : liveTimeToLive;
        this.offlineTimeToLive = offlineTimeToLive.isNegative() ? Duration.ZERO : offlineTimeToLive;
    }

    @Override
    public VerificationResult verify(StreamLink link) {
        return verifyAll(java.util.List.of(link)).get(link);
    }

    @Override
    public Map<StreamLink, VerificationResult> verifyAll(Collection<StreamLink> links) {
        Instant now = clock.instant();
        cache.entrySet().removeIf(entry -> !entry.getValue().validAt(now));

        Map<StreamLink, VerificationResult> results = new LinkedHashMap<>();
        Map<StreamLink, StreamLink> misses = new LinkedHashMap<>();
        for (StreamLink link : links) {
            if (results.containsKey(link)) {
                continue;
            }
            CachedVerification cached = cache.get(link);
            if (cached != null && cached.validAt(now)) {
                results.put(link, cached.result());
            } else {
                misses.put(link, link);
            }
        }

        if (misses.isEmpty()) {
            return Map.copyOf(results);
        }

        Map<StreamLink, CompletableFuture<VerificationResult>> futures = new LinkedHashMap<>();
        Set<StreamLink> ownedMisses = new HashSet<>();
        for (StreamLink link : misses.keySet()) {
            CompletableFuture<VerificationResult> created = new CompletableFuture<>();
            CompletableFuture<VerificationResult> existing = inFlight.putIfAbsent(link, created);
            if (existing == null) {
                futures.put(link, created);
                ownedMisses.add(link);
            } else {
                futures.put(link, existing);
            }
        }

        if (!ownedMisses.isEmpty()) {
            completeOwnedMisses(ownedMisses, futures);
        }

        for (Map.Entry<StreamLink, CompletableFuture<VerificationResult>> entry : futures.entrySet()) {
            results.put(entry.getKey(), join(entry.getKey(), entry.getValue()));
        }
        return Map.copyOf(results);
    }

    private void completeOwnedMisses(
            Set<StreamLink> ownedMisses,
            Map<StreamLink, CompletableFuture<VerificationResult>> futures
    ) {
        try {
            Map<StreamLink, VerificationResult> freshResults = delegate.verifyAll(ownedMisses);
            for (StreamLink link : ownedMisses) {
                VerificationResult result = freshResults.getOrDefault(
                        link,
                        VerificationResult.unavailable(link.providerId(), "Verification provider returned no result.")
                );
                cache(link, result);
                futures.get(link).complete(result);
            }
        } catch (RuntimeException exception) {
            for (StreamLink link : ownedMisses) {
                VerificationResult result = VerificationResult.unavailable(
                        link.providerId(),
                        "Verification provider failed: " + exception.getMessage()
                );
                futures.get(link).complete(result);
            }
        } finally {
            for (StreamLink link : ownedMisses) {
                inFlight.remove(link, futures.get(link));
            }
        }
    }

    private VerificationResult join(StreamLink link, CompletableFuture<VerificationResult> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            return VerificationResult.unavailable(link.providerId(), "Verification provider failed.");
        }
    }

    private void cache(StreamLink link, VerificationResult result) {
        if (!result.available()) {
            cache.remove(link);
            return;
        }
        Duration timeToLive = result.live() ? liveTimeToLive : offlineTimeToLive;
        if (timeToLive.isZero()) {
            cache.remove(link);
            return;
        }
        cache.put(link, new CachedVerification(result, clock.instant().plus(timeToLive)));
    }

    private record CachedVerification(VerificationResult result, Instant expiresAt) {

        private boolean validAt(Instant now) {
            return now.isBefore(expiresAt);
        }
    }
}
