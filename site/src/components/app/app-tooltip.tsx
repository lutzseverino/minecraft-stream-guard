import type { ComponentProps, ReactElement } from "react";

import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";

type AppTooltipProviderProps = Readonly<ComponentProps<typeof TooltipProvider>>;

type AppTooltipProps = Readonly<
  Omit<ComponentProps<typeof TooltipContent>, "children"> & {
    /** The trigger element; it renders in place of Base UI's own trigger. */
    children: ReactElement<Record<string, unknown>>;
    label: string;
  }
>;

/*
 * Hover opens after 250 ms; keyboard focus opens at once. A tooltip opened
 * within 300 ms of another closing skips the delay (Base UI defaults to 400).
 * Triggers leave `delay` unset so this provider delay applies.
 */
function AppTooltipProvider(props: AppTooltipProviderProps) {
  return <TooltipProvider delay={250} timeout={300} {...props} />;
}

/*
 * Escape closes the tooltip and still reaches the page, so a single Escape on a
 * focused control also dismisses the page layer it sits on (Base UI otherwise
 * stops the event).
 */
const letEscapeReachPage: ComponentProps<typeof Tooltip>["onOpenChange"] = (
  _open,
  details,
) => {
  if (details.reason === "escape-key") {
    details.allowPropagation();
  }
};

function AppTooltip({
  children,
  label,
  side = "bottom",
  // Keeps the 10 px trigger gap the wall had under Radix (base-nova uses 4).
  sideOffset = 10,
  ...props
}: AppTooltipProps) {
  return (
    <Tooltip onOpenChange={letEscapeReachPage}>
      <TooltipTrigger render={children} />
      <TooltipContent side={side} sideOffset={sideOffset} {...props}>
        {label}
      </TooltipContent>
    </Tooltip>
  );
}

export { AppTooltip, AppTooltipProvider };
