# Run the Site Locally

Run StreamGuard Site against a local plugin feed or its development-only demo
feed.

## Steps

1. Install Node.js 24 or 26 and pnpm 11.
2. Install dependencies with `pnpm install`.
3. Start StreamGuard's HTTP feed on `http://127.0.0.1:8127`, if available.
4. Start the site with `pnpm dev`.
5. Open the local URL printed by Vite.

Vite proxies `/api/live` to the local plugin endpoint. If that endpoint is
unavailable, the development build uses a demo feed so the wall remains
inspectable.

## Verification

Run `pnpm validate`. The command must complete Prettier, oxlint, TypeScript,
Vitest, architecture, documentation and registry checks, and the production
build, without errors.

## Notes

- Production builds do not use the demo feed.
- Use `pnpm build` followed by `pnpm preview` to inspect production output.
