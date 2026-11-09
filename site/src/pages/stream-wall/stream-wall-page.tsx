import { RefreshCw } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import type { LiveStreamer } from "@/api/live-feed";
import { AppIconButton } from "@/components/app/app-icon-button";
import { StreamEmbed } from "@/pages/stream-wall/stream-embed";
import { computeStreamLayout } from "@/pages/stream-wall/stream-layout";
import { StreamTile } from "@/pages/stream-wall/stream-tile";
import { TallyLight } from "@/pages/stream-wall/tally-light";
import { useLiveFeed } from "@/pages/stream-wall/use-live-feed";
import { useViewportAspect } from "@/pages/stream-wall/use-viewport-aspect";
import { WallStatusScreen } from "@/pages/stream-wall/wall-status-screen";

/**
 * The product. A full-viewport multiviewer wall: every live streamer is a
 * monitor in a gap-free grid, click one to open it fullscreen, or fall back to a
 * broadcast test card when nothing is on air. No navbar, no sidebar — the first
 * viewport is the whole experience.
 */
export function StreamWallPage() {
  const { feed, status, source, error, refresh } = useLiveFeed();
  const viewportAspect = useViewportAspect();

  const streamers = useMemo(() => feed?.streamers ?? [], [feed]);
  const layout = useMemo(
    () => computeStreamLayout(streamers.length, viewportAspect),
    [streamers.length, viewportAspect],
  );

  const [focusedPlayer, setFocusedPlayer] = useState<string | null>(null);
  const focusedStreamer = useMemo(
    () => streamers.find((streamer) => streamer.playerName === focusedPlayer),
    [streamers, focusedPlayer],
  );

  const openStream = useCallback((streamer: LiveStreamer) => {
    setFocusedPlayer(streamer.playerName);
  }, []);

  // Restore focus to the originating monitor when returning to the wall.
  const closeStream = useCallback(() => {
    const player = focusedPlayer;
    setFocusedPlayer(null);
    if (player) {
      requestAnimationFrame(() => {
        const tile = document.querySelector<HTMLButtonElement>(
          `[data-player="${CSS.escape(player)}"]`,
        );
        tile?.focus();
      });
    }
  }, [focusedPlayer]);

  // Escape returns to the wall; close if the focused streamer drops off air.
  useEffect(() => {
    if (!focusedStreamer) {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        closeStream();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [focusedStreamer, closeStream]);

  useEffect(() => {
    if (focusedPlayer && !focusedStreamer && status === "ready") {
      setFocusedPlayer(null);
    }
  }, [focusedPlayer, focusedStreamer, status]);

  if (status === "loading" && streamers.length === 0) {
    return <WallStatusScreen kind="loading" onRefresh={refresh} />;
  }

  if (status === "error" && streamers.length === 0) {
    return (
      <WallStatusScreen
        kind="error"
        message={error ?? undefined}
        onRefresh={refresh}
      />
    );
  }

  if (streamers.length === 0) {
    return <WallStatusScreen kind="empty" onRefresh={refresh} />;
  }

  return (
    <main className="relative size-full overflow-hidden bg-black">
      <div
        className="grid size-full gap-0"
        style={{
          gridTemplateColumns: `repeat(${layout.columns}, minmax(0, 1fr))`,
          gridTemplateRows: `repeat(${layout.rows}, minmax(0, 1fr))`,
          gridAutoFlow: "row",
        }}
      >
        {streamers.map((streamer, index) => (
          <StreamTile
            key={streamer.playerName}
            streamer={streamer}
            channel={index + 1}
            colSpan={layout.tiles[index]?.colSpan ?? 1}
            onOpen={openStream}
          />
        ))}
      </div>

      {/* Floating transport cluster — centered so it clears the left-aligned
          monitor labels. Quiet by default, sharpens on hover/focus. */}
      <div className="absolute bottom-[max(0.75rem,env(safe-area-inset-bottom))] left-1/2 z-20 flex -translate-x-1/2 items-center gap-3 rounded-full bg-black/45 py-1.5 pr-1.5 pl-4 opacity-70 backdrop-blur-md transition-opacity duration-300 hover:opacity-100 focus-within:opacity-100">
        <TallyLight label="On Air" size="lg" />
        <span className="type-umd text-white/70">
          {streamers.length} {streamers.length === 1 ? "Source" : "Sources"}
        </span>
        {source === "fallback" ? (
          <span className="type-umd rounded-sm bg-umd/15 px-1.5 py-1 text-umd">
            Demo
          </span>
        ) : null}
        <AppIconButton
          label="Refresh feed"
          size="icon"
          onClick={refresh}
          tooltipSide="top"
          className="border-white/10"
        >
          <RefreshCw />
        </AppIconButton>
      </div>

      {focusedStreamer ? (
        <StreamEmbed streamer={focusedStreamer} onBack={closeStream} />
      ) : null}
    </main>
  );
}
