/**
 * Pure split-screen layout math for the wall.
 *
 * No React, no DOM — given a stream count and the viewport's aspect ratio, this
 * picks the column/row grid that keeps each tile as close to 16:9 as possible,
 * then distributes any partial final row so the grid fills the viewport edge to
 * edge with no gaps. Keeping it pure makes the packing behavior easy to reason
 * about and unit-test in isolation from rendering.
 */

/** Per-tile placement within the computed grid. */
export interface TilePlacement {
  /** 1-based grid row. */
  row: number;
  /** Number of columns this tile spans (>= 1). */
  colSpan: number;
}

export interface StreamLayout {
  columns: number;
  rows: number;
  /** One placement per stream, in input order. */
  tiles: TilePlacement[];
}

/** Tiles look best near 16:9; the grid search is scored against this. */
const TARGET_TILE_ASPECT = 16 / 9;

/**
 * Choose the column count whose resulting tile aspect ratio is closest to
 * {@link TARGET_TILE_ASPECT} for the given viewport aspect (width / height).
 */
function chooseColumns(count: number, viewportAspect: number): number {
  let bestColumns = 1;
  let bestScore = Number.POSITIVE_INFINITY;

  for (let columns = 1; columns <= count; columns++) {
    const rows = Math.ceil(count / columns);
    // Tile aspect = (viewportW / columns) / (viewportH / rows).
    const tileAspect = (viewportAspect * rows) / columns;
    // Compare in log-space so "twice as wide" and "twice as tall" score equally.
    const score = Math.abs(Math.log(tileAspect / TARGET_TILE_ASPECT));

    if (score < bestScore - 1e-9) {
      bestScore = score;
      bestColumns = columns;
    }
  }

  return bestColumns;
}

/**
 * Spread `itemsInRow` tiles across `columns` so their spans sum to `columns`
 * exactly (the extra width is given to the left-most tiles). This is what lets a
 * 3-up wall render as a full row of two plus a full-width third with no gap.
 */
function distributeRow(itemsInRow: number, columns: number): number[] {
  const base = Math.floor(columns / itemsInRow);
  const remainder = columns % itemsInRow;
  return Array.from({ length: itemsInRow }, (_, index) =>
    index < remainder ? base + 1 : base,
  );
}

export function computeStreamLayout(
  count: number,
  viewportAspect: number,
): StreamLayout {
  if (count <= 0) {
    return { columns: 0, rows: 0, tiles: [] };
  }
  if (count === 1) {
    return { columns: 1, rows: 1, tiles: [{ row: 1, colSpan: 1 }] };
  }

  const safeAspect =
    Number.isFinite(viewportAspect) && viewportAspect > 0
      ? viewportAspect
      : TARGET_TILE_ASPECT;
  const columns = chooseColumns(count, safeAspect);
  const rows = Math.ceil(count / columns);

  const tiles: TilePlacement[] = [];
  for (let index = 0; index < count; index++) {
    const row = Math.floor(index / columns) + 1;
    const itemsInRow = row < rows ? columns : count - columns * (rows - 1);
    const spans = distributeRow(itemsInRow, columns);
    const positionInRow = index - columns * (row - 1);
    tiles.push({ row, colSpan: spans[positionInRow] });
  }

  return { columns, rows, tiles };
}
