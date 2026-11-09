package com.lutzseverino.streamguard.domain;

public record StreamLink(StreamProviderId providerId, String channel) {

  public StreamLink {
    if (providerId == null) {
      throw new IllegalArgumentException("providerId cannot be null");
    }
    if (channel == null || channel.isBlank()) {
      throw new IllegalArgumentException("channel cannot be blank");
    }
    channel = channel.trim();
  }
}
