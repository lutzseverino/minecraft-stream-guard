import type { ComponentProps } from "react";

import { AppTooltip } from "@/components/app/app-tooltip";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type AppIconButtonProps = Readonly<
  Omit<ComponentProps<typeof Button>, "aria-label"> & {
    /** Accessible name — applied as aria-label and shown in a tooltip. */
    label: string;
    tooltipSide?: ComponentProps<typeof AppTooltip>["side"];
  }
>;

/**
 * Floating, glassy icon control for the wall (back, fullscreen, refresh, …).
 *
 * Icon-only by design, so a `label` is mandatory and wired to both `aria-label`
 * and a tooltip. These float over the screens; they are not page chrome.
 */
export function AppIconButton({
  className,
  label,
  tooltipSide = "bottom",
  size = "icon-lg",
  variant = "ghost",
  ...props
}: AppIconButtonProps) {
  return (
    <AppTooltip label={label} side={tooltipSide}>
      <Button
        type="button"
        aria-label={label}
        size={size}
        variant={variant}
        className={cn(
          "rounded-full border border-white/10 bg-black/40 text-white/90 shadow-lg backdrop-blur-md transition-colors hover:bg-black/60 hover:text-white",
          className,
        )}
        {...props}
      />
    </AppTooltip>
  );
}
