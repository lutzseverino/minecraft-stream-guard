<div align="center">
    <h1 align="center">StreamGuard</h1>
    <p>A configurable Paper/Spigot plugin that requires players to be live before they can affect a survival world.</p>
    <p>
        <img alt="plugin" src="https://img.shields.io/badge/plugin-paper%2Fspigot-0f172a">
        <img alt="minecraft" src="https://img.shields.io/badge/minecraft-1.20.1%2B-111827">
        <img alt="java" src="https://img.shields.io/badge/java-17-1f2937">
        <img alt="build" src="https://img.shields.io/badge/build-maven-374151">
        <img alt="license" src="https://img.shields.io/badge/license-GPL--3.0--only-4b5563">
    </p>
</div>

## Overview

StreamGuard is a survival-server guardrail for communities that want world interaction gated behind
live stream verification.

Unverified players can be allowed to move, chat, and use safe commands, or they can be frozen,
command-limited, or kicked. World-affecting actions are configured separately, so each server can
choose how strict the live requirement should be.

## Server Target

- Paper/Spigot `1.20.1+`
- Java `17+`
- Classic `plugin.yml`
- No NMS or CraftBukkit internals

## Player Flow

Players manage their stream link from the server:

```text
/stream status
/stream link twitch <channel-login>
/stream link youtube <channel-id-or-@handle>
/stream verify
```

Admins manage access, manual verification, and bypasses:

```text
/streamguard status <player>
/streamguard bypass grant <player> [duration] [reason]
/streamguard bypass remove <player>
/streamguard verify <player> [manual|twitch|youtube] [reason]
/streamguard unverify <player> [reason]
/streamguard reload
```

Bypass durations support `s`, `m`, `h`, and `d` suffixes, such as `30m`, `2h`, or `1d`. Omitting
the duration grants a persistent bypass.

## Configuration

`config.yml` separates player state rules from world-affecting action rules.

- `enforcement.unlinked` controls players who have not linked a stream account.
- `enforcement.not-live` controls linked players who are not currently live.
- `enforcement.blocked-actions` controls block break/place, containers, pickup/drop, crafting,
  trading, entity damage/interact, buckets, fire, and other guarded interactions.
- `commands.safe-while-unverified` keeps commands such as `/stream`, `/login`, and `/register`
  usable when command blocking is enabled.

Each state can independently allow or block movement, chat, commands, and kick-on-join.

## Providers

Twitch verification uses the Helix streams API with:

```yaml
providers:
  twitch:
    enabled: true
    client-id: ""
    client-secret: ""
```

YouTube verification uses the YouTube Data API with:

```yaml
providers:
  youtube:
    enabled: true
    api-key: ""
```

YouTube links accept either a channel ID or `@handle`.

Provider identity is modeled as data through `StreamProviderId`, not as a closed enum. A future
provider, such as Discord streaming presence, can add an infrastructure adapter and register its
provider ID without changing the core access policy.

## Architecture

The codebase keeps server, network, YAML, persistence, and rendering concerns at the edges.

- `domain` owns pure policy, stream status, session state, and bypass rules.
- `application` owns use cases and provider/repository ports.
- `config` owns typed settings and validation.
- `i18n` owns locale fallback and message rendering.
- `infrastructure` owns Twitch, YouTube, and YAML persistence adapters.
- `platform.bukkit` owns Bukkit listeners, commands, permissions, and scheduler glue.
- `bootstrap` wires the plugin together.

Dependency direction points inward: Bukkit and infrastructure depend on application/domain
abstractions, not the other way around.

## Build

Build and test from the repository root:

```bash
mvn package
```

The plugin jar is written to:

```text
target/StreamGuard-0.1.0-SNAPSHOT.jar
```

## Quality Checks

```bash
mvn test
mvn package
```

`mvn package` runs the tests and produces the shaded plugin jar. Gson is shaded and relocated for
provider API JSON parsing; Paper/Spigot remains a provided server dependency.

## License

StreamGuard is licensed under the GNU General Public License v3.0 only.

You can sell packaged builds, use the plugin, compile it yourself, and redistribute modified versions
under the GPL terms. See [LICENSE](LICENSE).
