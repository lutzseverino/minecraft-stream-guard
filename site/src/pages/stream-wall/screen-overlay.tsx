/**
 * Shared monitor treatment used by source tiles: scanlines, readability scrim,
 * and a hairline bezel. This keeps the visual language consistent without
 * promoting wall-specific chrome into generic components.
 */
export function ScreenOverlay() {
  return (
    <>
      <div className="screen-scanlines pointer-events-none absolute inset-0 opacity-40" />
      <div className="screen-scrim pointer-events-none absolute inset-0 opacity-65 transition-opacity duration-300 group-hover:opacity-90 motion-reduce:transition-none" />
      <div className="pointer-events-none absolute inset-0 ring-1 ring-white/5 ring-inset" />
    </>
  );
}
