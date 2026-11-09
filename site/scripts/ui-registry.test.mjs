import { cpSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { checkRegistry } from "./ui-registry.mjs";

const projectRoot = fileURLToPath(new URL("../", import.meta.url));

describe("ui:check", () => {
  let root = "";

  beforeEach(() => {
    root = mkdtempSync(path.join(tmpdir(), "ui-registry-"));
    cpSync(
      path.join(projectRoot, "registry-manifest.json"),
      path.join(root, "registry-manifest.json"),
    );
    cpSync(
      path.join(projectRoot, "src/components/ui"),
      path.join(root, "src/components/ui"),
      { recursive: true },
    );
  });

  afterEach(() => {
    rmSync(root, { recursive: true, force: true });
  });

  it("passes the committed output", () => {
    expect(checkRegistry(root)).toEqual([]);
  });

  it("fails a changed file", () => {
    writeFileSync(
      path.join(root, "src/components/ui/button.tsx"),
      "export {};\n",
    );
    expect(checkRegistry(root)).toEqual([
      "changed: src/components/ui/button.tsx",
    ]);
  });

  it("fails a missing file", () => {
    rmSync(path.join(root, "src/components/ui/tooltip.tsx"));
    expect(checkRegistry(root)).toEqual([
      "missing: src/components/ui/tooltip.tsx",
    ]);
  });

  it("fails an unexpected file", () => {
    writeFileSync(path.join(root, "src/components/ui/extra.tsx"), "");
    expect(checkRegistry(root)).toEqual([
      "unexpected: src/components/ui/extra.tsx",
    ]);
  });
});
