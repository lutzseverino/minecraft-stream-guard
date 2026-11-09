import { describe, expect, it } from "vitest";

import { computeStreamLayout } from "@/pages/stream-wall/stream-layout";

describe("computeStreamLayout", () => {
  it("fills the viewport with one tile", () => {
    expect(computeStreamLayout(1, 16 / 9)).toEqual({
      columns: 1,
      rows: 1,
      tiles: [{ row: 1, colSpan: 1 }],
    });
  });

  it("uses the full final row when a row is partial", () => {
    const layout = computeStreamLayout(5, 16 / 9);

    expect(layout.columns).toBe(2);
    expect(layout.rows).toBe(3);
    expect(layout.tiles.at(-1)).toEqual({ row: 3, colSpan: 2 });
  });

  it("returns an empty layout for no streams", () => {
    expect(computeStreamLayout(0, 16 / 9)).toEqual({
      columns: 0,
      rows: 0,
      tiles: [],
    });
  });
});
