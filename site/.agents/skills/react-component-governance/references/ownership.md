# Component Ownership And Sharing Policy

## Purpose

Define how responsibilities stay local by default and when code may be promoted into broader shared modules.

## Context

Most frontend entropy starts when code is shared too early or stored in locations that do not reflect ownership. This policy keeps dependencies and abstractions honest by defaulting to local scope first.

## Policy

### Local First

Code should live in the narrowest scope that fully owns its reason to change.

By default:

- component-specific helpers stay inside the component boundary
- feature-specific logic stays inside the feature boundary
- shared modules are earned through demonstrated reuse or broader responsibility

### Promotion Rule

Move code upward only when at least one of these is true:

- the code is reused by multiple owners
- the code represents a broader domain concept than the current owner
- the current owner is becoming responsible for concerns it should only consume

When promoting code, the new destination must express clearer ownership than the old one.

If broader placement is clearly appropriate but the correct broader owner is not clear from the repo structure, ask the user before creating a new feature-, domain-, or app-scoped location.

### Shared Utilities

Shared utilities are allowed only when they satisfy all of the following:

- they have a stable responsibility
- they are not tied to component rendering details
- they are not secretly feature-specific
- reuse is real rather than anticipated

Domain-scoped or app-scoped utilities follow the same rule. Promote them only when the broader owner is clear and the responsibility is genuinely broader than the current boundary.

### Exception Documentation

Do not require explanatory comments for routine boundary-owned support files that already follow packaging policy, such as `ComponentName.types.ts` or `ComponentName.utils.ts`.

Require explicit rationale only for real standard deviations, such as:

- cross-layer dependency exceptions
- temporary compatibility shims
- ownership violations accepted for a bounded migration

When a rationale is needed, place it near the deviation in a short, dated comment or link it from the owning document or ADR.

## Allowed Patterns

- Keeping logic local until repeated use is proven
- Promoting a local helper into shared scope after concrete reuse
- Extracting a shared contract while keeping feature-specific implementation local
- Asking the user when broader ownership is appropriate but several plausible destinations exist

## Forbidden Patterns

- Creating shared helpers "just in case"
- Moving code to global `utils` because its owner feels crowded
- Sharing component-specific assumptions across unrelated features
- Hiding architectural exceptions in undocumented convenience code
- Guessing a broader owner when the repo structure does not make that ownership clear

## Tradeoffs

This policy prefers slight local duplication over premature abstraction.

That tradeoff is intentional. Wrong abstractions spread complexity farther than duplicated local code.

## Related

- [classification.md](./classification.md)
- [packaging.md](./packaging.md)
- [extraction.md](./extraction.md)
