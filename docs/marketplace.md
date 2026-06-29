# StreamGuard

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
/stream setup
/stream status
/stream link twitch <channel-login>
/stream link youtube <channel-id-or-@handle>
/stream verify
/stream cancel
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
- `onboarding.enabled` controls the optional provider-picker setup flow.
- `onboarding.provider-picker` controls the chest GUI title, rows, filler item, cancel item, and
  provider buttons.
- `onboarding.chat-input` controls chat input timeout, max length, cancel keyword, and whether to
  verify immediately after linking.
- `web.live-feed` controls the optional live feed API, CORS origins, refresh interval, and provider
  metadata cache for the companion site.
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

Provider identity is modeled as data, not as a closed enum. A future provider, such as Discord
streaming presence, can add an infrastructure adapter and register its provider ID without changing
the core access policy.

## Companion Site

The optional live wall lives at
[minecraft-stream-guard-site](https://github.com/lutzseverino/minecraft-stream-guard-site). It reads
StreamGuard's `/api/live` feed and renders the currently live players as a fullscreen web wall.

## Support

StreamGuard is free and open source. If it helps your server, you can support development on
[Ko-fi](https://ko-fi.com/lutzseverino).

## Links

- Source: https://github.com/lutzseverino/minecraft-stream-guard
- Companion site: https://github.com/lutzseverino/minecraft-stream-guard-site
- Support development: https://ko-fi.com/lutzseverino

## License

StreamGuard is licensed under GPL-3.0-only.
