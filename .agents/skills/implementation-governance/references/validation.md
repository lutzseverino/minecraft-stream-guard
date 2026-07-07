# Validation Policy

## Default Rule

Validation depth must match blast radius.

Meet the minimum relevant validation floor first, then add more coverage only when the actual risk requires it.

## Minimum Floors

- If the touched owner already has automated validation, run the most relevant existing automated validation for that owner or state why it could not be run.
- If a contract surface changes, include at least one direct contract-level validation path.
- If only manual validation is available, state the exact path exercised and what remains unverified.
- Do not rely on compile, typecheck, or lint alone for a behavior change unless the changed behavior is purely static and that claim is explicit.

## Validation By Change Kind

### `direct fix`

Prefer targeted validation of the corrected path.

Minimum floor:

- reproduce or exercise the corrected path directly
- if the owner already has automated validation, run the most relevant existing automated validation for that area

Examples:

- the smallest relevant test selection
- a focused query or command
- a direct reproduction and confirmation path
- a narrow compile, typecheck, or lint target when that is the most relevant signal

### `local refactor`

Validate behavior parity at the owner level.

Minimum floor:

- run existing owner-level automated validation when it exists, or
- perform a focused manual parity check and state the missing automation

Prefer:

- existing tests that cover the owner
- targeted tests plus a narrow static check
- focused manual validation when automation does not exist

### `boundary extraction`

Validate before-and-after behavior around the extracted seam.

Minimum floor:

- exercise the original owner path directly
- run existing owner-level automated validation when it exists

Prefer:

- targeted coverage of the owner and extracted boundary
- parity checks for the moved logic
- the narrowest integration check that proves the extraction did not alter behavior

### `cross-cutting refactor`

Use broader validation because the blast radius is broader.

Minimum floor:

- exercise at least one representative path for each touched pattern
- run the broadest existing automated validation that is still bounded to the affected area

Prefer:

- targeted coverage for each touched pattern
- broader compile, type, lint, or integration checks in the affected area
- selective regression coverage where contract surfaces or multiple owners are involved

### `new feature slice`

Validate the new behavior directly and verify any touched contracts proportionately.

Minimum floor:

- exercise the new path directly
- run existing automated validation for the touched owner when it exists
- add direct contract validation when the feature changes a contract surface

Prefer:

- focused validation of the new path
- nearby owner-level checks
- contract or integration checks when the feature crosses contract surfaces or boundaries

## Validation Rules

- Prefer existing validation entrypoints before inventing new infrastructure.
- Add or update validation when the repo already supports it and the change would otherwise be weakly verified.
- Do not widen a narrow change into a broad test rewrite unless the request or risk clearly requires it.
- If credible automated validation is unavailable, use the best targeted static or manual validation available and state what remains unverified.

## Common Errors

- running broad validation when a targeted check would prove the change
- claiming a refactor is safe without parity checks
- treating compilation alone as sufficient for a behavioral change
- changing a contract surface without direct contract-level validation
- skipping validation details when the change affects a public contract or multiple owners
