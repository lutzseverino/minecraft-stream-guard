package com.lutzseverino.streamguard.application;

import java.time.Instant;
import java.util.List;

public record LiveFeedSnapshot(Instant updatedAt, List<LiveStreamer> streamers) {

  public LiveFeedSnapshot {
    if (updatedAt == null) {
      throw new IllegalArgumentException("updatedAt cannot be null");
    }
    streamers = List.copyOf(streamers);
  }
}
