// Project-owned shadcn registry integrity.
//
//   node scripts/ui-registry.mjs add <component...>  add components with the pinned CLI
//   node scripts/ui-registry.mjs update              refresh the recorded components
//   node scripts/ui-registry.mjs check               verify generated output offline
//
// Generated registry output lives in the directories below and is never edited
// by hand. registry-manifest.json records the components and a SHA-256 of every
// generated file; `check` fails on changed, missing or unexpected files.
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { existsSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const generatedDirectories = ["src/components/ui", "src/hooks"];
const manifestName = "registry-manifest.json";

function listGenerated(root) {
  return generatedDirectories
    .flatMap((directory) => {
      const absolute = path.join(root, directory);
      if (!existsSync(absolute)) {
        return [];
      }
      return readdirSync(absolute, { recursive: true, withFileTypes: true })
        .filter((entry) => entry.isFile())
        .map((entry) =>
          path
            .relative(root, path.join(entry.parentPath, entry.name))
            .split(path.sep)
            .join("/"),
        );
    })
    .sort((left, right) => left.localeCompare(right));
}

function hashFile(root, file) {
  const digest = createHash("sha256")
    .update(readFileSync(path.join(root, file)))
    .digest("hex");
  return `sha256-${digest}`;
}

function readManifest(root) {
  return JSON.parse(readFileSync(path.join(root, manifestName), "utf8"));
}

function writeManifest(root, components) {
  const files = Object.fromEntries(
    listGenerated(root).map((file) => [file, hashFile(root, file)]),
  );
  const manifest = {
    components: [...components].sort((left, right) =>
      left.localeCompare(right),
    ),
    files,
  };
  writeFileSync(
    path.join(root, manifestName),
    `${JSON.stringify(manifest, null, 2)}\n`,
  );
}

export function checkRegistry(root) {
  const { files } = readManifest(root);
  const recorded = Object.keys(files);
  const present = listGenerated(root);
  const problems = [];

  for (const file of recorded) {
    if (!present.includes(file)) {
      problems.push(`missing: ${file}`);
    } else if (hashFile(root, file) !== files[file]) {
      problems.push(`changed: ${file}`);
    }
  }
  for (const file of present) {
    if (!recorded.includes(file)) {
      problems.push(`unexpected: ${file}`);
    }
  }

  return problems;
}

function runShadcn(root, args) {
  const result = spawnSync("pnpm", ["exec", "shadcn", "add", ...args], {
    cwd: root,
    stdio: "inherit",
  });
  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

// Regenerating records the current bytes, so start only from clean output;
// otherwise a hand edit or stray file would become the new baseline.
function requireCleanRegistry(root, command) {
  const problems = checkRegistry(root);
  if (problems.length === 0) {
    return;
  }
  for (const problem of problems) {
    console.error(problem);
  }
  console.error(
    `ui:${command} stopped: generated output already differs from ${manifestName}. Restore it before regenerating.`,
  );
  process.exit(1);
}

function main() {
  const root = fileURLToPath(new URL("../", import.meta.url));
  const [command, ...names] = process.argv.slice(2);

  if (command === "check") {
    const problems = checkRegistry(root);
    for (const problem of problems) {
      console.error(problem);
    }
    if (problems.length > 0) {
      console.error(
        `ui:check failed: generated output differs from ${manifestName}. Regenerate with pnpm ui:add or pnpm ui:update.`,
      );
      process.exit(1);
    }
    console.log(`ui:check passed (${listGenerated(root).length} files).`);
    return;
  }

  if (command === "add") {
    if (names.length === 0) {
      console.error("Usage: pnpm ui:add <component...>");
      process.exit(1);
    }
    const hasManifest = existsSync(path.join(root, manifestName));
    if (hasManifest) {
      requireCleanRegistry(root, command);
    }
    const components = hasManifest ? readManifest(root).components : [];
    runShadcn(root, [...names, "--yes"]);
    writeManifest(root, new Set([...components, ...names]));
    return;
  }

  if (command === "update") {
    requireCleanRegistry(root, command);
    const { components, files } = readManifest(root);
    runShadcn(root, [...components, "--overwrite", "--yes"]);
    // Refresh only what is recorded; new files mean the registry grew.
    const created = listGenerated(root).filter((file) => !(file in files));
    if (created.length > 0) {
      console.error(
        `ui:update created unrecorded files: ${created.join(", ")}. Add new components with pnpm ui:add.`,
      );
      process.exit(1);
    }
    writeManifest(root, components);
    return;
  }

  console.error("Usage: node scripts/ui-registry.mjs <add|update|check>");
  process.exit(1);
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main();
}
