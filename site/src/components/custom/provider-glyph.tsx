import type { SVGProps } from "react";

import type { StreamProvider } from "@/api/live-feed";
import { cn } from "@/lib/utils";

/*
 * lucide-react dropped its brand icons, so the Twitch / YouTube marks are kept
 * here as small, self-contained glyphs. They inherit `currentColor`, which lets
 * callers tint them with the provider accent tokens (text-twitch / text-youtube).
 */

function TwitchGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" {...props}>
      <path d="M4.265 3 3 6.236v13.06h4.476V22h2.52l2.508-2.704h3.654L21 14.297V3H4.265Zm14.74 10.51-2.864 3.085h-4.476l-2.508 2.7v-2.7H5.371V4.62h13.634v8.89Z" />
      <path d="M14.79 7.77h1.668v4.885H14.79V7.77Zm-4.59 0h1.667v4.885H10.2V7.77Z" />
    </svg>
  );
}

function YouTubeGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" {...props}>
      <path d="M23.5 6.507a3.02 3.02 0 0 0-2.124-2.135C19.505 3.866 12 3.866 12 3.866s-7.505 0-9.376.506A3.02 3.02 0 0 0 .5 6.507C0 8.39 0 12.32 0 12.32s0 3.93.5 5.813a3.02 3.02 0 0 0 2.124 2.135c1.871.506 9.376.506 9.376.506s7.505 0 9.376-.506a3.02 3.02 0 0 0 2.124-2.135c.5-1.883.5-5.813.5-5.813s0-3.93-.5-5.813ZM9.6 15.907V8.733l6.272 3.587L9.6 15.907Z" />
    </svg>
  );
}

type ProviderGlyphProps = Readonly<
  SVGProps<SVGSVGElement> & {
    provider: StreamProvider;
  }
>;

export function ProviderGlyph({
  provider,
  className,
  ...props
}: ProviderGlyphProps) {
  const Glyph = provider === "twitch" ? TwitchGlyph : YouTubeGlyph;
  return <Glyph className={cn("size-4", className)} {...props} />;
}

export function providerLabel(provider: StreamProvider): string {
  return provider === "twitch" ? "Twitch" : "YouTube";
}

export function providerAccentClass(provider: StreamProvider): string {
  return provider === "twitch" ? "text-twitch" : "text-youtube";
}
