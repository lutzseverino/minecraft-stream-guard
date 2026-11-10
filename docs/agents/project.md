# StreamGuard constraints

The repository has `plugin/` (Maven/Java) and `site/` (pnpm/React/Vite) components.
Run their canonical checks through the root command in
[development](../development/README.md). Keep component tools, lockfiles,
licenses, versions, and READMEs with their component; contribution and delivery
guidance has one root owner.

Keep plugin domain and application code free of Bukkit, HTTP, YAML, and
provider-specific adapter dependencies. Preserve the inward dependency direction
and composition root described in [architecture](../development/architecture.md);
`plugin/src/test/java/com/lutzseverino/streamguard/ProjectStructureTest.java`
enforces these boundaries. Retain Java 17 bytecode, JDK 21 validation, Maven
wrapper, Spotless, dependency convergence, warnings as errors and jar shading.

For site changes, read [site development](../../site/docs/development/README.md).
Keep generated registry files byte-exact and preserve the dependency-cruiser
boundaries and `/api/live` HTTP contract. Static builds do not prove hosting or
public-feed acceptance.

CI and plugin release workflows are project-owned. Plugin publication uses
annotated `plugin-v…` tags, component/version checks, and the existing release
environment and distribution identities. Historical `v…` refs/releases remain.
The root factory image supplies both component toolchains.
