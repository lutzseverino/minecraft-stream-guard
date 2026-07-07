# Escalation Policy

## Default Rule

Escalate when the next correct step is wider, riskier, or less reversible than the request makes explicit.

## Escalate Before Proceeding

Ask the user before proceeding when any of these are true:

- the change would alter a public API, schema, or external contract
- the change would require an explicitly breaking contract change or a staged migration
- broader ownership seems correct but the right destination is unclear
- the touched area is mixed and choosing one pattern would standardize a wider area
- the change would introduce a new dependency or cross-layer dependency with broader architectural consequences
- the change would trigger a destructive migration, irreversible data change, or broad rename
- the requested scope appears too small for a safe fix and the wider scope has product or architectural consequences
- high-risk behavior must change but credible validation is unavailable

## Do Not Escalate For Normal Local Decisions

Do not ask for confirmation when:

- the change stays with the current owner
- the extraction is clearly local
- the convention in the touched area is clear
- the validation depth is proportionate and available
- the change does not widen ownership or public surface

## How To Escalate

When escalating:

- name the decision that is unclear
- name the concrete consequence of guessing
- propose the narrowest safe option if one exists
- avoid open-ended design brainstorming unless the user asked for it

## Common Escalation Errors

- asking for permission on ordinary local refactors
- guessing broader ownership because a shared home feels cleaner
- hiding a public contract change inside a local implementation request
- proceeding with a risky change after noting that validation is missing
