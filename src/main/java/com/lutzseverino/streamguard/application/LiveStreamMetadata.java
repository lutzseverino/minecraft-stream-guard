package com.lutzseverino.streamguard.application;

import java.time.Instant;
import java.util.Optional;

public record LiveStreamMetadata(
        String channel,
        String title,
        String thumbnailUrl,
        Integer viewerCount,
        Instant liveSince,
        String url
) {

    public Optional<String> channelOptional() {
        return Optional.ofNullable(channel);
    }

    public Optional<String> titleOptional() {
        return Optional.ofNullable(title);
    }

    public Optional<String> thumbnailUrlOptional() {
        return Optional.ofNullable(thumbnailUrl);
    }

    public Optional<Instant> liveSinceOptional() {
        return Optional.ofNullable(liveSince);
    }

    public Optional<String> urlOptional() {
        return Optional.ofNullable(url);
    }
}
