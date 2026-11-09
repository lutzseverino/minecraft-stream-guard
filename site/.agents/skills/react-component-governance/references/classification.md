# Component Classification Policy

## Purpose

Classify component boundaries with a truthful model that separates primary responsibility from optional API or composition patterns.

## Context

Use this reference only after deciding that the extracted boundary is a `component boundary`.

Component classification does not define folder layout. It defines only the conceptual model used to describe component boundaries.

This policy uses two axes:

- primary role
- optional patterns

A component should have one primary role and may use zero or more optional patterns when they fit its responsibility.

Detailed role and pattern references live in:

- [roles.md](./roles.md)
- [patterns.md](./patterns.md)

## Out Of Scope For Component Roles

Do not use component roles for boundaries whose main value is:

- a reusable headless behavior contract with no required UI shell
- provider or infrastructure wiring
- local support code such as loading or error handling that serves another owner

For those outcomes, keep the boundary kind explicit and skip primary component roles.

## Primary Roles

Choose one primary role for each extracted component boundary:

- `presentational`
  Render UI from explicit inputs while staying centered on display concerns.
- `interactive`
  Own meaningful local interaction behavior inside a reusable UI boundary.
- `container`
  Assemble a feature workflow at one explicit composition boundary.

## Optional Patterns

A component may use one or more of these patterns when they strengthen the chosen primary role:

- `compound`
  Expose a coordinated API through multiple related subcomponents.
- `headless hook plus UI shell`
  Separate reusable interaction logic from rendering.
- `slot-based or polymorphic`
  Preserve a stable responsibility while allowing controlled rendering flexibility.

## Forbidden Classifications

- Treating roles and patterns as if they were one mutually exclusive list
- Defining a component with no clear primary role
- Using a pattern as the only explanation of a component's responsibility
- Classifying a generic primitive as a container because it happens to coordinate state

## Decision Guide

1. Identify the component's primary responsibility.
2. Choose one primary role that matches that responsibility.
3. Add optional patterns only if they strengthen the chosen role.
4. Reject any pattern that weakens clarity or creates speculative flexibility.

## Role Tie-Breakers

When more than one role feels plausible, choose the role that matches the component's main value:

- choose `presentational` when the main value is rendering explicit inputs and any local state is only incidental UI behavior
- choose `interactive` when the main value is a reusable UI contract whose interaction behavior is part of the boundary itself
- choose `container` when the main value is assembling a feature workflow across narrower components, hooks, or use cases

Use these comparisons when the choice still feels close:

- `presentational` vs `interactive`: if removing the local behavior would leave the same meaningful component contract, prefer `presentational`; if the behavior is part of why the boundary is reusable, prefer `interactive`
- `interactive` vs `container`: if the boundary is mainly a reusable control surface, prefer `interactive`; if it mainly coordinates a feature slice, prefer `container`
- `presentational` vs `container`: if the boundary is understandable from props and rendered output, prefer `presentational`; if it exists to orchestrate feature concerns, prefer `container`

## Allowed Patterns

- one primary role plus zero or more optional patterns
- presentational plus compound when composition is the API
- interactive plus headless when a reusable interactive component exposes one default shell and a reusable behavior contract
- interactive plus compound when coordinated parts expose one reusable interactive surface
- interactive plus slot-based when behavior stays stable while the rendering surface varies
- presentational plus slot-based when structure stays stable but rendering varies
- container plus headless behavior when orchestration consumes a reusable interaction contract

## Forbidden Patterns

- selecting several primary roles for one component
- treating compound or polymorphic behavior as a substitute for choosing a role
- using patterns to hide a component with mixed responsibilities

## Related

- [roles.md](./roles.md)
- [patterns.md](./patterns.md)
- [extraction.md](./extraction.md)
- [packaging.md](./packaging.md)
- [ownership.md](./ownership.md)
