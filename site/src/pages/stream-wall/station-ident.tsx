import type { LiveStreamer } from "@/api/live-feed";
import {
  ProviderGlyph,
  providerAccentClass,
} from "@/components/custom/provider-glyph";
import { TallyLight } from "@/pages/stream-wall/tally-light";

interface StationIdentProps {
  streamer: LiveStreamer;
}

/**
 * Fullscreen station identity overlay. Kept page-local because it belongs to
 * the wall's broadcast treatment, not to the generic app component layer.
 */
export function StationIdent({ streamer }: StationIdentProps) {
  return (
    <div className="pointer-events-none absolute top-[max(1rem,env(safe-area-inset-top))] right-[max(1rem,env(safe-area-inset-right))] z-10 flex max-w-[calc(100vw-5.5rem)] items-center gap-2.5 rounded-full bg-black/45 px-3.5 py-2 backdrop-blur-md">
      <TallyLight />
      <span className="truncate text-sm font-semibold text-white">
        {streamer.playerName}
      </span>
      <ProviderGlyph
        provider={streamer.provider}
        className={`size-4 ${providerAccentClass(streamer.provider)}`}
      />
    </div>
  );
}
