package com.lutzseverino.streamguard.application;

import com.lutzseverino.streamguard.domain.PlayerAccessRecord;
import com.lutzseverino.streamguard.domain.StreamLink;
import com.lutzseverino.streamguard.domain.StreamProviderId;
import com.lutzseverino.streamguard.domain.VerificationStatus;
import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LiveFeedService {

  private static final Pattern YOUTUBE_WATCH_URL = Pattern.compile("[?&]v=([^&]+)");
  private static final Pattern YOUTUBE_SHORT_URL = Pattern.compile("youtu\\.be/([^/?#]+)");
  private static final Pattern YOUTUBE_LIVE_URL = Pattern.compile("youtube\\.com/live/([^/?#]+)");

  private final PlayerAccessRepository repository;
  private final StreamMetadataProvider metadataProvider;
  private final Clock clock;
  private final Duration maximumStatusAge;

  public LiveFeedService(PlayerAccessRepository repository, Clock clock) {
    this(repository, StreamMetadataProvider.none(), clock, Duration.ofMinutes(3));
  }

  public LiveFeedService(
      PlayerAccessRepository repository, StreamMetadataProvider metadataProvider, Clock clock) {
    this(repository, metadataProvider, clock, Duration.ofMinutes(3));
  }

  public LiveFeedService(
      PlayerAccessRepository repository,
      StreamMetadataProvider metadataProvider,
      Clock clock,
      Duration maximumStatusAge) {
    this.repository = repository;
    this.metadataProvider = metadataProvider;
    this.clock = clock;
    this.maximumStatusAge = maximumStatusAge;
  }

  public LiveFeedSnapshot snapshot(Collection<LiveFeedPlayer> players) {
    return new LiveFeedSnapshot(
        clock.instant(),
        players.stream()
            .map(this::liveStreamer)
            .flatMap(Optional::stream)
            .sorted(Comparator.comparing(LiveStreamer::playerName, String.CASE_INSENSITIVE_ORDER))
            .toList());
  }

  private Optional<LiveStreamer> liveStreamer(LiveFeedPlayer player) {
    PlayerAccessRecord accessRecord =
        repository.getOrCreate(player.playerId(), player.playerName());
    Optional<StreamLink> link = accessRecord.streamLinkOptional();
    Optional<VerificationStatus> status = accessRecord.verificationStatusOptional();
    if (link.isEmpty()
        || status
            .filter(value -> value.grantsAccessAt(clock.instant(), maximumStatusAge))
            .isEmpty()) {
      return Optional.empty();
    }
    StreamLink streamLink = link.get();
    if (StreamProviderId.MANUAL.equals(streamLink.providerId())) {
      return Optional.empty();
    }
    String provider = streamLink.providerId().value();
    String channel = streamLink.channel();
    Optional<LiveStreamMetadata> metadata = metadataProvider.metadata(streamLink);
    String feedChannel = metadata.flatMap(LiveStreamMetadata::channelOptional).orElse(channel);
    String url =
        metadata.flatMap(LiveStreamMetadata::urlOptional).orElseGet(() -> providerUrl(streamLink));
    String embedReference =
        StreamProviderId.TWITCH.equals(streamLink.providerId()) ? feedChannel : url;
    return Optional.of(
        new LiveStreamer(
            player.playerName(),
            provider,
            feedChannel,
            url,
            metadata.flatMap(LiveStreamMetadata::titleOptional).orElse(null),
            metadata.flatMap(LiveStreamMetadata::thumbnailUrlOptional).orElse(null),
            metadata.map(LiveStreamMetadata::viewerCount).orElse(null),
            metadata
                .flatMap(LiveStreamMetadata::liveSinceOptional)
                .map(java.time.Instant::toString)
                .orElse(null),
            embed(new StreamLink(streamLink.providerId(), embedReference))));
  }

  private static String providerUrl(StreamLink link) {
    String channel = link.channel();
    if (channel.startsWith("http://") || channel.startsWith("https://")) {
      return channel;
    }
    if (StreamProviderId.TWITCH.equals(link.providerId())) {
      return "https://twitch.tv/" + channel;
    }
    if (StreamProviderId.YOUTUBE.equals(link.providerId())) {
      if (channel.startsWith("@")) {
        return "https://youtube.com/" + channel;
      }
      if (channel.startsWith("UC")) {
        return "https://youtube.com/channel/" + channel;
      }
      return "https://youtube.com/" + channel;
    }
    return channel;
  }

  private static LiveFeedEmbed embed(StreamLink link) {
    String channel = link.channel();
    if (StreamProviderId.TWITCH.equals(link.providerId())) {
      return LiveFeedEmbed.twitch(channel);
    }
    if (StreamProviderId.YOUTUBE.equals(link.providerId())) {
      Optional<String> videoId = youtubeVideoId(channel);
      if (videoId.isPresent()) {
        return LiveFeedEmbed.youtubeVideo(videoId.get());
      }
      if (channel.startsWith("UC")) {
        return LiveFeedEmbed.youtubeChannel(channel);
      }
    }
    return LiveFeedEmbed.link();
  }

  private static Optional<String> youtubeVideoId(String value) {
    return firstMatch(YOUTUBE_WATCH_URL, value)
        .or(() -> firstMatch(YOUTUBE_SHORT_URL, value))
        .or(() -> firstMatch(YOUTUBE_LIVE_URL, value));
  }

  private static Optional<String> firstMatch(Pattern pattern, String value) {
    Matcher matcher = pattern.matcher(value);
    return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
  }
}
