<div align="center">
  <h1>StreamGuard</h1>
  <p>Require players to be live before they can affect a Minecraft survival world.</p>

  [![Releases](https://img.shields.io/github/v/release/lutzseverino/minecraft-stream-guard)](https://github.com/lutzseverino/minecraft-stream-guard/releases)
  [![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%2B-3c8527)](https://papermc.io/)
  [![Java](https://img.shields.io/badge/Java-17%2B-e76f00)](https://adoptium.net/)
  [![License: GPL-3.0-only](https://img.shields.io/badge/license-GPL--3.0--only-2f3437)](LICENSE)
</div>

StreamGuard is a configurable Paper and Spigot plugin for communities where
survival interaction depends on a live Twitch or YouTube stream. A player can
still move, chat, authenticate, and manage their stream link while StreamGuard
independently blocks world-changing actions.

> [!IMPORTANT]
> StreamGuard checks whether a public channel is live; it does not prove that the
> Minecraft player owns that channel. Treat self-service linking as an honor
> system, or have administrators verify player/channel ownership separately.

## How It Works

1. A player links a Twitch channel or YouTube channel, handle, or live video.
2. StreamGuard checks the provider and records whether the linked stream is live.
3. The configured policy decides what unlinked and offline players may do.
4. Scheduled checks keep that state fresh; administrators can verify, unverify,
   or bypass a player when needed.

Provider checks are deduplicated by link. Twitch and direct YouTube video checks
are batched, and live and offline results use separate configurable cache times.
Provider errors are not treated as proof that a player is offline.

## Installation

1. Download the latest jar from [GitHub Releases](https://github.com/lutzseverino/minecraft-stream-guard/releases).
2. Place it in the server's `plugins/` directory.
3. Start the server once to create `plugins/StreamGuard/config.yml`.
4. Stop the server, configure at least one provider, then start it again.

StreamGuard targets Paper or Spigot `1.20.1+` on Java `17+`. It uses the classic
Bukkit plugin system and does not depend on NMS or CraftBukkit internals.

## Configuration

The generated `plugins/StreamGuard/config.yml` keeps providers and the live feed
disabled until credentials and exposure are configured deliberately:

```yaml
enforcement:
  grace-period-seconds: 120
  recheck-interval-seconds: 60
  unlinked:
    allow-movement: true
    allow-chat: true
    allow-commands: true
  blocked-actions:
    block-break: true
    block-place: true
    container-open: true
    crafting: true
verification:
  maximum-status-age-seconds: 180
  cache:
    live-seconds: 60
    offline-seconds: 120
providers:
  twitch:
    enabled: false
    client-id: ''
    client-secret: ''
  youtube:
    enabled: false
    api-key: ''
web:
  live-feed:
    enabled: false
    bind-host: 127.0.0.1
```

Each unverified state can independently allow movement, chat, commands, and
kick-on-join behavior, while guarded actions decide which world changes require
a current live observation. Manual administrator verification remains durable;
provider observations expire. See the
[configuration reference](docs/reference/configuration.md) for every setting
and complete provider examples.

## Commands

Players use `/stream setup`, `/stream status`, `/stream link`, `/stream verify`,
and `/stream cancel`. Administrators use `/streamguard` to inspect status,
manage bypasses, verify or unverify players, and reload configuration.

See the [commands and permissions reference](docs/reference/commands-and-permissions.md)
for syntax, duration rules, and permission nodes.

## Companion Site

The optional [StreamGuard site](https://github.com/lutzseverino/minecraft-stream-guard-site)
renders the plugin's `/api/live` feed as a fullscreen live wall. The feed can
include provider metadata such as titles, thumbnails, viewer counts, and live
start times when the provider returns them.

## Development

Build and test from the repository root with Java 17 and Maven:

```bash
mvn --batch-mode verify
```

The plugin jar is written to `target/StreamGuard-<version>.jar`. Gson and Adventure
are shaded and relocated; the Paper API remains a provided server dependency.

## Documentation

Start with the [documentation index](docs/README.md). Documentation is organized
by reader intent, including installation, operational guides, configuration
reference, game-policy concepts, architecture, and versioning.

## Support

StreamGuard is free and open source. If it helps your server, you can support
development on [Ko-fi](https://ko-fi.com/lutzseverino).

## License

StreamGuard is available under the [GNU General Public License v3.0 only](LICENSE).
