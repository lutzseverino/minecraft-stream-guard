<div align="center">
  <h1>StreamGuard Site</h1>
  <p>A fullscreen live stream wall for servers using the StreamGuard Minecraft plugin.</p>

  [![TypeScript](https://img.shields.io/badge/TypeScript-7-blue)](https://www.typescriptlang.org/)
  [![Node.js 24 | 26](https://img.shields.io/badge/Node.js-24%20%7C%2026-green)](https://nodejs.org/)
  [![CI](https://github.com/lutzseverino/minecraft-stream-guard-site/actions/workflows/ci.yml/badge.svg)](https://github.com/lutzseverino/minecraft-stream-guard-site/actions/workflows/ci.yml)
  [![License: GPL-3.0-only](https://img.shields.io/badge/license-GPL--3.0--only-2f3437)](LICENSE)
</div>

StreamGuard Site is a companion web surface for
[StreamGuard](https://github.com/lutzseverino/minecraft-stream-guard), a
Paper/Spigot plugin that checks whether survival players are live on Twitch or
YouTube.

The site reads the plugin's live feed and turns the current streamers into a
fullscreen broadcast wall. Every live player becomes a screen; selecting a
screen opens the Twitch or YouTube embed.

## Installation

Install [Node.js](https://nodejs.org/) 24 or 26 and pnpm 11, clone this repository, then run `pnpm install` and `pnpm build`. Serve the resulting `dist/` directory alongside the plugin so `/api/live` reaches its HTTP feed.

## Usage

Open the deployed wall in a browser and select a streamer to open the embedded Twitch or YouTube player.

## Screenshots

Captured from the local development demo feed.

![Live wall grid](public/screenshots/live-wall-grid.png)

| Focused stream | Mobile wall |
| --- | --- |
| ![Focused stream](public/screenshots/focused-stream.png) | ![Mobile wall](public/screenshots/mobile-wall.png) |

## Development

See the [development guide](docs/development/README.md) for contributor setup and `pnpm validate`.

## Plugin Feed

The site reads `/api/live`, served by the
[StreamGuard plugin](https://github.com/lutzseverino/minecraft-stream-guard). In local development,
`vite.config.ts` proxies that path to `http://127.0.0.1:8127`.

When the plugin is not running, development builds use a demo feed so the wall remains inspectable.
Production builds expect the real plugin feed.

## Documentation

[Documentation](docs/README.md)

## Contributing

[Contributing](CONTRIBUTING.md)

## License

[GNU General Public License v3.0 only](LICENSE)
