# Install and Configure StreamGuard

This tutorial starts with a new installation and ends with a player whose live
stream can be verified.

## Prerequisites

- Paper or Spigot 1.20.1 or newer
- Java 17 or newer
- Twitch Helix credentials, a YouTube Data API key, or both

## Install the Plugin

1. Download `StreamGuard-1.0.0.jar` from GitHub Releases.
2. Place it in the server's `plugins/` directory.
3. Start the server once.
4. Confirm that `plugins/StreamGuard/config.yml` and both language files exist.
5. Stop the server before editing configuration.

## Configure a Provider

Enable Twitch by adding its Helix application credentials:

```yaml
providers:
  twitch:
    enabled: true
    client-id: "your-client-id"
    client-secret: "your-client-secret"
```

Or enable YouTube with a YouTube Data API key:

```yaml
providers:
  youtube:
    enabled: true
    api-key: "your-api-key"
```

Keep credentials out of source control and server logs. See
[configure providers](../how-to/configure-providers.md) for accepted link forms
and request-volume controls.

Provider linking proves only that the configured public channel is live. It does
not authenticate channel ownership. Decide whether your community can use that
honor system or whether administrators must confirm ownership out of band.

## Review the Game Policy

The default policy allows unverified players to move, chat, and use commands but
blocks world-affecting actions. Review `enforcement.unlinked`,
`enforcement.not-live`, and every value under `enforcement.blocked-actions`
before opening the server to players.

Review `verification.maximum-status-age-seconds` as well. A provider result older
than this limit no longer grants access; manual administrator verification does
not expire through this setting.

Keep `/stream` and authentication commands in `commands.safe-while-unverified`
if either state disables general command use.

## Verify the Setup

1. Start the server and join as a player.
2. Run `/stream setup` and choose an enabled provider.
3. Enter the requested channel reference in chat, or run
   `/stream link twitch <channel-login>` or `/stream link youtube <reference>`.
4. Start streaming, then run `/stream verify`.
5. Run `/stream status` and confirm the stream is reported live.
6. Stop the stream and confirm a guarded action is blocked after the next check.

If the provider reports an error, verify the credentials and link format before
changing enforcement policy.
