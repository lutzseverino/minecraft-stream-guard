/**
 * Provider-specific embed logic, isolated from React.
 *
 * Twitch's player iframe requires a `parent` query param listing the host
 * domain(s) it is embedded under. Rather than expose a runtime config file, the
 * parent hosts are derived from the current location at call time, with a
 * source-level fallback list below for non-browser contexts.
 */
import type { StreamEmbed } from "@/api/live-feed";

/**
 * Hosts always permitted as Twitch embed parents, regardless of where the wall
 * is served. Add your production domain(s) here if you ever pre-render or embed
 * the wall somewhere the runtime hostname can't be detected.
 */
export const TWITCH_PARENT_FALLBACK_HOSTS = ["localhost"] as const;

/** Resolve the Twitch `parent` host list for the current environment. */
export function resolveTwitchParents(): string[] {
  const hosts = new Set<string>(TWITCH_PARENT_FALLBACK_HOSTS);
  if (typeof window !== "undefined" && window.location.hostname) {
    hosts.add(window.location.hostname);
  }
  return [...hosts];
}

export interface EmbedTarget {
  /** Iframe `src` when the stream can be embedded inline. */
  iframeSrc?: string;
  /** True when only an outbound link is possible (no inline player). */
  linkOnly: boolean;
  /** Whether the iframe needs `allow="autoplay"` / fullscreen permissions. */
  allowAutoplay: boolean;
}

interface BuildEmbedOptions {
  /** Autoplay + unmute when a tile is opened fullscreen. Defaults to true. */
  autoplay?: boolean;
  /** Twitch parent hosts. Defaults to {@link resolveTwitchParents}. */
  twitchParents?: string[];
}

/**
 * Turn provider embed metadata into a concrete iframe target. Returns
 * `linkOnly` when no inline player is available (e.g. a YouTube channel with no
 * known video id) so the UI can degrade to a provider link instead of breaking.
 */
export function buildEmbedTarget(
  embed: StreamEmbed,
  options: BuildEmbedOptions = {},
): EmbedTarget {
  const autoplay = options.autoplay ?? true;

  switch (embed.kind) {
    case "twitch": {
      const parents = options.twitchParents ?? resolveTwitchParents();
      const params = new URLSearchParams({
        channel: embed.channel,
        muted: "false",
        autoplay: String(autoplay),
      });
      for (const parent of parents) {
        params.append("parent", parent);
      }
      return {
        iframeSrc: `https://player.twitch.tv/?${params.toString()}`,
        linkOnly: false,
        allowAutoplay: true,
      };
    }

    case "youtube-video": {
      const params = new URLSearchParams({
        autoplay: autoplay ? "1" : "0",
        rel: "0",
        modestbranding: "1",
      });
      return {
        iframeSrc: `https://www.youtube.com/embed/${embed.videoId}?${params.toString()}`,
        linkOnly: false,
        allowAutoplay: true,
      };
    }

    case "youtube-channel": {
      const params = new URLSearchParams({
        channel: embed.channelId,
        autoplay: autoplay ? "1" : "0",
        rel: "0",
        modestbranding: "1",
      });
      return {
        iframeSrc: `https://www.youtube.com/embed/live_stream?${params.toString()}`,
        linkOnly: false,
        allowAutoplay: true,
      };
    }

    case "link":
      return { linkOnly: true, allowAutoplay: false };
  }
}
