# StreamGuard plugin

A Paper/Spigot plugin that gates survival interactions on live Twitch or YouTube
streams. Domain policy and application services stay independent of Bukkit,
HTTP and provider adapters; the bootstrap package composes them.

## Development

Follow the [shared setup and validation guide](../docs/development/README.md)
and [contribution instructions](../CONTRIBUTING.md). From `plugin/`:

```sh
./mvnw --batch-mode --no-transfer-progress verify
```

The Maven wrapper pins Maven 3.9.16. JDK 21 builds Java 17 bytecode; verification
checks dependency convergence, Spotless, compiler warnings, JUnit/ArchUnit tests
and jar shading. Output is `target/StreamGuard-<version>.jar`.

## Configuration and integration

The plugin runs on Paper/Spigot 1.20.1+ with Java 17+. The generated
`plugins/StreamGuard/config.yml` keeps providers and the live feed disabled until
configured. See the [configuration reference](../docs/usage/reference/configuration.md),
[commands and permissions](../docs/usage/reference/commands-and-permissions.md)
and [architecture](../docs/development/architecture.md).

The companion [site](../site/README.md) consumes `/api/live`. Public channel
liveness does not establish that the Minecraft player owns the channel;
administrators can verify ownership separately.

## Releases

Plugin publication uses annotated `plugin-v…` tags and the existing release
environment. Historical `v…` releases remain available. See
[versioning](../docs/usage/reference/versioning.md) for the release procedure.
Site changes do not require a plugin release.

## License

[GNU General Public License v3.0 only](LICENSE)
