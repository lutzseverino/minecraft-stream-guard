# React Component Governance

## Purpose

Group the component governance references by concern so extraction, classification, packaging, and ownership rules remain clear and independently maintainable.

Use [Workflow](./workflow.md) as the operational entry point when making a decision.

## Documents

- [Workflow](./workflow.md)
- [Component Classification Policy](./classification.md)
- [Component Role Reference](./roles.md)
- [Component Pattern Reference](./patterns.md)
- [Component Packaging Policy](./packaging.md)
- [Component Ownership And Sharing Policy](./ownership.md)
- [Component Extraction Policy](./extraction.md)
- [Component Boundary Cases](./boundary-cases.md)

## Scope Boundaries

- Workflow defines the order of operations for a decision.
- Classification policy defines how primary roles and optional patterns work together.
- Role reference defines the allowed primary responsibilities.
- Pattern reference defines optional API and composition patterns.
- Packaging defines how public and private UI boundaries appear in the filesystem.
- Ownership defines when code stays local and when it may be promoted into broader shared scope.
- Extraction defines when a meaningful UI boundary should exist at all and whether it should become a component or some other boundary kind.

## Decision Flow

1. Start with [Workflow](./workflow.md) to follow the decision sequence.
2. Use [Component Extraction Policy](./extraction.md) to decide whether the UI should stay `inline`, become a `local helper`, become a `component boundary`, or become another boundary kind.
3. If the extraction creates a `component boundary`, use [Component Classification Policy](./classification.md) to choose one primary role and any optional patterns that strengthen that role.
4. If the extraction creates a `headless behavior boundary`, `provider/infrastructure boundary`, or `support boundary`, keep the boundary kind explicit instead of forcing a component role.
5. Use [Component Boundary Cases](./boundary-cases.md) before finalizing classification, packaging, or ownership when a provider, screen, form coordinator, or loading/error shell does not fit cleanly at first glance.
6. Read the matching role and pattern references when you need deeper guidance on the chosen classification.
7. Apply [Component Packaging Policy](./packaging.md) to decide how the chosen boundary appears in the filesystem.
8. Apply [Component Ownership And Sharing Policy](./ownership.md) to decide what stays local and what may move into broader shared scope.
