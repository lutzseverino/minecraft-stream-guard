package com.lutzseverino.streamguard.domain;

import java.util.Optional;
import java.util.UUID;

public record PlayerAccessRecord(
    UUID playerId,
    String playerName,
    StreamLink streamLink,
    VerificationStatus verificationStatus,
    BypassGrant bypassGrant) {

  public PlayerAccessRecord {
    if (playerId == null) {
      throw new IllegalArgumentException("playerId cannot be null");
    }
    playerName = playerName == null ? "" : playerName;
  }

  public static PlayerAccessRecord empty(UUID playerId, String playerName) {
    return new PlayerAccessRecord(playerId, playerName, null, null, null);
  }

  public Optional<StreamLink> streamLinkOptional() {
    return Optional.ofNullable(streamLink);
  }

  public Optional<VerificationStatus> verificationStatusOptional() {
    return Optional.ofNullable(verificationStatus);
  }

  public Optional<BypassGrant> bypassGrantOptional() {
    return Optional.ofNullable(bypassGrant);
  }

  public PlayerAccessRecord withPlayerName(String name) {
    return new PlayerAccessRecord(playerId, name, streamLink, verificationStatus, bypassGrant);
  }

  public PlayerAccessRecord withStreamLink(StreamLink link) {
    return new PlayerAccessRecord(playerId, playerName, link, verificationStatus, bypassGrant);
  }

  public PlayerAccessRecord withVerificationStatus(VerificationStatus status) {
    return new PlayerAccessRecord(playerId, playerName, streamLink, status, bypassGrant);
  }

  public PlayerAccessRecord withBypassGrant(BypassGrant grant) {
    return new PlayerAccessRecord(playerId, playerName, streamLink, verificationStatus, grant);
  }

  public PlayerAccessRecord withoutBypassGrant() {
    return new PlayerAccessRecord(playerId, playerName, streamLink, verificationStatus, null);
  }
}
