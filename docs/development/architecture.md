# Architecture

StreamGuard keeps policy in plain Java and side effects at the edges.

## Responsibility Boundaries

- `domain` owns policy values, session state, stream status, and bypass rules.
- `application` owns use cases and ports for providers and persistence.
- `config` owns typed settings and validation.
- `i18n` owns locale fallback and message rendering.
- `infrastructure` owns Twitch, YouTube, and YAML adapters.
- `platform.bukkit` translates Bukkit events and commands into application calls.
- `bootstrap` composes dependencies and controls plugin lifecycle.

Dependencies point inward: Bukkit, HTTP, YAML, and provider-specific formats do
not belong in domain decisions. Provider identity is modeled as data through
`StreamProviderId`, so adding a provider does not require a closed core enum.

## Side-Effect Boundaries

Network calls, file access, scheduling, logging, and Bukkit operations are
adapter concerns. Application services coordinate them through interfaces;
domain objects remain small and testable. Commands and listeners should perform
translation and permission checks, then delegate rather than grow independent
policy.

The plugin composition root is the one place where concrete adapters should be
wired together. This makes lifecycle ownership visible and avoids service-locator
or static-global dependencies.
