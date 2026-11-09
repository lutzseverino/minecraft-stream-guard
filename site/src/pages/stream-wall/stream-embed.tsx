import { ArrowLeft, ExternalLink } from "lucide-react";
import { useMemo } from "react";

import { buildEmbedTarget } from "@/api/embeds";
import type { LiveStreamer } from "@/api/live-feed";
import { AppButton } from "@/components/app/app-button";
import { AppIconButton } from "@/components/app/app-icon-button";
import {
  ProviderGlyph,
  providerAccentClass,
  providerLabel,
} from "@/components/custom/provider-glyph";
import { StationIdent } from "@/pages/stream-wall/station-ident";

interface StreamEmbedProps {
  streamer: LiveStreamer;
  onBack: () => void;
}

/**
 * Fullscreen player for a single opened stream. Loads the real Twitch / YouTube
 * iframe and floats a back control over it. When no inline embed is possible
 * (e.g. a YouTube channel with no known live video id), it degrades to a clear
 * provider link instead of a broken frame. Video is never proxied — the iframe
 * points straight at the provider.
 */
export function StreamEmbed({ streamer, onBack }: StreamEmbedProps) {
  const target = useMemo(
    () => buildEmbedTarget(streamer.embed),
    [streamer.embed],
  );

  return (
    <div className="absolute inset-0 z-30 bg-black">
      {target.iframeSrc ? (
        <iframe
          key={streamer.playerName}
          title={`${streamer.playerName} on ${providerLabel(streamer.provider)}`}
          src={target.iframeSrc}
          className="size-full border-0"
          allow="autoplay; fullscreen; encrypted-media; picture-in-picture"
          allowFullScreen
        />
      ) : (
        <LinkFallback streamer={streamer} />
      )}

      {/* Floating back control — returns to the wall. */}
      <div className="absolute top-[max(1rem,env(safe-area-inset-top))] left-[max(1rem,env(safe-area-inset-left))] z-10">
        <AppIconButton
          label="Back to wall"
          onClick={onBack}
          tooltipSide="right"
        >
          <ArrowLeft />
        </AppIconButton>
      </div>

      <StationIdent streamer={streamer} />
    </div>
  );
}

function LinkFallback({ streamer }: { streamer: LiveStreamer }) {
  return (
    <div className="flex size-full flex-col items-center justify-center gap-6 px-6 text-center">
      <ProviderGlyph
        provider={streamer.provider}
        className={`size-12 ${providerAccentClass(streamer.provider)}`}
      />
      <div className="space-y-2">
        <p className="text-lg font-semibold text-white">
          {streamer.playerName} is live on {providerLabel(streamer.provider)}
        </p>
        <p className="type-mono-label text-white/50">
          No inline player available for this channel
        </p>
      </div>
      <AppButton
        nativeButton={false}
        // Base UI gives non-native buttons role="button"; keep link semantics.
        role="link"
        render={
          <a href={streamer.url} target="_blank" rel="noreferrer noopener" />
        }
      >
        Watch on {providerLabel(streamer.provider)}
        <ExternalLink />
      </AppButton>
    </div>
  );
}
