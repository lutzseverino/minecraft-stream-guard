# Component Pattern Reference

## Purpose

Collect optional component patterns so contributors can add composition or API patterns without confusing them with primary responsibilities.

## Table Of Contents

- [compound](#compound)
- [headless hook plus UI shell](#headless-hook-plus-ui-shell)
- [slot-based or polymorphic](#slot-based-or-polymorphic)

## Patterns

### `compound`

#### Purpose

Expose a coordinated API through multiple related subcomponents that form one conceptual component.

#### When To Use

- consumers need a flexible composition surface
- a parent component must coordinate state and semantics across child slots
- a flat prop API would become bloated or unreadable

#### When Not To Use

- a single component with explicit props is sufficient
- the relationship between parts is weak
- the pattern would hide required constraints from consumers

#### API Shape

Provide one conceptual component with named subcomponents and a clear parent-child contract.

#### Responsibilities

- coordinate related subcomponents through a narrow shared contract
- preserve semantics across the composed surface
- keep shared context limited to the compound boundary

It must not own:

- unrelated feature orchestration
- hidden side effects unrelated to composition
- a vague API that lets consumers bypass the component's semantics

#### Example

A `Tabs` API with `Tabs.List`, `Tabs.Trigger`, and `Tabs.Content`, or a form field family that shares a narrow accessibility contract.

#### Anti-Patterns

- using the pattern to avoid naming props clearly
- exposing subcomponents that can be used in meaningless combinations
- leaking internal coordination details into the public API

#### Checklist

- [ ] Is composition itself the reason this pattern is needed?
- [ ] Does the shared contract express real semantics instead of incidental implementation details?

### `headless hook plus UI shell`

#### Purpose

Separate interaction logic from rendering so behavior can be reused without fixing the visual presentation.

#### When To Use

- the behavior is reusable across multiple visual representations
- interaction logic is non-trivial
- the visual layer should remain replaceable

#### When Not To Use

- the logic is too small to justify separation
- the hook exists only to move code out of sight
- consumers would need to reconstruct too much implicit behavior

#### API Shape

The hook owns behavior and exposes a stable interaction contract. The UI shell consumes that contract and owns rendering concerns.

#### Responsibilities

- isolate reusable interaction state and actions
- keep rendering replaceable without duplicating the behavior contract
- preserve dependency direction from UI to behavior, not the reverse

It must not own:

- presentation details inside the hook
- hidden coupling that makes the shell effectively mandatory
- abstractions introduced without repeated need

#### Example

A selection hook reused by both a compact dropdown and a full-screen picker, or a date-range interaction model consumed by multiple visual shells.

The pattern can be combined with:

- an interactive component role, when one default reusable shell remains part of the public component contract
- a container component role, when the pattern supports feature orchestration rather than a reusable control boundary

#### Anti-Patterns

- extracting a hook that is only one component's trivial local state
- returning a grab bag of unrelated values and callbacks
- coupling the hook to specific DOM structure or styling classes

#### Checklist

- [ ] Would the behavior still make sense with a different visual shell?
- [ ] Does the hook expose a coherent contract instead of implementation fragments?

### `slot-based or polymorphic`

#### Purpose

Provide a reusable structural boundary whose semantics stay stable while selected rendered elements or slot content may vary.

#### When To Use

- semantics stay stable while the rendered element changes
- the component provides layout, accessibility, or structural guarantees
- consumers need controlled flexibility at extension points

#### When Not To Use

- polymorphism exists only for convenience
- the API obscures semantics
- the component becomes a generic escape hatch for arbitrary rendering

#### API Shape

Expose explicit slots or typed extension points while preserving one stable responsibility independent of the rendered tag.

#### Responsibilities

- maintain semantic and accessibility guarantees
- allow controlled variation in rendering surface
- keep the public contract narrower than the total implementation surface

It must not own:

- arbitrary feature logic hidden behind flexible rendering
- polymorphism used as a substitute for naming the right abstraction
- extension points with no semantic boundaries

#### Example

A button-like action surface that may render a link or button while preserving the same interaction responsibility, or a layout shell with controlled header/body/footer slots.

#### Anti-Patterns

- using `asChild` or similar escape hatches without stable semantics
- allowing arbitrary slot combinations that break the component contract
- turning one reusable boundary into a pass-through wrapper for anything

#### Checklist

- [ ] Does the component still have one clear responsibility regardless of the rendered element?
- [ ] Are the extension points intentional and semantically bounded?

## Related

- [classification.md](./classification.md)
- [roles.md](./roles.md)
