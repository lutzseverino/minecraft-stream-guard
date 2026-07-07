# Change Scoping Policy

## Default Rule

Choose the smallest correct change.

The correct scope is the narrowest set of files, ownership levels, and contracts that must change to satisfy the request safely.

## Keep The Change Local By Default

Prefer the current ownership level when:

- the behavior belongs clearly to the touched ownership level
- the change corrects or clarifies existing behavior
- the extracted logic is only used by one ownership level
- broader reuse is speculative rather than present

## Widen Scope Only For Real Reasons

Broader scope is justified when one or more of these are true:

- the same change is required across multiple ownership levels
- the extracted code clearly belongs to a broader owner that already exists
- the current owner mixes responsibilities in a way that directly blocks a correct change
- a public contract, schema, API, or integration boundary must change to satisfy the request

## Ownership Rules

- Interpret `owner` as the nearest existing ownership level in this precedence order:
  1. the current function, method, or equivalent executable unit
  2. the current file, class, module, or equivalent local code unit
  3. the current package, feature, service, or equivalent local area
  4. an existing broader shared area
- Keep code with the nearest level in that order that already owns the responsibility.
- After extracting a `boundary`, keep it at the nearest ownership level that already owns the responsibility.
- Promote an extracted `boundary` into broader shared scope only when ownership is clearly broader and already lives at level 4.
- Promote code into broader shared scope only when ownership is clearly broader than the current area.
- If broader ownership seems correct but the right home is unclear from the repo structure, ask the user instead of guessing.
- Do not create a new shared home only because multiple future uses seem possible.

## Contract Surface Rules

Treat public exports, interfaces, schemas, route shapes, event payloads, database contracts, and external integration boundaries as contract surfaces.

- Keep a contract surface as narrow as the request allows.
- Prefer changing an internal seam before widening a public contract.
- If a contract surface must widen or change shape, include that in the scope explicitly rather than treating it as incidental fallout.
- If the request implies a contract change but does not acknowledge its broader consequence, escalate before proceeding.
- After approval, follow [Contract Change Policy](./contract-changes.md) instead of treating the approved change as an ordinary local edit.

## Mixed-Area Rule

If the touched area contains mixed conventions or uneven structure:

- preserve the dominant pattern of the local owner you are changing
- avoid widening the change just to make nearby code uniform
- escalate if choosing one pattern would effectively standardize a wider area

## Depth And Stop Rules

Descend into another layer of decision-making only when the task introduces or reshapes a meaningful boundary.

Treat these as meaningful boundaries:

- a new package, feature slice, service, module, adapter, or boundary
- a new public seam or entrypoint
- a structural split between units with different reasons to change

Do not descend further for:

- trivial helpers
- straightforward local implementation inside a clearly owned unit
- structure that is already obvious from nearby convention

Stop when all of these are true:

- the remaining unit is clearly owned
- the remaining unit is local in effect
- package, module, ownership, and public-seam decisions are settled
- nearby convention is already clear enough to materialize the unit without another boundary decision

## Common Scoping Errors

- fixing one bug by rewriting the whole subsystem
- promoting local code into shared scope before real reuse exists
- broad cleanup during a feature request
- changing unrelated call sites because they are nearby
- guessing broader ownership when the architecture does not make it clear
