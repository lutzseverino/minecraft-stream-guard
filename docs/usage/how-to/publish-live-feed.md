# Publish the Live Feed Safely

The live feed is an unauthenticated HTTP endpoint intended for a companion site.
Its default loopback binding keeps it off the public network.

## Configure StreamGuard

```yaml
web:
  live-feed:
    enabled: true
    bind-host: "127.0.0.1"
    port: 8127
    path: "/api/live"
    cors:
      allowed-origins:
        - "https://streams.example.com"
```

Use exact origins, including scheme and port when non-standard. Do not use a
broad public bind address merely to make a reverse proxy work.

## Configure the Reverse Proxy

Forward only the configured path from the public HTTPS virtual host to
`http://127.0.0.1:8127`. Add TLS, request limits, and access logging at the proxy.
Do not expose other server ports through the same rule.

## Verify

1. Request `/api/live` through the public HTTPS hostname.
2. Confirm the response is unavailable through the host's raw port `8127`.
3. Load the companion site and inspect its browser console for CORS errors.
4. Confirm an origin not listed in `allowed-origins` cannot read the response.

The feed reveals live-player and stream metadata by design. Publish it only when
players and server operators understand that visibility.
