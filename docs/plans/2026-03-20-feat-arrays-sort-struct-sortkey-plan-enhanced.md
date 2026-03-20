---
title: "feat: Arrays.sort for struct arrays with SortKey builder (Enhanced)"
type: feat
date: 2026-03-20
issue: 56
review_date: 2026-03-20
original_plan: docs/plans/2026-03-20-feat-arrays-sort-struct-sortkey-plan.md
---

# feat: Arrays.sort for Struct Arrays with SortKey Builder (Enhanced)

> **Note:** Enhanced version incorporating feedback from plan review on 2026-03-20.
> Primary fix: replaced `SortKey...` varargs with fixed-arity overloads to match JCP's
> reflection-based dispatch mechanism.

## Overview

Add a `SortKey` builder type and new `Arrays.sort` / `Arrays.reverse` overloads to the JCP base
module. This lets users sort struct arrays by one or more fields with per-field direction
(ASC/DESC), dot-notation nested field paths, and null-last semantics — without requiring lambda
support.

## Technical Approach

### SortKey — pure Java builder, not a JCP DataType

`SortKey` is a plain Java class registered alongside `Arrays` in `BaseModuleRegister`. It does
**not** need to be a `SystemDataType` entry. `ClassConverter` will auto-register it as a DataType
named `"SortKey"` (via `DataTypeUtils.getDataTypeAndCreateOnMissing`) when it appears as a
parameter type. No manual type-system changes are required.

```
module/base/arrays/
  Arrays.java          ← add sort(Object[], SortKey) + sort(Object[], SortKey, SortKey) +
                          sort(Object[], SortKey, SortKey, SortKey) + reverse overloads
  SortKey.java         ← new: builder class
```

### Why fixed-arity overloads instead of varargs

JCP's `ClassConverter.getParameterDefs()` introspects methods via `method.getParameters()`.
Java varargs `SortKey...` is seen as `SortKey[]` — a single fixed-arity array parameter. The
runtime evaluator `FunCallEvaluator.hasSameParameterDefinition()` matches by
`parameterDefs.length == arguments.length`. A caller passing 2 `SortKey` values has
`argumentData.length == 2`, but the registered function has `parameterDefs.length == 1`.
This causes `UndeclaredException` at runtime.

**Solution:** Follow the existing `Arrays.java` pattern of explicit typed overloads:

```java
public static Object[] sort(Object[] a, SortKey k1)
public static Object[] sort(Object[] a, SortKey k1, SortKey k2)
public static Object[] sort(Object[] a, SortKey k1, SortKey k2, SortKey k3)
```

Three overloads cover all practical cases (primary, primary+secondary, three-level).
The internal implementation delegates to a shared private method that accepts `SortKey[]`.

### Field path resolution

For a path `"address.city"`:
1. Split on `"."` → `["address", "city"]`
2. Starting from the `StructData` element, call `getField(segment)` for each segment
3. If any intermediate result is `null` or not a `StructData`, return `null` (→ sorts last)
4. The final `Data` value's `.get()` is compared via its natural `Comparable` order

Supported leaf types: `Integer` (INT), `String`, `Boolean`, `Double` — all box types implement
`Comparable`. Unsupported leaf types treat the value as `null` and sort last.

### Comparator construction (internal)

The private shared method composes per-field comparators:

```java
private static Object[] sortByKeys(Object[] a, SortKey... keys) {
    Comparator<Object> comparator = (x, y) -> 0;
    for (SortKey key : keys) {
        comparator = comparator.thenComparing(key.toComparator());
    }
    java.util.Arrays.sort(a, comparator);
    return a;
}
```

Each `SortKey.toComparator()` resolves the field path, applies null-last logic, then reverses if
`DESC`.

### Arrays.reverse

Five typed overloads (int[], String[], boolean[], double[], Object[]) each reverse in-place and
return the array — consistent with existing `sort` overloads.

### BaseModuleRegister

Add `SortKey.class` to the registration list. `ClassConverter` will process it and register
`SortKey` as a DataType so it can appear as a parameter type in `Arrays.sort` overloads.

## Implementation Phases

### Phase 1: SortKey builder

**Deliverables:**
- [ ] Create `SortKey.java` in `module/base/arrays/`
  - `public static SortKey by(String fieldPath)` — factory method, ASC default
  - `.asc()` / `.desc()` — fluent direction setters, return `this`
  - `Comparator<Object> toComparator()` — package-private, used by `Arrays`
  - Private field path resolution: split on `"."`, walk `StructData.getField()`, null-last
- [ ] Unit-test `SortKey.toComparator()` directly (no JCP runtime needed):
  - Single-level field, int leaf, ASC and DESC
  - Single-level field, String leaf
  - Nested path `"address.city"`
  - Missing field → sorts last (both ASC and DESC)
  - Non-StructData element → sorts last
  - Empty field path edge case

### Phase 2: Arrays.sort with SortKey (fixed-arity overloads)

**Deliverables:**
- [ ] Add to `Arrays.java`:
  - `public static Object[] sort(Object[] a, SortKey k1)`
  - `public static Object[] sort(Object[] a, SortKey k1, SortKey k2)`
  - `public static Object[] sort(Object[] a, SortKey k1, SortKey k2, SortKey k3)`
  - Private `sortByKeys(Object[] a, SortKey... keys)` — shared implementation
- [ ] Register `SortKey.class` in `BaseModuleRegister.classToRegister()`
- [ ] Integration tests via `FunctionCallExpression` dispatch (not direct Java calls):
  - 1-key sort (validates `sort(Object[], SortKey)` overload resolves)
  - 2-key sort (validates `sort(Object[], SortKey, SortKey)` overload resolves)
  - Sort with nested path via dispatch
  - Null-field element via dispatch

