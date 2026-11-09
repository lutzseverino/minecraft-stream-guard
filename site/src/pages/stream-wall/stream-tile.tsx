import { Eye, Maximize2 } from "lucide-react";
import { useState } from "react";

import type { LiveStreamer } from "@/api/live-feed";
import {
  ProviderGlyph,
  providerAccentClass,
  providerLabel,
} from "@/components/custom/provider-glyph";
import { cn } from "@/lib/utils";
import {
  formatLiveDuration,
  formatViewers,
  hueFromName,
  monogram,
} from "@/pages/stream-wall/format";
import { ScreenOverlay } from "@/pages/stream-wall/screen-overlay";
import { TallyLight } from "@/pages/stream-wall/tally-light";

interface StreamTileProps {
  streamer: LiveStreamer;
  /** 1-based input number for this monitor (CH 01, CH 02, …). */
  channel: number;
  /** Grid column span for this tile (from the layout math). */
  colSpan: number;
  onOpen: (streamer: LiveStreamer) => void;
}

/**
 * One monitor on the multiviewer wall. Renders the "source" treatment first — a
 * provider thumbnail or a generated screen surface — with broadcast chrome:
 * a tally light, a channel number, and an under-monitor display. The real player
 * only loads once opened. Acts as a button: click / Enter / Space opens it.
 */
export function StreamTile({
  streamer,
  channel,
  colSpan,
  onOpen,
}: StreamTileProps) {
  const [thumbFailed, setThumbFailed] = useState(false);
  const hue = hueFromName(streamer.playerName);
  const showThumb = Boolean(streamer.thumbnailUrl) && !thumbFailed;
  const duration = streamer.liveSince
    ? formatLiveDuration(streamer.liveSince)
    : null;
  const channelId = `CH ${String(channel).padStart(2, "0")}`;

  const accessibleName = [
    `Open ${streamer.playerName} on ${providerLabel(streamer.provider)}`,
    `channel ${channelId}`,
    streamer.title ? `— ${streamer.title}` : "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button
      type="button"
      data-player={streamer.playerName}
      onClick={() => onOpen(streamer)}
      aria-label={accessibleName}
      style={{ gridColumn: `span ${colSpan} / span ${colSpan}` }}
      className="group relative isolate block size-full overflow-hidden bg-black text-left outline-none ring-inset focus-visible:z-10 focus-visible:ring-2 focus-visible:ring-ring"
    >
      {/* Source surface: thumbnail when available, otherwise a generated signal. */}
      {showThumb ? (
        <img
          src={streamer.thumbnailUrl}
          alt=""
          loading="lazy"
          onError={() => setThumbFailed(true)}
          className="absolute inset-0 size-full object-cover transition-transform duration-700 ease-out group-hover:scale-[1.03] motion-reduce:transition-none motion-reduce:group-hover:scale-100"
        />
      ) : (
        <div
          className="screen-surface absolute inset-0 transition-transform duration-700 ease-out group-hover:scale-[1.03] motion-reduce:transition-none motion-reduce:group-hover:scale-100"
          style={{ ["--screen-hue" as string]: String(hue) }}
        >
          <span
            aria-hidden="true"
            className="font-display absolute inset-0 flex items-center justify-center text-[30vmin] leading-none text-white/[0.06] select-none"
          >
            {monogram(streamer.playerName)}
          </span>
        </div>
      )}

      <ScreenOverlay />

      {/* Top: tally light + channel id / provider, always on but quiet. */}
      <div className="pointer-events-none absolute inset-x-0 top-0 flex items-start justify-between p-3 sm:p-3.5">
        <TallyLight label="Live" />
        <div className="flex items-center gap-2">
          <span className="type-umd text-umd drop-shadow">{channelId}</span>
          <ProviderGlyph
            provider={streamer.provider}
            className={cn(
              "size-4 drop-shadow",
              providerAccentClass(streamer.provider),
            )}
          />
        </div>
      </div>

      {/* Center: expand affordance, revealed on proximity (hover / focus). */}
      <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
        <span className="flex size-14 scale-90 items-center justify-center rounded-full border border-white/20 bg-black/40 text-white opacity-0 backdrop-blur-md transition-all duration-300 group-hover:scale-100 group-hover:opacity-100 group-focus-visible:scale-100 group-focus-visible:opacity-100 motion-reduce:scale-100 motion-reduce:transition-none">
          <Maximize2 className="size-5" />
        </span>
      </div>

      {/* Under-monitor display: name always; title + meta reveal on proximity. */}
      <div className="pointer-events-none absolute inset-x-0 bottom-0 flex flex-col gap-1.5 p-3 sm:p-4">
        <h2 className="truncate text-lg font-semibold tracking-tight text-white drop-shadow-md sm:text-xl">
          {streamer.playerName}
        </h2>
        <div className="grid grid-rows-[0fr] opacity-0 transition-all duration-300 group-hover:grid-rows-[1fr] group-hover:opacity-100 group-focus-visible:grid-rows-[1fr] group-focus-visible:opacity-100 motion-reduce:transition-none">
          <div className="flex min-h-0 flex-col gap-1.5 overflow-hidden">
            {streamer.title ? (
              <p className="line-clamp-2 text-sm text-white/85 drop-shadow">
                {streamer.title}
              </p>
            ) : null}
            <div className="type-umd flex items-center gap-3 text-white/65">
              <span>{providerLabel(streamer.provider)}</span>
              {typeof streamer.viewerCount === "number" ? (
                <span className="inline-flex items-center gap-1">
                  <Eye className="size-3" aria-hidden="true" />
                  {formatViewers(streamer.viewerCount)}
                </span>
              ) : null}
              {duration ? <span>{duration}</span> : null}
            </div>
          </div>
        </div>
      </div>
    </button>
  );
}
