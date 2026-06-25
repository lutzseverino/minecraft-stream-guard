# StreamGuard Implementation Prompt

Build `StreamGuard`, a configurable Paper/Spigot plugin for survival servers where players must be live on stream before they can interact with the world.

The plugin should support Paper/Spigot `1.20.1+`, compile for Java `17`, and avoid NMS or CraftBukkit internals. Use a classic Bukkit `plugin.yml`, not the experimental Paper plugin system. Keep compatibility broad by compiling against a conservative Paper API and avoiding APIs newer than the target unless guarded by adapters.

The server policy is:

- Players who are not stream-verified may move and chat.
- Players who are not stream-verified may not affect the world.
- Block breaking, block placing, containers, item pickup/drop, crafting, trading, damaging entities, and risky interactions should be individually configurable.
- Operators/admins can grant persistent or temporary bypasses, with optional duration and reason.
- The plugin should be i18n-ready, defaulting to `es_ES` for the Maresme server while shipping `en_US` as the fallback locale.

Architect the project with clear responsibility boundaries:

- `domain`: pure stream gate policy, player session state, verification status, bypass concepts, and rule decisions. No Bukkit, no HTTP, no file I/O.
- `application`: use cases such as checking a player's gate status, applying bypasses, linking accounts, refreshing stream status, and deciding whether an attempted action should be allowed.
- `config`: typed configuration loading, defaults, and validation.
- `i18n`: locale selection, fallback lookup, MiniMessage rendering, and placeholder replacement.
- `infrastructure`: Twitch/YouTube clients and persistence implementations. External APIs live here behind ports.
- `platform.bukkit`: Bukkit listeners, commands, permission checks, scheduler integration, and adaptation between Bukkit events and application use cases.
- `bootstrap`: plugin composition root and lifecycle wiring.

Keep the design plain Java and SOLID:

- Depend on abstractions at domain/application boundaries.
- Keep side effects at adapters and the Bukkit edge.
- Keep domain objects small, explicit, and testable.
- Prefer immutable value objects for policy inputs and verification results.
- Make commands and listeners thin; they should translate platform events into application calls.
- Do not let Twitch, YouTube, Bukkit, YAML, or MiniMessage concerns leak into domain decisions.

Initial commands should include:

- `/streamguard status [player]`
- `/streamguard bypass <player> [duration] [reason]`
- `/streamguard unbypass <player>`
- `/streamguard exempt <player>`
- `/streamguard require <player>`
- `/streamguard reload`
- `/stream link <platform>`
- `/stream unlink <platform>`
- `/stream status`

Permissions should include:

- `streamguard.admin`
- `streamguard.bypass`
- `streamguard.exempt`
- `streamguard.reload`
- `streamguard.status.self`
- `streamguard.status.others`

Configuration should be expressive but boring:

- language default, fallback, and per-player-locale toggle
- grace period
- stream status recheck interval
- enforcement mode
- allow/block toggles by event category
- safe command allowlist
- bypass behavior
- provider settings for Twitch and YouTube
- Spanish and English message files with MiniMessage formatting

Implement tests around domain policy first, then application use cases, then platform adapters where practical.