### Phase 3: Arrays.reverse

**Deliverables:**
- [ ] Add `reverse(int[])`, `reverse(String[])`, `reverse(boolean[])`, `reverse(double[])`,
  `reverse(Object[])` to `Arrays.java` — each reverses in-place, returns the array
- [ ] Tests for all five overloads:
  - Empty array
  - Single-element array
  - Even-length array
  - Odd-length array

### Phase 4: Coverage verification

- [ ] Run `mvn verify -pl core` — confirm JaCoCo ≥ 80% instruction and branch
- [ ] Fix any coverage gaps (target: SortKey path resolution branches, all `sort` overloads)

## Acceptance Criteria

### Functional

- [ ] `Arrays.sort(persons, SortKey.by("age"))` sorts struct array by int field ascending
- [ ] `Arrays.sort(persons, SortKey.by("name").desc())` sorts descending
- [ ] `Arrays.sort(persons, SortKey.by("lastName"), SortKey.by("firstName"))` sorts by two fields
- [ ] `Arrays.sort(persons, SortKey.by("lastName"), SortKey.by("firstName"), SortKey.by("age"))` sorts by three fields
- [ ] `Arrays.sort(persons, SortKey.by("address.city"))` resolves nested field path
- [ ] Elements with a missing/null field sort to the end (null-last, both ASC and DESC)
- [ ] `Arrays.reverse(intArray)` reverses in-place and returns the array
- [ ] `Arrays.reverse(objectArray)` works for struct arrays
- [ ] Existing `Arrays.sort(int[])` / `sort(String[])` etc. are unaffected
- [ ] All three `sort(Object[], SortKey...)` overloads are callable via `FunctionCallExpression` dispatch without `UndeclaredException`

### Non-Functional

- [ ] No new `SystemDataType` entries required
- [ ] `SortKey` is not exposed as a JCP DSL keyword — it is a library type only
- [ ] JaCoCo thresholds pass (≥ 80% instruction, ≥ 80% branch)
- [ ] No compile-mode changes (out of scope)

## Dependencies

- No blocked dependencies — self-contained within `core/`
- `StructData.getField(String)` already exists and is used as-is
- `ClassConverter.registerClass()` auto-registers `SortKey` as a DataType when encountered as a parameter type — no manual registration beyond adding to `BaseModuleRegister`

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `SortKey` DataType not auto-registered before `Arrays` methods are scanned | M | H | Register `SortKey.class` **before** `Arrays.class` in `BaseModuleRegister.classToRegister()` list |
| Fixed-arity overloads cause `FunctionAmbiguityException` when 2-key and 3-key overloads both match | L | H | Java dispatch passes exact argument count to `getParameterDefs`; `hasSameParameterDefinition` checks exact length — no ambiguity possible across different arities |
| `ClassConverter` unwraps `Data.get()` before passing to Java method; `SortKey` is the raw value | L | M | `AbstractModuleFunction.doFunction` calls `getArgumentValues` which calls `.get()` on each `Data`; since `SortKey` is stored as `AnyData<SortKey>`, `.get()` returns the `SortKey` object correctly |
| Nested path on non-struct mid-node silently wrong | L | M | Covered by null-last rule; explicit test: path through a non-struct field |
| Coverage drops below 80% threshold | L | H | Tests cover: all sort overloads, all reverse overloads, all SortKey direction/path branches, null-field case, unsupported leaf type |

## Testing Strategy

**Unit tests** (no JCP runtime — test Java logic directly):
- `SortKeyTest`: path resolution, direction, null-last, nested path, unsupported leaf
- `ArraysReverseTest`: all five typed overloads, edge cases

**Integration tests** (via `FunctionCallExpression` — validates full dispatch chain):
- `ArraysSortStructTest`: all three sort overloads, nested path, null-field, multi-key ordering
- Pattern: follow `ArraysTest.evalOnArray()` helper — seed array in context, call via `FunctionCallExpression`, assert result

## Changes from Original Plan

1. **Replaced `SortKey...` varargs with fixed-arity overloads** — `sort(Object[], SortKey)`, `sort(Object[], SortKey, SortKey)`, `sort(Object[], SortKey, SortKey, SortKey)`. Root cause: JCP's reflection-based dispatch (`ClassConverter` + `FunCallEvaluator`) does not handle varargs; callers with N `SortKey` args would cause `UndeclaredException`.
2. **Corrected risk mitigation for varargs** — Original mitigation cited Java compile-time overload resolution, which is irrelevant to JCP runtime dispatch. Updated to document the fixed-arity approach as the actual solution.
3. **Added dispatch integration tests as an explicit acceptance criterion** — Direct Java unit tests of `SortKey.toComparator()` do not catch runtime dispatch failures. Integration tests via `FunctionCallExpression` are required.
4. **Added SortKey registration ordering note** — `SortKey.class` must precede `Arrays.class` in the `BaseModuleRegister` list to ensure the `SortKey` DataType exists before `Arrays` methods are scanned.
5. **Expanded testing strategy** — Separated unit tests (pure Java) from integration tests (JCP dispatch), with explicit patterns for each.

## References

- Original plan: `docs/plans/2026-03-20-feat-arrays-sort-struct-sortkey-plan.md`
- Plan review: `docs/plans/2026-03-20-feat-arrays-sort-struct-sortkey-plan-review.md`
- Issue: https://github.com/elminsterjimmy/jcp/issues/56
