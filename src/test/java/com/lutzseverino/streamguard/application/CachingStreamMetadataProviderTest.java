package com.lutzseverino.streamguard.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lutzseverino.streamguard.domain.StreamLink;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class CachingStreamMetadataProviderTest {

  private static final StreamLink LINK = new StreamLink(StreamProviderId.TWITCH, "lutzseverino");

  @Test
  void cachesMetadataUntilTtlExpires() {
    AtomicInteger calls = new AtomicInteger();
    StreamMetadataProvider delegate =
        link ->
            Optional.of(
                new LiveStreamMetadata(
                    link.channel(), "Title " + calls.incrementAndGet(), null, null, null, null));
    MutableClock clock = new MutableClock(Instant.parse("2026-06-26T12:00:00Z"));
    CachingStreamMetadataProvider provider =
        new CachingStreamMetadataProvider(delegate, clock, Duration.ofSeconds(60));

    assertEquals("Title 1", provider.metadata(LINK).orElseThrow().title());
    assertEquals("Title 1", provider.metadata(LINK).orElseThrow().title());
    assertEquals(1, calls.get());

    clock.advance(Duration.ofSeconds(61));

    assertEquals("Title 2", provider.metadata(LINK).orElseThrow().title());
    assertEquals(2, calls.get());
  }

  private static final class MutableClock extends java.time.Clock {

    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public java.time.Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }

    private void advance(Duration duration) {
      instant = instant.plus(duration);
    }
  }
}
