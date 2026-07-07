# Implementation Governance

## Purpose

Group the implementation governance references by concern so change classification, scope control, extraction, materialization, convention handling, validation, and escalation remain clear and independently maintainable.

Use [Workflow](./workflow.md) as the operational entry point when making a decision.

## Documents

- [Workflow](./workflow.md)
- [Change Kind Policy](./change-kinds.md)
- [Change Scoping Policy](./scoping.md)
- [Contract Change Policy](./contract-changes.md)
- [Extraction Policy](./extraction.md)
- [Materialization Policy](./materialization.md)
- [Convention Handling Policy](./conventions.md)
- [Validation Policy](./validation.md)
- [Escalation Policy](./escalation.md)

## Scope Boundaries

- Workflow defines the order of operations for a change.
- Change kind policy defines what kind of work is being performed and what defaults it carries.
- Scoping policy defines how far the change should reach, how ownership levels are interpreted, and when broader ownership is justified.
- Contract change policy defines how an approved contract-affecting change should be carried out safely.
- Extraction policy defines whether code should stay `inline`, become a `local helper`, move into a `local module`, or become a `boundary`.
- Materialization policy defines how code inside a chosen unit should keep responsibility, public seams, dependency direction, and orchestration shape clear.
- Workflow is recursive when the task introduces meaningful new boundaries that must be materialized. Scoping defines which boundaries are meaningful and when recursion should stop.
- Convention handling defines how to preserve dominant local patterns, acceptable stack patterns, dependency shapes, and side-effect placement without silently standardizing a wider area.
- Validation policy defines how much validation the change needs based on risk and blast radius.
- Escalation policy defines when the next step should be confirmed instead of guessed.

## Decision Flow

1. Start with [Workflow](./workflow.md) to follow the decision sequence.
2. Use [Change Kind Policy](./change-kinds.md) to classify the work with one `primary change kind` and any `secondary change kinds` that materially affect the plan.
3. Use [Change Scoping Policy](./scoping.md) to choose the smallest correct change, identify the correct owner level, and determine whether broader ownership is actually required.
4. If the change affects a contract surface, use [Contract Change Policy](./contract-changes.md) after approval to choose the narrowest safe contract change.
5. Use [Extraction Policy](./extraction.md) to decide what stays `inline` and what should become a `local helper`, `local module`, or `boundary`.
6. Re-apply the full [Workflow](./workflow.md) inside each meaningful boundary introduced by the task until [Change Scoping Policy](./scoping.md) says the remaining work is straightforward local implementation.
7. Use [Materialization Policy](./materialization.md) and [Convention Handling Policy](./conventions.md) to shape local code clearly without accidental coupling or silent standardization.
8. Use [Validation Policy](./validation.md) to meet the minimum validation floor for the change kind and blast radius.
9. Use [Escalation Policy](./escalation.md) when broader consequences, unclear ownership, or mixed conventions make the next step unsafe to assume.
