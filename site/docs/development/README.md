Contributor setup and maintenance guidance for StreamGuard Site.

## Setup and validation

Use Node.js 24 or 26 and pnpm 11; `packageManager` in `package.json` pins the exact pnpm version. From the repository root, run `pnpm install` and `pnpm dev`.

Run `pnpm validate` before review. It runs, in order:

- `format:check`: Prettier 3.
- `lint`: oxlint 1, type-aware through `oxlint-tsgolint`, with warnings denied.
- `typecheck`: TypeScript 7 (`tsc -b`).
- `test:run`: Vitest 5.
- `check:architecture`: dependency-cruiser module boundaries.
- `check:docs`: documentation links and indexes.
- `ui:check`: generated shadcn registry integrity.
- `build`: the production Vite 8 build.

`pnpm build` creates `dist/`; `pnpm preview` serves it locally. `pnpm format` rewrites formatting. `validate` is the App's own aggregate; Repo Canon's `scripts/validate.mjs` is a separate issue and pull request contract check.

The required checks are `Validate site` from CI, which runs `validate` on Node.js 24 and 26, plus `validate` from PR Policy and `PR metadata`.

### Lint and format coverage

`.oxlintrc.json` enables oxlint's `correctness` category, the typed TypeScript rules, the React hooks rules and the react-refresh export rule. oxlint does not organize imports or lint CSS and JSON; Prettier formats CSS and JSON. `react/set-state-in-effect` is off, because the wall's effect that closes a stream when it goes off air calls `setState`.

`.prettierignore` and the `ignorePatterns` in `.oxlintrc.json` exclude Repo Canon-owned files and generated registry output. Prettier also leaves Markdown, YAML and workflows unformatted.

dependency-cruiser 18 reads TypeScript through the compiler API, which TypeScript 7 does not ship, so `pnpm-workspace.yaml` gives it a private TypeScript 6 to parse with. The App itself compiles with TypeScript 7.

### shadcn registry

`components.json` uses the `base-nova` style (Base UI) with lucide icons. Generated output in `src/components/ui/` is never edited or formatted by hand. `registry-manifest.json` records each component and a SHA-256 of every generated file; `pnpm ui:check` fails on a changed, missing or unexpected file.

- `pnpm ui:add <component>` adds components with the pinned shadcn CLI and records them.
- `pnpm ui:update` refreshes only the recorded components and fails if the refresh creates unrecorded files.
- Both stop before running the CLI when `ui:check` fails, so a hand edit or stray file never becomes the recorded baseline.

Pages use registry components through `src/components/app/`. Custom components the registry has no match for, such as the provider glyphs, live in `src/components/custom/`.

Light colour tokens live in `:root` and dark tokens in `.dark`. The boot script in `index.html` and `ThemeProvider` toggle only `.dark`; dark is the default. There is no theme toggle in the UI. To check another theme, run `localStorage.setItem('streamguard-theme', 'light'); location.reload()` in the browser (or `'dark'`, `'system'`), and remove the key to return to the default.

## Index

- [Architecture](architecture/README.md): Module boundaries and runtime behavior.
- [Templates](templates/README.md): Starting points for maintained documentation.
