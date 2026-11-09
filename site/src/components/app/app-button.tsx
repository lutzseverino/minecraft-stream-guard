import type { ComponentProps } from "react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type AppButtonProps = Readonly<ComponentProps<typeof Button>>;

/**
 * Text button with the wall's quiet, telemetry-style typography. Used for the
 * sparse copy that appears in empty / error states — never as page chrome.
 */
export function AppButton({ className, ...props }: AppButtonProps) {
  return (
    <Button
      className={cn(
        "h-10 px-5 font-mono text-xs leading-none tracking-[0.16em] uppercase",
        className,
      )}
      {...props}
    />
  );
}
