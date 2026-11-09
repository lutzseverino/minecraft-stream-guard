# Workflow

Use this order when making React component architecture decisions:

1. Start with extraction.
Decide whether the UI should stay `inline`, become a `local helper`, become a `component boundary`, or become another boundary kind.

2. If the extracted boundary is a `component boundary`, choose one primary role.
Classify the component as `presentational`, `interactive`, or `container`.

3. If the extracted boundary is not a `component boundary`, classify the boundary kind instead.
Use one of these:

- `headless behavior boundary`
- `provider/infrastructure boundary`
- `support boundary`

4. Check boundary cases before finalizing downstream decisions.
Use special guidance for providers, pages, form coordinators, and loading or error shells before finalizing classification, packaging, or ownership.

5. Add optional patterns only when they strengthen the chosen role.
Patterns such as `compound`, `headless hook plus UI shell`, and `slot-based or polymorphic` are modifiers, not replacements for responsibility.

6. Decide packaging.
Make the public API boundary and local file ownership obvious.

7. Decide ownership.
Keep code in the narrowest scope that fully owns its reason to change.
If a broader owner is appropriate but the correct broader owner is not clear from the repo structure, ask the user before promoting the boundary.

## Decision Outcome Rule

Do not force a primary component role onto boundaries whose main value is:

- reusable behavior with no required UI shell
- infrastructure or context wiring
- local support for another owner such as loading or error handling
