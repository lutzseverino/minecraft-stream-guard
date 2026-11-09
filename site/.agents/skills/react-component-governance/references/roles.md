# Component Role Reference

## Purpose

Collect the primary component roles so contributors can classify a component by its main responsibility before considering optional patterns.

Use this reference only when the extracted boundary is a `component boundary`.

Use the canonical role terms consistently: `presentational`, `interactive`, and `container`.

## Table Of Contents

- [presentational](#presentational)
- [interactive](#interactive)
- [container](#container)

## Roles

### `presentational`

#### Purpose

Render UI from explicit inputs while keeping the component centered on display concerns.

#### When To Use

- the component's job is primarily visual
- state is fully controlled from the outside or is trivial and local
- the component can be understood from its props and rendered output alone

#### When Not To Use

- the component fetches data
- the component coordinates multiple services or workflows
- the component owns domain rules rather than display rules

#### API Shape

Prefer explicit props, predictable rendering, and minimal internal behavior.

#### Responsibilities

- render UI for a clearly bounded concern
- translate props into accessible structure and styling
- own only local visual behavior that does not change the component's broader responsibility

It must not own:

- feature orchestration
- remote data loading
- shared business rules

#### Example

A badge, card, row, field wrapper, or read-only display component that receives explicit values and renders them.

#### Anti-Patterns

- hiding network requests behind a generic UI component
- embedding feature-specific decision logic in a reusable visual primitive
- exposing a large prop surface to compensate for mixed responsibilities

#### Checklist

- [ ] Can the component be understood from its props and rendered output?
- [ ] Would removing the UI leave no important business behavior behind?

### `interactive`

#### Purpose

Own meaningful local interaction behavior inside a reusable UI boundary without expanding into feature orchestration.

#### When To Use

- the component owns non-trivial local interaction state
- the behavior is part of the component's reusable UI contract
- the component remains understandable as a reusable UI boundary rather than a feature workflow

#### When Not To Use

- the component is primarily a visual rendering surface with only trivial local behavior
- the component coordinates a feature slice across multiple domain concerns
- the reusable value is only the headless behavior contract and the component shell is incidental

#### API Shape

Expose a stable reusable UI API that includes meaningful interaction behavior while keeping feature-specific orchestration outside the component.

#### Responsibilities

- manage interaction state that belongs to the component's reusable contract
- expose clear actions, props, and callbacks around that local behavior
- preserve reusable UI semantics while owning non-trivial interaction logic

It must not own:

- feature-level workflow orchestration
- shared business rules unrelated to the component contract
- arbitrary behavior that exists only because the component became a convenient place to put it

#### Example

A dropdown, accordion, date input, autocomplete shell, or disclosure component that owns interaction behavior as part of its reusable UI contract.

#### Anti-Patterns

- treating any component with local state as automatically interactive
- embedding domain workflow rules into a reusable interactive control
- avoiding a headless extraction when multiple visual shells need the same contract

#### Relationship To Headless Behavior

`interactive` is a primary role. `headless hook plus UI shell` is an optional pattern.

An interactive component may use the headless pattern when:

- the reusable component still has one clear interactive UI boundary
- the default shell is part of the reusable contract
- the headless behavior needs to be reused or tested separately

Choose the headless pattern without an interactive component shell only when the main reusable asset is the behavior contract itself rather than a reusable interactive UI boundary.

#### Checklist

- [ ] Is the interaction behavior part of the component's reusable contract?
- [ ] Would the component still be understandable outside one feature workflow?

### `container`

#### Purpose

Assemble a feature slice by coordinating narrower components, hooks, and use cases at one explicit composition boundary.

#### When To Use

- a feature requires orchestration across multiple subcomponents or hooks
- one boundary should own the assembly of a workflow
- the component expresses feature composition rather than reusable primitive UI

#### When Not To Use

- the file becomes a god component
- orchestration could be moved into a narrower hook or service
- the component tries to be both reusable primitive and feature-specific container

#### API Shape

Keep the container focused on composition. Delegate rendering details to child components and reusable behavior to hooks or services.

#### Responsibilities

- assemble a feature workflow at one clear boundary
- compose narrower building blocks without swallowing their responsibilities
- keep feature-specific orchestration out of reusable primitives

It must not own:

- low-level reusable UI concerns that should live elsewhere
- business logic that belongs in services or domain-focused modules
- a public API pretending to be generic when the component is feature-specific

#### Example

A dashboard panel that wires filters, query state, actions, and presentational blocks for one feature slice, while delegating each narrower concern to dedicated parts.

#### Anti-Patterns

- placing all feature logic, rendering, and data transformation in one file
- turning a container component into a shared UI primitive
- keeping orchestration inline after it clearly deserves narrower collaborators

#### Checklist

- [ ] Is this component's main job to assemble a feature workflow?
- [ ] Are reusable concerns delegated to narrower components, hooks, or services?

## Related

- [classification.md](./classification.md)
- [patterns.md](./patterns.md)
