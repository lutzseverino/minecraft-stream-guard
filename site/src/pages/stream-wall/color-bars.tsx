/**
 * SMPTE-style colour bars — the wall's signature "no signal" surface.
 *
 * Hand-built from three stacked bands (main 75% bars, a reverse castellation
 * strip, and a PLUGE / sub-black row) so it reads as a real broadcast test
 * pattern rather than decoration. Colours are fixed screen test values.
 */

const MAIN_BARS = [
  "#bfbfbf", // 75% white
  "#bfbf00", // yellow
  "#00bfbf", // cyan
  "#00bf00", // green
  "#bf00bf", // magenta
  "#bf0000", // red
  "#0000bf", // blue
];

const CASTELLATION = [
  "#0000bf",
  "#131313",
  "#bf00bf",
  "#131313",
  "#00bfbf",
  "#131313",
  "#bfbfbf",
];

const PLUGE = [
  "#131313", // -I
  "#ffffff", // 100% white
  "#131313", // +Q
  "#0a0a0a", // black
  "#000000", // -4% (super black)
  "#0a0a0a", // black
  "#1a1a1a", // +4%
];

function barGradient(colors: string[]): string {
  const step = 100 / colors.length;
  const stops = colors
    .map((color, index) => {
      const start = (index * step).toFixed(4);
      const end = ((index + 1) * step).toFixed(4);
      return `${color} ${start}%, ${color} ${end}%`;
    })
    .join(", ");
  return `linear-gradient(to right, ${stops})`;
}

export function ColorBars({ className }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={className}
      style={{ display: "flex", flexDirection: "column", height: "100%" }}
    >
      <div
        style={{ flex: "1 1 0%", backgroundImage: barGradient(MAIN_BARS) }}
      />
      <div
        style={{ flex: "0 0 9%", backgroundImage: barGradient(CASTELLATION) }}
      />
      <div style={{ flex: "0 0 14%", backgroundImage: barGradient(PLUGE) }} />
    </div>
  );
}
