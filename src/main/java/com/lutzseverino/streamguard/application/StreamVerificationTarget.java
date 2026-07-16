package com.lutzseverino.streamguard.application;

import java.util.UUID;

public record StreamVerificationTarget(UUID playerId, String playerName) {

  public StreamVerificationTarget {
    if (playerId == null) {
      throw new IllegalArgumentException("playerId cannot be null");
    }
    playerName = playerName == null ? "" : playerName;
  }
}
