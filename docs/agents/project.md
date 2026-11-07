# StreamGuard constraints

Keep domain and application code free of Bukkit, HTTP, YAML, and provider-specific adapter dependencies. Preserve the inward dependency direction and composition root described in [architecture](../development/architecture.md); `ProjectStructureTest` enforces these boundaries.

Retain the Maven wrapper, Java 17 bytecode target, JDK 21 validation, Spotless, dependency convergence, compiler warnings as errors, and plugin jar shading. The CI and release workflows are project-owned. No optional JavaScript stack applies to this Maven-only plugin.
