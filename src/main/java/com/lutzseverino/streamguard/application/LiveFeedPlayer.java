package com.lutzseverino.streamguard.application;

import java.util.UUID;

public record LiveFeedPlayer(UUID playerId, String playerName) {

  public LiveFeedPlayer {
    if (playerId == null) {
      throw new IllegalArgumentException("playerId cannot be null");
    }
    playerName = playerName == null || playerName.isBlank() ? "Unknown" : playerName.trim();
  }
}
