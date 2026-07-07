# Extraction Policy

## Canonical Outcomes

Use these outcome terms consistently:

- `inline`
- `local helper`
- `local module`
- `boundary`

## Outcome Definitions

### `inline`

Keep the code in its current function, method, type, class, file, or owner.

Choose `inline` when:

- the change is small and directly supports the surrounding logic
- the responsibility is still clear while reading the owner
- extraction would only move code without creating a clearer seam

### `local helper`

Extract a private helper that still belongs entirely to the current owner.

Choose `local helper` when:

- a repeated branch, calculation, mapping, or formatting rule appears inside one owner
- a named step would make the owner scan more cleanly
- a pure transformation or validation rule can be separated from surrounding orchestration
- the helper does not need its own file or public surface

Keep a `local helper` in the same file by default. Move it into a sibling file only when it is used by another file owned by the same boundary or the owner already contains multiple extracted local helpers.

### `local module`

Extract a sibling file, module, class, or equivalent unit that still belongs to one owner.

Choose `local module` when:

- the extracted code needs its own imports, dependencies, state, or configuration
- multiple related helpers naturally belong together
- pure transformation logic should be separated from side-effecting orchestration inside one owner
- the code should stay local to one owner but no longer belongs in the main file

Do not create a `local module` just to shorten a file.

When a `local module` creates a meaningful new seam with its own imports, dependencies, state, or entrypoint shape, check whether that module now needs its own local governance pass before materializing the rest of its contents.

### `boundary`

Extract a named boundary with a distinct responsibility. Ownership is a later decision handled by [Change Scoping Policy](./scoping.md).

Choose `boundary` when:

- the responsibility is distinct enough to deserve its own seam
- the code benefits from a narrow local API or entrypoint
- the new unit has a clearer responsibility than a helper or simple sibling module
- side effects or orchestration deserve a clearer seam than the current owner provides

After extracting a `boundary`, keep it at the nearest ownership level first. Promote it into broader shared scope only when [Change Scoping Policy](./scoping.md) shows that ownership is clearly broader.

When a `boundary` is introduced, re-run the local governance questions for that unit before filling in its internal structure.

## Objective Signals

Use these as concrete extraction signals:

- repeated logic inside one owner
- a distinct dependency set or configuration
- pure logic that can be separated from side-effecting flow
- a stable local API or seam
- multiple local helpers that form one responsibility
- actual reuse across independent ownership levels
- a responsibility that no longer fits cleanly inside one local module or local area

## Guardrails

- Do not extract only to reduce line count.
- Do not stop after naming a top-level boundary if the task also requires scaffolding or materializing it.
- Do not promote an extracted `boundary` into shared scope only because it looks reusable.
- Do not share side-effecting orchestration before the underlying responsibility is stable and ownership is clearly broader.
- Do not create a boundary without a distinct responsibility.
- If broader placement seems right but the correct destination is unclear, ask the user instead of inventing a new shared home.
