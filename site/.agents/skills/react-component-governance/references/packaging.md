# Component Packaging Policy

## Purpose

Create a predictable filesystem structure for UI boundaries without turning folder layout into ceremony detached from responsibility.

## Context

Packaging rules should reveal ownership, public API boundaries, and expected reuse. They should not force identical layouts for files with different responsibilities.

Packaging is independent of component role. A presentational component and a compound component may use the same packaging.

Boundary-owned files stay with that boundary unless broader ownership or an established repo convention clearly requires otherwise.

For this policy, the `local owner` is the nearest folder, feature area, or route area that already owns the boundary. A reusable `component boundary` counts as outside its local owner when files outside that local owner import it directly.

A local owner should usually present one reusable packaging style. Treat a flat reusable component area as an area-level convention, not as an ad hoc per-component choice.

## Table Of Contents

- [Reusable Component Boundaries](#reusable-component-boundaries)
- [Private Local Render Helpers](#private-local-render-helpers)
- [UI Primitives](#ui-primitives)
- [Headless Behavior Boundaries](#headless-behavior-boundaries)
- [Provider Or Infrastructure Boundaries](#provider-or-infrastructure-boundaries)
- [Support Boundaries](#support-boundaries)
- [Component Folder Contents](#component-folder-contents)
- [File Naming Convention](#file-naming-convention)
- [Allowed Patterns](#allowed-patterns)
- [Forbidden Patterns](#forbidden-patterns)
- [Tradeoffs](#tradeoffs)
- [Related](#related)

## Policy

### Reusable Component Boundaries

A reusable `component boundary` uses its own folder by default when it is imported directly outside its local owner.

Expected folder shape:

```text
ComponentName/
  ComponentName.tsx
  ComponentName.types.ts
  ComponentName.utils.ts
  index.ts
```

Use a flatter reusable component shape only when an established repo convention in that area clearly uses flat reusable components and ownership remains obvious.

Do not treat mixed sibling shapes as the steady-state default for one local owner. If an area is intentionally flat, keep reusable boundaries in that area flat unless a richer boundary is clearly separated into its own subarea. If an area is folder-based, keep reusable boundaries folder-based unless the local owner explicitly designates a thin wrapper or primitive exception.

Treat mixed sibling shapes within one local owner as acceptable only when they reflect:

- an explicit migration in progress
- a clearly separated subarea such as primitives or vendor-aligned thin wrappers
- a local owner whose convention is already stable and intentionally mixed

### Private Local Render Helpers

Private helper components do not need their own folders. Keep them in the owner file by default. Move them to a sibling file only when the helper is consumed by another file owned by the same boundary or when the owner already carries multiple extracted local helpers and in-file placement no longer keeps each helper obvious. Do not treat them as public API boundaries.

### UI Primitives

Low-level UI primitives in a designated primitives area such as `src/components/ui/` may remain single-file components when they are intentionally thin wrappers or vendor-aligned primitives.

### Headless Behavior Boundaries

Reusable headless behavior does not need component-style packaging. Use a focused hook file by default. Use a small folder only when the boundary owns related types, tests, or helpers. Make the behavior contract obvious without pretending the hook is a component.

Do not declare a custom hook inside a component body.

Keep a private hook in the same file only when it remains at module scope, serves one owner, and is not consumed by tests, stories, or sibling files. Use a standalone hook file when the hook is reused, consumed by multiple files, or owns related support files.

### Provider Or Infrastructure Boundaries

Provider or infrastructure code should live with the narrowest owner that needs it. Do not force it into reusable component packaging unless it truly exposes a stable shared API.

### Support Boundaries

Loading, error, and similar support boundaries should stay local by default. Promote them only when they become a named concept with a stable contract.

## Component Folder Contents

Inside a component folder, include only files with clear responsibilities owned by that boundary.

- `ComponentName.tsx`
- `ComponentName.types.ts`
- `ComponentName.utils.ts`
- `ComponentName.constants.ts`
- `ComponentName.hooks.ts` or other focused prefixed hook files
- `index.ts`
- private subcomponents
- colocated tests or stories when the team chooses that strategy

This general placement rule applies to types, utils, hooks, private subcomponents, tests, and stories that are owned by the boundary.

Keep support files inline by default. Extract them only when the rules below justify it.

If a reusable `component boundary` imported outside its local owner owns two or more extracted support files, keep the folder shape even in areas that sometimes allow flat reusable components.

When extracted, prefer boundary-prefixed filenames so IDE search and filename discovery remain obvious in large codebases.

Use `ComponentName.types.ts` only when one of these is true:

- one or more types are exported from the boundary
- one or more types are imported by another file owned by the boundary
- tests or stories in separate files consume the same boundary-owned types

Otherwise keep types inline.

Use `ComponentName.utils.ts` only when helper logic is used by two or more files owned by the boundary and is still owned by the component's responsibility.

Otherwise keep helper logic inline or as a private local helper.

Use `ComponentName.hooks.ts` or another focused prefixed hook file when hook logic is used by multiple files owned by the boundary and is still owned by the component's responsibility.

Do not move logic into shared utility modules until reuse or broader ownership justifies it.

If tests or stories exist, colocate them with the boundary unless an established repo convention clearly places them elsewhere. Do not promote a boundary into broader scope only to host tests or stories.

## File Naming Convention

When a boundary extracts support files, prefer boundary-prefixed filenames for discoverability.

Examples inside `ComponentName/`:

- `ComponentName.tsx`
- `ComponentName.types.ts`
- `ComponentName.utils.ts`
- `ComponentName.hooks.ts`
- `ComponentName.constants.ts`
- `index.ts`

Keep generic names only when the file is not a standalone extracted support file or when an established repo convention already clearly governs the area.

For standalone hook files:

- use `ComponentName.hooks.ts` for a boundary-scoped hook file inside `ComponentName/`
- follow the established project convention when the area clearly uses `useThing.ts` or `use-thing.ts`
- default to `use-thing.ts` when the convention is mixed or unclear
- ask the user before introducing a broader shared naming convention that the repo structure does not make clear

## Allowed Patterns

- Reusable component folder with a narrow local API as the default packaging
- Flatter reusable component packaging only when an established repo convention clearly governs that area
- One reusable packaging style per local owner as the default expectation
- Local helper file scoped under the owning component
- Single-file primitive in a designated primitives area
- Focused hook file or small folder for reusable headless behavior
- Boundary-prefixed support filenames for extracted files inside a boundary folder
- Colocated tests or stories for the boundary when the repo convention allows them

## Forbidden Patterns

- Reusable component boundary exposed as an unowned loose file outside the primitive-ui exception
- Ad hoc mixing of flat reusable component files and folder-packaged reusable components under one local owner without a clear area boundary or migration reason
- Component folders filled with generic dumping-ground files
- Shared `utils` modules created before repeated use or broader ownership exists
- Exporting private helper subcomponents as if they were reusable API
- Packaging a hook-first boundary as if it were a component just to satisfy naming symmetry
- Filling the codebase with ambiguous standalone names such as `utils.ts` or `types.ts` for extracted support files across many boundary folders

## Tradeoffs

This policy values predictable ownership and area-level consistency over maximal per-component flexibility.

It deliberately allows exceptions for UI primitives and intentionally flat wrapper areas because strict symmetry would sometimes add structure without adding meaning.

## Related

- [classification.md](./classification.md)
- [ownership.md](./ownership.md)
- [extraction.md](./extraction.md)
