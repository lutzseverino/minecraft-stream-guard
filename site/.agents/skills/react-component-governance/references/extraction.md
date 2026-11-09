# Component Extraction Policy

## Purpose

Define when a meaningful UI boundary should be introduced and when extraction would only add accidental complexity.

## Context

Extraction should follow responsibility and readability, not line count alone. The goal is to create meaningful UI boundaries, not to fragment rendering into many shallow wrappers.

This policy is the first decision point in component design. It determines whether code should stay `inline`, become a `local helper`, become a `component boundary`, or become another boundary kind.

## Policy

### Choose `inline`

Choose `inline` when:

- the markup belongs to one local responsibility
- extraction would only proxy props without adding meaning
- the code is easier to understand in its current owner
- the section does not introduce repeated named substructure, repeated render branches, or a distinct local API surface

### Choose `local helper`

Choose `local helper` when:

- a named UI concept improves readability
- a subsection has one clear responsibility inside the owner
- separation helps comprehension but reuse is still local
- repeated markup, repeated conditional branches, or clustered handlers make the owner harder to scan but do not justify a broader boundary

Keep a `local helper` inside the owning boundary rather than promoting it into shared component space.

### Choose `component boundary`

Choose a `component boundary` when:

- the UI boundary represents a named concept used by multiple owners
- the boundary has a stable responsibility and a clear API
- the extraction improves both readability and ownership clarity
- the extracted boundary needs its own explicit prop contract and a real reusable UI contract

Owned support files such as `ComponentName.types.ts`, `ComponentName.utils.ts`, or `ComponentName.hooks.ts` do not by themselves justify a new `component boundary`. If those files stay owned by the current boundary, keep the extraction local and let packaging rules decide the file split.

### Choose Another Boundary Kind Instead Of A Component

Do not force every extraction into a component boundary.

Choose another boundary kind when:

- the reusable value is primarily a behavior contract with no required UI shell
- the main responsibility is provider, infrastructure, or context wiring
- the extracted code mainly supports another owner, such as loading or error handling, rather than standing as an ordinary reusable component

Keep the resulting boundary kind explicit:

- `headless behavior boundary`
- `provider/infrastructure boundary`
- `support boundary`

### Avoid Extraction

Avoid extraction when:

- the only goal is reducing file length
- the new component would forward props without owning meaning
- the abstraction is speculative rather than driven by real need

## Tie-Breaker Signals

Use these signals to support the extraction choice:

- prefer `inline` when one render path or one small conditional remains easy to scan in the current owner
- prefer `local helper` when one named subsection accumulates repeated markup, multiple conditional branches, or several related handlers inside the current owner
- prefer `component boundary` when the extracted UI has an explicit public prop contract, a stable reusable UI contract, and multiple owners
- prefer another boundary kind when the reusable value is behavior, infrastructure, or support for another owner rather than a reusable UI contract

Treat file length, statement count, and method count as supporting signals only. Do not use them as standalone reasons to extract.

## Allowed Patterns

- keeping a small piece of markup inline until a named boundary becomes clear
- extracting a local helper before promoting it into public reusable space
- extracting boundary-owned support files while keeping one existing boundary
- promoting a component boundary after real reuse or stable architectural ownership emerges
- extracting reusable headless behavior when several UI shells need the same contract
- keeping provider or infrastructure wiring out of ordinary component classification when that is its real responsibility

## Forbidden Patterns

- extracting for aesthetics alone
- creating thin wrapper components with no semantic responsibility
- promoting local view fragments into shared space before reuse is proven
- hiding feature-specific assumptions behind generic component names
- forcing a headless behavior or infrastructure concern into component form just to preserve naming symmetry

## Decision Guide

1. Does this UI represent a distinct responsibility?
2. Would a good name improve understanding?
3. Is the responsibility still local to the current owner?
4. If extracted, is the right outcome `local helper`, `component boundary`, or another boundary kind?
5. Is reuse real, or only anticipated?
6. Would extraction clarify ownership more than it increases indirection?

If the answer to the first question is no, choose `inline`. If the answer to the first two questions is yes but reuse is still local, choose `local helper`. If the boundary has stable responsibility and multiple owners, extract a reusable boundary whose kind matches its real responsibility. Use a `component boundary` only when the reusable value includes a meaningful UI contract.

When the choice still feels close:

- prefer `inline` over `local helper` when the section still scans as one local responsibility
- prefer `local helper` over `component boundary` when the extracted code has no stable external API and no second owner
- prefer another boundary kind over `component boundary` when the reusable value is not primarily UI

## Related

- [classification.md](./classification.md)
- [packaging.md](./packaging.md)
- [ownership.md](./ownership.md)
