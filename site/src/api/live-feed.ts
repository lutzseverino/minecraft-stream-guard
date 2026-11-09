/**
 * StreamGuard live-feed domain model and client.
 *
 * This module is intentionally framework-free: pure types plus a small fetch
 * client. Nothing here imports React, so the feed contract can be reused, tested,
 * or swapped without touching the presentation layer.
 */

export type StreamProvider = "twitch" | "youtube";

/**
 * Provider-specific metadata needed to embed a stream. Kept as a discriminated
 * union so the embed layer can decide, per provider, whether a real player can
 * be shown or whether we must fall back to an outbound link.
 */
export type StreamEmbed =
  /** Twitch player, keyed by channel login. */
  | { kind: "twitch"; channel: string }
  /** A specific YouTube video / live broadcast id — directly embeddable. */
  | { kind: "youtube-video"; videoId: string }
  /** A YouTube channel id — embeddable via the channel live_stream player. */
  | { kind: "youtube-channel"; channelId: string }
  /** Nothing embeddable is known; presentation should link out instead. */
  | { kind: "link" };

/** A single streamer that StreamGuard has detected as live. */
export interface LiveStreamer {
  /** Minecraft / in-game player name. Stable id for the tile. */
  playerName: string;
  provider: StreamProvider;
  /** Channel handle / login / display name on the provider. */
  channel: string;
  /** Canonical watch URL on the provider. */
  url: string;
  title?: string;
  /** Provider-supplied preview image, when available. */
  thumbnailUrl?: string;
  viewerCount?: number;
  /** ISO-8601 timestamp of when the stream went live. */
  liveSince?: string;
  embed: StreamEmbed;
}

/** Normalized snapshot of who is live right now. */
export interface LiveFeed {
  /** ISO-8601 timestamp of when this snapshot was produced. */
  updatedAt: string;
  streamers: LiveStreamer[];
}

const DEFAULT_ENDPOINT = "/api/live";

export interface LiveFeedClientOptions {
  /** Feed endpoint. Defaults to `/api/live`. */
  endpoint?: string;
  /** Override the global fetch (tests, custom transports). */
  fetchImpl?: typeof fetch;
  /**
   * Demo feed used when the real endpoint is unreachable. Only consulted when
   * {@link LiveFeedClientOptions.useFallbackOnError} is true (dev by default),
   * so production never silently shows fake streamers.
   */
  fallbackFeed?: () => LiveFeed;
  useFallbackOnError?: boolean;
}

export class LiveFeedError extends Error {
  constructor(
    message: string,
    readonly cause?: unknown,
  ) {
    super(message);
    this.name = "LiveFeedError";
  }
}

/** Result of a fetch, tagged with where the data came from. */
export interface LiveFeedResult {
  feed: LiveFeed;
  source: "network" | "fallback";
}

export class LiveFeedClient {
  private readonly endpoint: string;
  private readonly fetchImpl: typeof fetch;
  private readonly fallbackFeed?: () => LiveFeed;
  private readonly useFallbackOnError: boolean;

  constructor(options: LiveFeedClientOptions = {}) {
    this.endpoint = options.endpoint ?? DEFAULT_ENDPOINT;
    this.fetchImpl = options.fetchImpl ?? globalThis.fetch.bind(globalThis);
    this.fallbackFeed = options.fallbackFeed;
    this.useFallbackOnError = options.useFallbackOnError ?? false;
  }

  async fetchLive(signal?: AbortSignal): Promise<LiveFeedResult> {
    try {
      const response = await this.fetchImpl(this.endpoint, {
        signal,
        headers: { accept: "application/json" },
      });

      if (!response.ok) {
        throw new LiveFeedError(
          `Live feed responded with ${response.status} ${response.statusText}`,
        );
      }

      const raw: unknown = await response.json();
      return { feed: normalizeFeed(raw), source: "network" };
    } catch (error) {
      if (signal?.aborted) {
        throw error;
      }

      if (this.useFallbackOnError && this.fallbackFeed) {
        return { feed: this.fallbackFeed(), source: "fallback" };
      }

      throw error instanceof LiveFeedError
        ? error
        : new LiveFeedError("Unable to reach the StreamGuard live feed", error);
    }
  }
}

