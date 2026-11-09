<div align="center">
  <h1>StreamGuard</h1>
  <p>A Minecraft plugin and live stream wall for streaming server communities.</p>

  [![CI](https://github.com/lutzseverino/minecraft-stream-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/lutzseverino/minecraft-stream-guard/actions/workflows/ci.yml)
  [![Site CI](https://github.com/lutzseverino/minecraft-stream-guard/actions/workflows/site-ci.yml/badge.svg)](https://github.com/lutzseverino/minecraft-stream-guard/actions/workflows/site-ci.yml)
  [![Java 17+](https://img.shields.io/badge/Java-17%2B-e76f00)](https://adoptium.net/)
  [![Node.js 24 | 26](https://img.shields.io/badge/Node.js-24%20%7C%2026-green)](https://nodejs.org/)
  [![TypeScript](https://img.shields.io/badge/TypeScript-7-blue)](https://www.typescriptlang.org/)
  [![License: GPL-3.0-only](https://img.shields.io/badge/license-GPL--3.0--only-2f3437)](LICENSE)
</div>

StreamGuard combines a Paper/Spigot plugin that guards survival interactions
with a fullscreen wall of live Twitch and YouTube streams.

## Installation

Download the plugin jar from [GitHub Releases](https://github.com/lutzseverino/minecraft-stream-guard/releases),
place it in the server's `plugins/` directory, and start the server to generate
`plugins/StreamGuard/config.yml`. Stop the server, configure a provider, then
restart. For the optional wall, run `pnpm --dir site install --frozen-lockfile`
and `pnpm --dir site build`, then serve `site/dist/` alongside the plugin so
`/api/live` reaches its feed.

## Usage

Players link a channel with `/stream link twitch <channel>` and inspect their
status with `/stream status`. Open the deployed wall to select a live streamer.

## Components

- [Plugin](plugin/README.md): installation, configuration, commands, and the
  Maven build. Requires Paper/Spigot 1.20.1+ and Java 17+ at runtime.
- [Site](site/README.md): installation, screenshots, and the pnpm/Vite build.
  Its static `dist/` consumes the plugin's `/api/live` HTTP feed and can be
  deployed independently.

> [!IMPORTANT]
> StreamGuard checks whether a public channel is live; it does not prove that
> the Minecraft player owns that channel. Administrators can verify ownership
> separately.

## Development

Use JDK 21, Python 3, Node.js 24 or 26 and pnpm 11.28.9. Install site dependencies
with `pnpm --dir site install --frozen-lockfile`, then run both components from
the repository root:

```sh
bash scripts/check.sh
```

See the [development guide](docs/development/README.md) for all prerequisites
and component commands. Each component retains its own tools and version.
Future plugin releases use annotated `plugin-v…` tags; site builds and
changes never require a plugin release. Historical plugin releases remain at
[GitHub Releases](https://github.com/lutzseverino/minecraft-stream-guard/releases).

## Documentation

[Documentation](docs/README.md)

## Contributing

[Contributing](CONTRIBUTING.md)

## Support

You can support development on [Ko-fi](https://ko-fi.com/lutzseverino).

## License

[GNU General Public License v3.0 only](LICENSE)
