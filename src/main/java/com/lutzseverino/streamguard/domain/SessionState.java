package com.lutzseverino.streamguard.domain;

import java.time.Instant;
import java.util.UUID;

public record SessionState(UUID playerId, Instant joinedAt) {

  public SessionState {
    if (playerId == null) {
      throw new IllegalArgumentException("playerId cannot be null");
    }
    if (joinedAt == null) {
      throw new IllegalArgumentException("joinedAt cannot be null");
    }
  }
}
