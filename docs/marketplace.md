# StreamGuard

StreamGuard is a configurable Paper/Spigot plugin for survival servers where players must be live on
stream before they can interact with the world.

Unverified players can be allowed to move, chat, and use safe commands, or they can be frozen,
command-limited, or kicked. World-affecting actions are configured separately, so each server can
choose how strict the live requirement should be.

## Features

- Twitch and YouTube live verification.
- Configurable rules for unlinked players and linked players who are not live.
- Configurable blocking for block breaking, block placing, containers, item pickup/drop, crafting,
  trading, entity damage, entity interaction, buckets, fire, and other risky interactions.
- Persistent and temporary admin bypasses with optional duration and reason.
- Optional in-game setup flow with provider picker and chat input.
- English and Spanish message files.
- Optional `/api/live` feed for companion live wall websites.
- No NMS or CraftBukkit internals.

## Commands

Players:

```text
/stream setup
/stream status
/stream link twitch <channel-login>
/stream link youtube <channel-id-or-@handle>
/stream verify
/stream cancel
```

Admins:

```text
/streamguard status <player>
/streamguard bypass grant <player> [duration] [reason]
/streamguard bypass remove <player>
/streamguard verify <player> [manual|twitch|youtube] [reason]
/streamguard unverify <player> [reason]
/streamguard reload
```

## Requirements

- Paper/Spigot 1.20.1+
- Java 17+
- Twitch Helix credentials for Twitch verification
- YouTube Data API key for YouTube verification

## Links

- Source: https://github.com/lutzseverino/minecraft-stream-guard
- Companion site: https://github.com/lutzseverino/minecraft-stream-guard-site
- Support development: https://ko-fi.com/lutzseverino

StreamGuard is licensed under GPL-3.0-only.
