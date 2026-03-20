---
title: "feat: Arrays.sort for struct arrays with SortKey builder"
type: feat
date: 2026-03-20
issue: 56
---

# feat: Arrays.sort for Struct Arrays with SortKey Builder

## Overview

Add a `SortKey` builder type and new `Arrays.sort(Object[], SortKey...)` / `Arrays.reverse(...)`
overloads to the JCP base module. This lets users sort struct arrays by one or more fields with
per-field direction (ASC/DESC), dot-notation nested field paths, and null-last semantics — all
without requiring lambda support.

## Technical Approach

### SortKey — pure Java builder, not a JCP DataType

`SortKey` is a plain Java class registered alongside `Arrays` in `BaseModuleRegister`. It does
**not** need to be a `SystemDataType` entry. The JCP runtime resolves it as `ANY` when passed as
an argument, which is sufficient for dispatch. No type-system changes are required.

```
module/base/arrays/
  Arrays.java          ← add sort(Object[], SortKey...) + reverse overloads
  SortKey.java         ← new: builder class
```

### Field path resolution

For a path `"address.city"`:
1. Split on `"."` → `["address", "city"]`
2. Starting from the `StructData` element, call `getField(segment)` for each segment
3. If any intermediate result is `null` or not a `StructData`, return `null` (→ sorts last)
4. The final `Data` value's `.get()` is compared via its natural `Comparable` order

Supported leaf types: `Integer` (INT), `String`, `Boolean`, `Double` — all box types implement
`Comparable`, so a single unchecked cast suffices. Unsupported leaf types sort last.

### Comparator construction

`Arrays.sort(Object[], SortKey...)` builds a `Comparator<Object>` by composing per-field
comparators in order:

```java
Comparator<Object> comparator = (a, b) -> 0;
for (SortKey key : keys) {
    comparator = comparator.thenComparing(key.toComparator());
}
java.util.Arrays.sort(array, comparator);
```

Each `SortKey.toComparator()` resolves the field path, applies null-last logic, then reverses if
`DESC`.

### Arrays.reverse

Five typed overloads (int[], String[], boolean[], double[], Object[]) each reverse in-place and
return the array — consistent with existing `sort` overloads.

### BaseModuleRegister

Add `SortKey.class` to the registration list so the runtime can resolve it as a parameter type.

## Implementation Phases

### Phase 1: SortKey builder

- [ ] Create `SortKey.java` in `module/base/arrays/`
  - `public static SortKey by(String fieldPath)` — factory method, ASC default
  - `.asc()` / `.desc()` — fluent direction setters, return `this`
  - `toComparator()` — package-private, used by `Arrays`
  - Field path resolution logic (split, walk `StructData`, null-last)
- [ ] Unit-test `SortKey` directly: path resolution, direction, null-last, nested paths

### Phase 2: Arrays.sort with SortKey

- [ ] Add `Arrays.sort(Object[] a, SortKey... keys)` to `Arrays.java`
  - Compose per-field comparators, call `java.util.Arrays.sort(a, comparator)`
  - Return `a`
- [ ] Register `SortKey.class` in `BaseModuleRegister.classToRegister()`
- [ ] Eval-mode integration tests (see Acceptance Criteria)

### Phase 3: Arrays.reverse

- [ ] Add `reverse(int[])`, `reverse(String[])`, `reverse(boolean[])`, `reverse(double[])`,
  `reverse(Object[])` to `Arrays.java` — each reverses in-place, returns the array
- [ ] Tests for all five overloads (empty array, single element, even/odd length)

### Phase 4: Coverage verification

- [ ] Run `mvn verify -pl core` — confirm JaCoCo ≥ 80% instruction and branch
- [ ] Fix any coverage gaps

## Acceptance Criteria

### Functional

- [ ] `Arrays.sort(persons, SortKey.by("age"))` sorts struct array by int field ascending
- [ ] `Arrays.sort(persons, SortKey.by("name").desc())` sorts descending
- [ ] `Arrays.sort(persons, SortKey.by("lastName"), SortKey.by("firstName"))` sorts by two fields
- [ ] `Arrays.sort(persons, SortKey.by("address.city"))` resolves nested field path
- [ ] Elements with a missing/null field sort to the end (null-last, both ASC and DESC)
- [ ] `Arrays.reverse(intArray)` reverses in-place and returns the array
- [ ] `Arrays.reverse(objectArray)` works for struct arrays
- [ ] Existing `Arrays.sort(int[])` / `sort(String[])` etc. are unaffected

### Non-Functional

- [ ] No new `SystemDataType` entries required
- [ ] `SortKey` is not exposed as a JCP DSL keyword — it is a library type only
- [ ] JaCoCo thresholds pass (≥ 80% instruction, ≥ 80% branch)
- [ ] No compile-mode changes (out of scope)

## Dependencies

- No blocked dependencies — this is self-contained within `core/`
- `StructData.getField(String)` already exists and is used as-is
- `BaseModuleRegister` pattern is established; adding `SortKey.class` follows existing convention

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `SortKey` parameter type not resolved by `FunCallEvaluator` | M | H | Register `SortKey.class` in `BaseModuleRegister`; add integration test that calls via `FunctionCallExpression` |
| Varargs `SortKey...` dispatch conflicts with existing `sort(Object[])` | M | M | Java method resolution prefers the more specific overload; verify with a test passing `SortKey` args |
| Nested path on non-struct mid-node silently wrong | L | M | Covered by null-last rule; add explicit test for path through non-struct |
| Coverage drops below 80% threshold | L | H | Write tests for all branches (empty keys, single key, multi-key, null field, unsupported leaf type) |
