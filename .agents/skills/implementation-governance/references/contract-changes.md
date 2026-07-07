# Contract Change Policy

## When This Applies

Use this policy after a contract-affecting change is confirmed.

Treat public exports, interfaces, schemas, route shapes, event payloads, database contracts, and external integration boundaries as contract surfaces.

## Choose The Narrowest Safe Contract Change

- Prefer additive changes before breaking changes.
- Prefer compatibility shims before flag-day rewrites when existing consumers must continue to work.
- Prefer internal adaptation layers before widening a shared contract.
- Make only the contract change required by the request. Do not batch unrelated cleanup into the same step.

## Safe Change Patterns

### Additive change

Use when the new behavior can coexist with the current contract.

- keep the old contract working
- add the new field, method, route shape, event shape, or behavior alongside it
- update direct consumers intentionally rather than opportunistically

### Breaking change

Use only when compatibility is not required or when the user has approved the migration cost.

- change the contract deliberately
- update all required consumers in the same governed scope, or stage the migration explicitly
- do not leave partially migrated callers unless staged migration is the chosen plan

### Staged migration

Use when a direct breaking change is too broad or unsafe.

- introduce the new contract
- add the compatibility path needed for the transition
- migrate consumers intentionally
- remove the old path only when the governed scope includes that cleanup

## Validation Expectations

- exercise the contract directly
- verify the affected consumer path or integration path directly
- state any remaining unmigrated consumers, deferred cleanup, or compatibility debt

## Common Errors

- treating an approved contract change like an ordinary local refactor
- widening multiple contracts when one narrow change would suffice
- updating incidental consumers opportunistically
- removing the old path before the migration plan covers it
