import { RefreshCw } from "lucide-react";

import { AppIconButton } from "@/components/app/app-icon-button";
import { cn } from "@/lib/utils";
import { ColorBars } from "@/pages/stream-wall/color-bars";

type WallStatusKind = "loading" | "empty" | "error";

interface WallStatusScreenProps {
  kind: WallStatusKind;
  message?: string;
  onRefresh: () => void;
}

const COPY: Record<WallStatusKind, { status: string; title: string }> = {
  loading: { status: "Standby", title: "Scanning inputs" },
  empty: { status: "No Signal", title: "No one is on air" },
  error: { status: "Signal Lost", title: "Can’t reach the feed" },
};

/**
 * Full-screen broadcast test card shown when the wall has nothing to display —
 * loading, an empty roster, or a feed error. The colour bars are the signature;
 * the overlay reads like a station ident, not a marketing page.
 */
export function WallStatusScreen({
  kind,
  message,
  onRefresh,
}: WallStatusScreenProps) {
  const copy = COPY[kind];

  return (
    <section className="relative flex size-full items-center justify-center overflow-hidden bg-background">
      <ColorBars className="absolute inset-0 opacity-30" />
      <div className="screen-scanlines absolute inset-0 opacity-30" />
      {/* Radial vignette: darkens behind the text, lets the bars read at the edges. */}
      <div className="absolute inset-0 bg-[radial-gradient(closest-side_at_center,var(--background)_30%,color-mix(in_oklch,var(--background),transparent_45%)_100%)]" />

      <div className="relative z-10 flex max-w-md flex-col items-center gap-6 px-6 text-center">
        <span className="type-umd text-muted-foreground">StreamGuard</span>

        <div className="flex flex-col items-center gap-3">
          <span
            className={cn(
              "type-umd inline-flex items-center gap-2",
              kind === "error" ? "text-tally" : "text-umd",
            )}
          >
            <span
              className={cn(
                "size-2 rounded-full",
                kind === "loading" ? "animate-pulse bg-umd" : "bg-current",
              )}
            />
            {copy.status}
          </span>
          <h1 className="font-display text-4xl text-foreground sm:text-5xl">
            {copy.title}
          </h1>
        </div>

        {message ? (
          <p className="max-w-sm text-sm text-muted-foreground">{message}</p>
        ) : null}

        {kind !== "loading" ? (
          <AppIconButton
            label="Refresh feed"
            variant="outline"
            onClick={onRefresh}
            tooltipSide="bottom"
            className="border-border bg-background/40 text-foreground hover:bg-background/70"
          >
            <RefreshCw />
          </AppIconButton>
        ) : null}
      </div>
    </section>
  );
}
