# Component Boundary Cases

## Purpose

Clarify how to reason about frontend structures that do not fit neatly into one example component role or pattern and would otherwise invite inconsistent local conventions.

## Context

This document does not create new roles or patterns. It explains how the existing classification, extraction, packaging, and ownership policies apply to common borderline cases.

## Providers

Providers are usually infrastructure boundaries rather than ordinary reusable UI components.

Use this guidance:

- treat providers as architectural or feature boundaries first
- colocate them with the narrowest owner that needs the provided context
- avoid promoting providers into shared space unless multiple owners truly depend on the same stable context contract

When a provider has visible UI shape, treat that shell as incidental unless its main value is truly reusable UI rather than provider or infrastructure wiring.

## Screens And Pages

Screens and pages usually act as container boundaries.

Use this guidance:

- treat a screen or page as a container when it assembles a feature or route workflow
- keep reusable UI concerns under narrower components rather than inside the screen itself
- do not turn route-level boundaries into generic reusable components unless they truly expose a stable shared contract

## Form Coordinators

Form coordinators usually behave as container boundaries, sometimes consuming optional patterns.

Use this guidance:

- treat a form coordinator as a container when it assembles validation, submission flow, and composed field sections
- keep reusable field UI in narrower presentational or interactive components
- extract headless behavior when several form surfaces need the same interaction contract

## Loading And Error Shells

Loading and error shells are usually support boundaries, not distinct primary roles.

Use this guidance:

- keep them local when they exist only for one owner
- extract them when they become a named reusable UI concept with a stable contract
- avoid giving loading or error shells feature orchestration responsibilities that belong to a container

## Decision Heuristic

When a case feels ambiguous:

1. Identify the main responsibility.
2. Decide whether the boundary is architectural, feature-level, or reusable UI.
3. Choose the nearest existing primary role if the boundary is a component.
4. Add optional patterns only when they strengthen that chosen role.
5. Keep the boundary local unless reuse or ownership clearly justifies promotion.

## Related

- [Component Classification Policy](./classification.md)
- [Component Extraction Policy](./extraction.md)
- [Component Packaging Policy](./packaging.md)
- [Component Ownership And Sharing Policy](./ownership.md)
