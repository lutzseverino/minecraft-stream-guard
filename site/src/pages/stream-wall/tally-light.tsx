import { cn } from "@/lib/utils";

/**
 * Broadcast tally light. On a real multiviewer a red LED means the source is on
 * air; that semantic is the wall's one loud colour. Used on each monitor and on
 * the floating master status.
 */
export function TallyLight({
  label,
  className,
  size = "sm",
}: {
  /** Optional uppercase label, e.g. "LIVE" or "ON AIR". */
  label?: string;
  className?: string;
  size?: "sm" | "lg";
}) {
  const dot = size === "lg" ? "size-2.5" : "size-2";
  return (
    <span className={cn("inline-flex items-center gap-2", className)}>
      <span className={cn("relative flex", dot)}>
        <span className="absolute inline-flex size-full animate-ping rounded-full bg-tally opacity-75 motion-reduce:animate-none" />
        <span
          className={cn(
            "relative inline-flex rounded-full bg-tally shadow-[0_0_8px_2px] shadow-tally/70",
            dot,
          )}
        />
      </span>
      {label ? (
        <span
          className={cn(
            "font-display leading-none text-tally",
            size === "lg" ? "text-sm tracking-[0.12em]" : "type-umd text-white",
          )}
        >
          {label}
        </span>
      ) : null}
    </span>
  );
}
