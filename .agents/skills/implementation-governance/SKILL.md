---
name: implementation-governance
description: Opinionated implementation guidance for code changes across projects and languages. Use when implementing features, fixing bugs, refactoring, reviewing code, or planning a change and you need to decide change kind, scope, extraction, ownership, convention handling, escalation, or validation depth without drifting into opportunistic cleanup or broad, unclear rewrites.
---

# Implementation Governance

Make consistent implementation and materialization decisions without drifting into broad, unclear, or opportunistic code changes.

## Workflow

1. Identify what kind of change this is.
Read [references/workflow.md](references/workflow.md) and [references/change-kinds.md](references/change-kinds.md) first to classify the work with one `primary change kind` and any `secondary change kinds` that materially affect scope, extraction, or validation.

2. Decide how wide the change should be and whether a contract surface is affected.
Use [references/scoping.md](references/scoping.md) to keep the change at the smallest correct scope, decide what stays with the current owner, and determine when broader ownership is actually justified.
If the change alters a public export, interface, schema, route shape, event payload, database contract, or external integration boundary, read [references/escalation.md](references/escalation.md) first. Read [references/contract-changes.md](references/contract-changes.md) only after that contract-affecting change is confirmed.

3. Decide what should stay inline and what should be extracted.
Use [references/extraction.md](references/extraction.md) to choose between `inline`, `local helper`, `local module`, and `boundary`.

4. Re-apply governance inside each meaningful boundary introduced by the task.
Use [references/workflow.md](references/workflow.md) to re-run the full workflow for each meaningful boundary introduced by the task. Use [references/scoping.md](references/scoping.md) and [references/extraction.md](references/extraction.md) to decide which new seams are meaningful and when to stop descending.

5. Materialize the chosen boundaries with local implementation rules.
Use [references/materialization.md](references/materialization.md) and [references/conventions.md](references/conventions.md) to preserve dominant local conventions while keeping responsibilities, public seams, dependency direction, and orchestration shape clear inside each unit.

6. Decide how much validation the change needs.
Use [references/validation.md](references/validation.md) to match validation depth to blast radius and change kind.

7. Escalate when the next correct step is not safe to guess.
Use [references/escalation.md](references/escalation.md) when the change would alter public contracts, widen ownership, standardize a mixed area, or otherwise create lasting consequences beyond the immediate request.

## Working Rules

- Choose one `primary change kind` for the current task.
- Add `secondary change kinds` only when they materially affect scope, extraction, or validation.
- Use the canonical change kinds consistently: `direct fix`, `local refactor`, `boundary extraction`, `cross-cutting refactor`, and `new feature slice`.
- Use the canonical extraction outcomes consistently: `inline`, `local helper`, `local module`, and `boundary`.
- Prefer the smallest correct change.
- Preserve the dominant convention in the touched area before considering wider normalization.
- Preserve acceptable local stack patterns before introducing a technically cleaner alternative.
- Re-run the full workflow at each meaningful boundary introduced by the task until [Change Scoping Policy](references/scoping.md) says the remaining work is straightforward local implementation.
- Do not stop at top-level architecture when the task includes scaffolding, package design, or other structural materialization.
- Materialize each unit with the simplest structure that keeps responsibility, public seams, and dependency direction clear.
- Separate pure local logic from orchestration when mixing them would obscure the unit's main responsibility.
- Shared abstractions must be earned by real reuse or clearly broader ownership.
- Prefer local ownership first after extraction before promoting code into broader shared scope.
- Keep contract surfaces as narrow as the request allows.
- Keep side effects at clear boundaries and prefer extracting pure local logic before sharing orchestration.
- Use narrower framework or domain skills for technology-specific structure. Use this skill to govern change strategy, scope, extraction pressure, and validation depth.

## Output Expectations

For a review, plan, or implementation proposal:

- state the `primary change kind`
- state any `secondary change kinds` only if they materially affect the plan
- state the intended scope
- state the extraction outcome
- explain any contract-surface handling when relevant
- explain any recursive boundary pass only when the task creates meaningful sub-boundaries
- explain the materialization decision whenever the outcome is `local module` or `boundary`, or when local code shape, dependency direction, or orchestration separation materially affects the result
- explain the convention decision
- explain the planned validation depth
- call out any escalation point or confirm that none is needed

Use this response shape:

```text
Primary change kind: ...
Secondary change kinds: ...
Scope: ...
Extraction outcome: ...
Contract note: ...
[Recursive notes: ...]
[Materialization note: ...]
Convention decision: ...
Validation: ...
Escalation note: ...
```

## Reference Map

Use these references directly as needed:

- [references/overview.md](references/overview.md)
- [references/workflow.md](references/workflow.md)
- [references/change-kinds.md](references/change-kinds.md)
- [references/scoping.md](references/scoping.md)
- [references/contract-changes.md](references/contract-changes.md)
- [references/extraction.md](references/extraction.md)
- [references/materialization.md](references/materialization.md)
- [references/conventions.md](references/conventions.md)
- [references/validation.md](references/validation.md)
- [references/escalation.md](references/escalation.md)

## Not For

- framework-specific architecture or folder layouts that belong to a narrower skill
- language-specific style rules that are already governed by repo tooling or conventions
- broad software design philosophy that does not change the current implementation decision
