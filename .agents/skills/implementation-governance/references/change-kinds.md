# Change Kind Policy

## Purpose

Choose one `primary change kind` before making structural decisions. Add `secondary change kinds` only when they materially change scope, extraction pressure, or validation depth.

## Allowed Change Kinds

### `direct fix`

Use when the goal is to correct a bug, typo, edge case, broken condition, incorrect query, invalid mapping, or other wrong behavior inside an existing structure.

Defaults:

- keep the scope as narrow as possible
- preserve the current owner and existing structure unless that structure directly blocks the fix
- prefer `inline` changes over extraction
- validate the corrected path directly

### `local refactor`

Use when the goal is to improve clarity, naming, duplication, or internal structure inside an existing owner without changing its public contract or broader ownership.

Defaults:

- keep the scope inside the current owner
- prefer `local helper` or `local module` before broader extraction
- preserve behavior
- validate parity at the owner level

### `boundary extraction`

Use when the current owner no longer scans cleanly, a distinct responsibility is emerging, or a stable local seam deserves its own boundary.

Defaults:

- extract locally first
- keep the extracted `boundary` at the nearest ownership level first
- keep the new boundary narrow and named by responsibility
- validate behavior before and after extraction

### `cross-cutting refactor`

Use when the same structural or behavioral change must be applied across multiple owners, modules, packages, services, or layers.

Defaults:

- widen scope only as far as the repeated change actually reaches
- do not standardize adjacent areas that are not required by the task
- escalate when the refactor sets a wider precedent or changes a public contract
- use broader validation than a local refactor

### `new feature slice`

Use when the change introduces new behavior, not just a correction or internal cleanup.

Defaults:

- implement the narrowest complete slice that satisfies the request
- keep each concern with its nearest clear owner
- extract only when the feature introduces a real local boundary or broader reuse
- validate the new path directly and any touched contracts proportionately

## Tie-Breakers

- If behavior is wrong and structure only changes to support the correction, choose `direct fix`.
- If behavior should remain the same and the work stays inside one owner, choose `local refactor`.
- If a distinct responsibility deserves its own local seam, choose `boundary extraction`.
- If the same change must be made across multiple owners, choose `cross-cutting refactor`.
- If the request introduces new behavior or a new vertical path, choose `new feature slice`.

## Mixed Work

Most tasks should still have one `primary change kind`.

Add a `secondary change kind` only when it changes the implementation plan in a real way:

- scope becomes broader than the primary kind would normally allow
- extraction pressure changes meaningfully
- validation needs become broader or different

Examples:

- A bug fix that requires a local seam may be `primary: direct fix`, `secondary: boundary extraction`.
- A feature slice that requires a broad compatibility update may be `primary: new feature slice`, `secondary: cross-cutting refactor`.
- A local refactor that also changes behavior is not mixed work. Reclassify it so the behavior change is primary.

## Do Not Collapse Change Kinds

- Do not call behavior changes a refactor.
- Do not turn a `direct fix` into a `cross-cutting refactor` unless the repeated change is required.
- Do not justify broader promotion solely because extracted code could become reusable later.
- Do not add secondary kinds unless they materially change the plan.
