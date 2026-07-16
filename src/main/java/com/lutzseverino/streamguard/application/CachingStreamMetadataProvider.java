package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.StreamLink;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CachingStreamMetadataProvider implements StreamMetadataProvider {

  private final StreamMetadataProvider delegate;
  private final Clock clock;
  private final Duration timeToLive;
  private final Map<StreamLink, CachedMetadata> cache = new ConcurrentHashMap<>();

  public CachingStreamMetadataProvider(
      StreamMetadataProvider delegate, Clock clock, Duration timeToLive) {
    this.delegate = delegate;
    this.clock = clock;
    this.timeToLive = timeToLive;
  }

  @Override
  public Optional<LiveStreamMetadata> metadata(StreamLink link) {
    Instant now = clock.instant();
    cache.entrySet().removeIf(entry -> !entry.getValue().validAt(now));
    CachedMetadata cached = cache.get(link);
    if (cached != null && cached.validAt(now)) {
      return cached.metadata();
    }
    Optional<LiveStreamMetadata> metadata = delegate.metadata(link);
    cache.put(link, new CachedMetadata(metadata, now.plus(timeToLive)));
    return metadata;
  }

  private record CachedMetadata(Optional<LiveStreamMetadata> metadata, Instant expiresAt) {

    private boolean validAt(Instant now) {
      return now.isBefore(expiresAt);
    }
  }
}
