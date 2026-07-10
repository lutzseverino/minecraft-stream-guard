# Configure Twitch and YouTube

## Configure Twitch

Create a Twitch application with access to the Helix API, then configure:

```yaml
providers:
  twitch:
    enabled: true
    client-id: "your-client-id"
    client-secret: "your-client-secret"
```

Players link their Twitch channel login, without the URL:

```text
/stream link twitch channel_login
```

Twitch becomes available for linking only when it is enabled and both credential
values are present. An incomplete configuration remains unavailable rather than
silently attempting provider calls.

## Configure YouTube

Enable the YouTube Data API for a Google Cloud project, create an API key, then
configure:

```yaml
providers:
  youtube:
    enabled: true
    api-key: "your-api-key"
```

Players may link a channel ID, `@handle`, channel URL, or direct live/watch URL.
Direct video URLs avoid a channel-wide live search.

```text
/stream link youtube @channel
/stream link youtube https://www.youtube.com/watch?v=VIDEO_ID
```

YouTube becomes available for linking only when it is enabled and its API key is
present.

> [!IMPORTANT]
> These APIs report whether a public channel or video is live. They do not prove
> that the Minecraft player owns it. Use an administrator-controlled ownership
> check when the honor system is not sufficient for your server.

## Tune Provider Traffic

Use separate cache times for positive and negative checks:

```yaml
verification:
  cache:
    live-seconds: 60
    offline-seconds: 120
```

Scheduled checks deduplicate identical links. Twitch and direct YouTube video
checks are batched, and concurrent checks for the same link share one lookup.
Increase the cache times or `enforcement.recheck-interval-seconds` when API quota
is more important than immediate state changes.

`verification.maximum-status-age-seconds` is a separate safety boundary: after
that age, an old provider observation no longer grants access even if provider
refreshes have failed. It does not expire manual administrator verification.

Restart the server after changing credentials. Use `/streamguard reload` for
ordinary configuration changes, but prefer a restart when diagnosing provider
initialization.
