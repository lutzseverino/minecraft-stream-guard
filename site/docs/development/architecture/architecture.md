# Architecture

## Purpose

This document explains how StreamGuard Site separates the live-feed contract,
stream-wall behavior, and reusable presentation primitives.

## Overview

The site is a static Vite application. It polls the StreamGuard plugin's
`/api/live` endpoint, normalizes the response in a framework-free client, and
renders the current streamers as a responsive wall. Selecting a streamer opens
an inline Twitch or YouTube player when the feed provides enough embed data.

## Key Concepts

- `src/api` owns the feed and embed contracts without importing React.
- `src/pages/stream-wall` owns polling, layout, and page-level composition.
- `src/components/app` wraps shadcn primitives for pages and owns shared
  product invariants.
- `src/components/ui` contains generated shadcn primitives, used only through
  `src/components/app`.
- `src/components/custom` holds custom components the registry has no match
  for, such as the provider glyphs.
- `src/theme` owns theme tokens and provider wiring.
- Development may fall back to a local demo feed; production never substitutes
  demo streamers for an unavailable plugin response.

Dependency-cruiser enforces these boundaries as part of `pnpm validate`.

## Implications

- Feed normalization and provider embed behavior can be tested without React.
- The deployed site and plugin must share an origin or route `/api/live` to the
  plugin service.
- A production feed failure is visible rather than disguised with demo data.
