# Versioning

This repository follows
[Romantic Versioning](https://romversioning.github.io/romver/), written as
`PROJECT.MAJOR.MINOR`.

Project version `1` identifies the complete StreamGuard product. A major
increment represents an incompatible change within StreamGuard; a minor
increment represents a compatible feature or fix. Releases use numeric versions
only, without Semantic Versioning prerelease suffixes.

Release tags use the form `v1.0.0`. The tag must exactly match the Maven project
version, the `plugin.yml` version, and a release note at
`docs/releases/1.0.0.md`. The release workflow rejects mismatches before
publishing.
