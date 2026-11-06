# Versioning

This repository follows
[Romantic Versioning](https://romversioning.github.io/romver/), written as
`PROJECT.MAJOR.MINOR`.

Project version `1` identifies the complete StreamGuard product. A major
increment represents an incompatible change within StreamGuard; a minor
increment represents a compatible feature or fix. Releases use numeric versions
only, without Semantic Versioning prerelease suffixes.

Release tags use the form `v1.0.0`. The tag must exactly match the Maven project
version and the packaged `plugin.yml` version. The release workflow rejects
mismatches before publishing.

Write release notes as the annotated tag's message, for example with
`git tag -a v1.0.0` and the editor it opens. Include the release summary and
user-visible changes as Markdown in that message. Lightweight tags and empty
or whitespace-only annotations are rejected. Pushing the tag starts release
validation and publication; create and push it only when authorized to release.

The annotation supplies the notes published on the GitHub release, Modrinth,
and Hangar. Notes live with the release, rather than as records in this tree.
For published notes, see [GitHub Releases](https://github.com/lutzseverino/minecraft-stream-guard/releases).
