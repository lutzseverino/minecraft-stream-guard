# Workflow

## Order Of Operations

1. Identify the change kind before changing structure.
Classify the task with one `primary change kind`: `direct fix`, `local refactor`, `boundary extraction`, `cross-cutting refactor`, or `new feature slice`. Add `secondary change kinds` only when they materially change scope, extraction, or validation.

2. Choose the smallest correct scope.
Decide what must change to satisfy the request and what should remain untouched. Do not widen the change just because nearby code could also be improved. Make any contract surface touched by the change explicit. If the change affects a contract surface, escalate or confirm that broader consequence before applying the contract-change policy.

3. Choose the extraction outcome only after the scope is clear.
Decide whether the change should stay `inline`, become a `local helper`, move into a `local module`, or become a `boundary`. After extraction, keep the boundary at the nearest ownership level first. Prefer extracting pure local logic before sharing code that carries side effects or orchestration.

4. Re-run the workflow inside each meaningful boundary introduced by the task.
If the task introduces or reshapes a meaningful boundary, re-run the full workflow for that unit before materializing it fully.

5. Materialize the change using local implementation rules and the dominant convention in the touched area.
Preserve local naming, file placement, entrypoint, dependency, side-effect, and validation conventions when they are clearly established in the area you are changing.
Keep responsibilities legible, public seams narrow, dependency direction obvious, and orchestration separate from pure local logic when mixing them would blur the unit's owned responsibility or seam.
Do not replace an acceptable local stack pattern with a technically cleaner alternative unless the existing pattern is materially harmful, directly blocks the change, or the request explicitly includes standardization.

6. Match validation depth to the blast radius.
Meet the minimum validation floor for the change kind and blast radius, then add more coverage only when the risk requires it.

7. Escalate instead of guessing when the consequence is wider than the request.
Ask before changing public contracts, promoting code into broader ownership with unclear placement, standardizing a mixed area, or making a high-risk change without credible validation.

## Recursive Boundary Pass

Use a recursive boundary pass when the task:

- scaffolds a new project area, package tree, or feature slice
- introduces a new module, package, service, adapter, or public seam
- splits one owner into sub-units with different reasons to change
- turns a high-level architecture plan into concrete files, folders, or packages

At each depth, re-run the same workflow:

1. classify the local change kind if it still matters at this layer
2. confirm the smallest correct local scope and any contract surface
3. decide what stays `inline`, what becomes a `local helper` or `local module`, and what deserves a `boundary`
4. materialize the chosen unit with clear responsibility, narrow seams, obvious dependency direction, and explicit orchestration shape
5. recurse again only if that decision creates another meaningful boundary
6. apply local convention, validation, and escalation rules for the unit

Use [Change Scoping Policy](./scoping.md) as the canonical stop rule for when to stop descending.

## Default Sequence

When in doubt, follow this default sequence:

1. classify the change kind
2. keep the scope local and make any contract surface explicit
3. keep code `inline` unless clarity or ownership clearly improves with extraction
4. keep extracted code at the nearest ownership level first
5. re-run the same governance questions for each meaningful boundary created by the task
6. materialize each unit with clear responsibility, narrow seams, and obvious dependency direction
7. preserve the local convention in the touched area
8. keep an acceptable local stack pattern unless the task or the current harm clearly justifies breaking from it
9. meet the minimum relevant validation floor
10. escalate only when the wider consequence is real and unclear

## Common Failure Modes

- widening a `direct fix` into unrelated cleanup
- forcing mixed work into one label and losing the real source of risk
- treating a `local refactor` as justification for new shared abstractions
- extracting code only to reduce file length or satisfy a style preference
- stopping at top-level architecture while leaving lower-level boundaries ad hoc
- materializing a boundary with incidental coupling or mixed responsibilities inside it
- widening a contract surface when an internal seam would suffice
- sharing orchestration code before the underlying logic has a stable local shape
- replacing an acceptable local stack pattern with a cleaner but disruptive one
- standardizing a mixed area without being asked
- changing behavior and calling it a refactor
- skipping escalation when public contracts or broader ownership are affected
