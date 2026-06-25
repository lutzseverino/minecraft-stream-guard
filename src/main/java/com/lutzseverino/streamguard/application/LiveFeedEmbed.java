package com.lutzseverino.streamguard.application;

public record LiveFeedEmbed(String kind, String channel, String videoId, String channelId) {

    public static LiveFeedEmbed twitch(String channel) {
        return new LiveFeedEmbed("twitch", channel, null, null);
    }

    public static LiveFeedEmbed youtubeVideo(String videoId) {
        return new LiveFeedEmbed("youtube-video", null, videoId, null);
    }

    public static LiveFeedEmbed youtubeChannel(String channelId) {
        return new LiveFeedEmbed("youtube-channel", null, null, channelId);
    }

    public static LiveFeedEmbed link() {
        return new LiveFeedEmbed("link", null, null, null);
    }
}
