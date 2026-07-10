# Configuration Reference

The canonical defaults live in `src/main/resources/config.yml`. Values below are
grouped by their top-level configuration section.

## `language`

| Setting | Meaning |
| --- | --- |
| `default-locale` | Primary catalog, `es_ES` by default. |
| `fallback-locale` | Catalog used when a message is missing, `en_US` by default. |

Message catalogs live under `plugins/StreamGuard/lang/` and accept MiniMessage
formatting. Placeholder names such as `{player}` are owned by each message.

## `enforcement`

`grace-period-seconds` delays ordinary enforcement after joining.
`recheck-interval-seconds` controls scheduled stream refreshes. The `unlinked`
and `not-live` sections each configure `kick-on-join`, `kick-delay-seconds`,
`allow-movement`, `allow-chat`, and `allow-commands`.

`blocked-actions` contains Boolean switches for:

- `block-break`, `block-place`, and `block-interact`
- `container-open`, `item-pickup`, `item-drop`, and `inventory-click`
- `crafting` and `villager-trading`
- `entity-damage` and `entity-interact`
- `buckets` and `fire`

A value of `true` means the action is blocked when the player's gate state is
enforced. Test inventory settings carefully: broad inventory blocking can also
affect benign menus.

## `verification`

`cache.live-seconds` and `cache.offline-seconds` independently control
provider-result cache lifetimes. Errors are not cached as offline results.

`maximum-status-age-seconds` limits how long a saved provider observation can
grant access. Once stale, the player must receive a fresh provider result. Manual
administrator verification remains durable and is not expired by this limit.

## `onboarding`

`enabled` toggles the `/stream setup` flow. `provider-picker` controls the title,
row count, filler, cancel item, and each provider button. Item definitions accept
`material`, `name`, `lore`, `custom-model-data`, and `glow`; buttons also accept
`enabled`, `slot`, and `input-hint`.

`chat-input` controls `timeout-seconds`, `max-length`, `cancel-keyword`, and
`verify-after-link`. Inventory slots are zero-based Bukkit slots and must fit
within the configured row count.

## `web.live-feed`

| Setting | Meaning |
| --- | --- |
| `enabled` | Starts the embedded HTTP feed; disabled by default. |
| `bind-host` | Listen address; defaults to loopback. |
| `port` | Listen port. |
| `path` | Feed request path. |
| `update-interval-seconds` | Snapshot refresh interval. |
| `metadata-cache-seconds` | Provider metadata cache lifetime. |
| `cors.allowed-origins` | Exact browser origins allowed to read the feed. |

See [publish the live feed safely](../how-to/publish-live-feed.md) before exposing
the endpoint beyond the host.

## `commands.safe-while-unverified`

Command labels and prefixes allowed when a state has `allow-commands: false`.
Keep StreamGuard setup/status and any login plugin commands required before a
player can authenticate.

## `bypass`

| Setting | Meaning |
| --- | --- |
| `ops-bypass-by-default` | Lets operators bypass enforcement. |
| `allow-temporary-bypass` | Allows duration-bearing bypass grants. |
| `max-temporary-bypass-minutes` | Maximum temporary duration; non-positive disables the limit. |

## `providers`

Twitch accepts `enabled`, `client-id`, and `client-secret`. YouTube accepts
`enabled` and `api-key`. A provider is linkable only when enabled with all of its
required credentials. Both bundled providers are disabled by default. Enabling
one with incomplete credentials logs a warning and leaves it unavailable. Keep
credentials out of version control.

StreamGuard verifies public live status, not ownership of the linked channel.
Self-service linking is therefore an honor system unless the server adds an
administrator-controlled ownership check. See
[configure providers](../how-to/configure-providers.md) for examples.
