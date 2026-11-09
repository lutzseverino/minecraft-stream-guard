/** Small presentation helpers shared across wall components. */

/** Compact viewer count, e.g. 1284 → "1.3K". */
export function formatViewers(count: number): string {
  if (count < 1000) {
    return String(count);
  }
  const thousands = count / 1000;
  if (thousands < 10) {
    return `${thousands.toFixed(1).replace(/\.0$/, "")}K`;
  }
  if (count < 1_000_000) {
    return `${Math.round(thousands)}K`;
  }
  return `${(count / 1_000_000).toFixed(1).replace(/\.0$/, "")}M`;
}

/** "Live for" duration since an ISO timestamp, e.g. "1h 36m". */
export function formatLiveDuration(liveSince: string): string | null {
  const started = Date.parse(liveSince);
  if (Number.isNaN(started)) {
    return null;
  }
  const totalMinutes = Math.max(0, Math.floor((Date.now() - started) / 60_000));
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours === 0) {
    return `${minutes}m`;
  }
  return `${hours}h ${minutes}m`;
}

/** Deterministic hue (0–360) from a name, for the generated screen surface. */
export function hueFromName(name: string): number {
  let hash = 0;
  for (let index = 0; index < name.length; index++) {
    hash = (hash * 31 + name.charCodeAt(index)) % 360;
  }
  return hash;
}

/** First glyph of a player name for the screen placeholder monogram. */
export function monogram(name: string): string {
  return name.trim().charAt(0).toUpperCase() || "?";
}