/* ------------------------------------------------------------------ */
/* Normalization — tolerate loosely-typed JSON from the plugin's API.  */
/* ------------------------------------------------------------------ */

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function asString(value: unknown): string | undefined {
  return typeof value === "string" && value.length > 0 ? value : undefined;
}

function asNumber(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value)
    ? value
    : undefined;
}

function normalizeProvider(value: unknown): StreamProvider | undefined {
  return value === "twitch" || value === "youtube" ? value : undefined;
}

/**
 * Derive embed metadata from a raw streamer record. Prefers an explicit `embed`
 * object, then falls back to inferring it from the provider + channel/url.
 */
function normalizeEmbed(
  raw: Record<string, unknown>,
  provider: StreamProvider,
  channel: string,
  url: string,
): StreamEmbed {
  const embed = isRecord(raw.embed) ? raw.embed : undefined;

  if (provider === "twitch") {
    const channelLogin =
      asString(embed?.channel) ??
      asString(channel) ??
      twitchChannelFromUrl(url);
    return channelLogin
      ? { kind: "twitch", channel: channelLogin.toLowerCase() }
      : { kind: "link" };
  }

  // YouTube: a concrete video id is best; a channel id still embeds; otherwise link.
  const videoId =
    asString(embed?.videoId) ?? youTubeVideoIdFromUrl(url) ?? undefined;
  if (videoId) {
    return { kind: "youtube-video", videoId };
  }

  const channelId = asString(embed?.channelId);
  if (channelId) {
    return { kind: "youtube-channel", channelId };
  }

  return { kind: "link" };
}

function normalizeStreamer(value: unknown): LiveStreamer | undefined {
  if (!isRecord(value)) {
    return undefined;
  }

  const provider = normalizeProvider(value.provider);
  const playerName = asString(value.playerName);
  const channel = asString(value.channel) ?? "";
  const url = asString(value.url) ?? "";

  if (!provider || !playerName || (!channel && !url)) {
    return undefined;
  }

  return {
    playerName,
    provider,
    channel,
    url: url || providerWatchUrl(provider, channel),
    title: asString(value.title),
    thumbnailUrl: asString(value.thumbnailUrl),
    viewerCount: asNumber(value.viewerCount),
    liveSince: asString(value.liveSince),
    embed: normalizeEmbed(value, provider, channel, url),
  };
}

export function normalizeFeed(raw: unknown): LiveFeed {
  if (!isRecord(raw)) {
    throw new LiveFeedError("Live feed payload must be a JSON object.");
  }

  if (!Array.isArray(raw.streamers)) {
    throw new LiveFeedError(
      "Live feed payload must contain a streamers array.",
    );
  }

  const streamers = raw.streamers.map((streamer, index) => {
    const normalized = normalizeStreamer(streamer);
    if (!normalized) {
      throw new LiveFeedError(
        `Live feed streamer at index ${index} is invalid.`,
      );
    }
    return normalized;
  });

  return {
    updatedAt: asString(raw.updatedAt) ?? new Date().toISOString(),
    streamers,
  };
}

/* --------------------------- URL helpers --------------------------- */

function providerWatchUrl(provider: StreamProvider, channel: string): string {
  return provider === "twitch"
    ? `https://twitch.tv/${channel}`
    : `https://youtube.com/${channel.startsWith("@") ? channel : `@${channel}`}`;
}

function twitchChannelFromUrl(url: string): string | undefined {
  const match = url.match(/twitch\.tv\/([^/?#]+)/i);
  return match?.[1];
}

function youTubeVideoIdFromUrl(url: string): string | undefined {
  const watch = url.match(/[?&]v=([^&]+)/);
  if (watch?.[1]) {
    return watch[1];
  }
  const short = url.match(/youtu\.be\/([^/?#]+)/);
  if (short?.[1]) {
    return short[1];
  }
  const live = url.match(/youtube\.com\/live\/([^/?#]+)/);
  return live?.[1];
}
