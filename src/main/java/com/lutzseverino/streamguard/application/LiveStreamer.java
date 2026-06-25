package com.lutzseverino.streamguard.application;

public record LiveStreamer(
        String playerName,
        String provider,
        String channel,
        String url,
        String title,
        String thumbnailUrl,
        Integer viewerCount,
        String liveSince,
        LiveFeedEmbed embed
) {
}
