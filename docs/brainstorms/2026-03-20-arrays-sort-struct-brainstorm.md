# Brainstorm: Arrays.sort for Struct Arrays with SortKey

**Date:** 2026-03-20
**Status:** Ready for planning
**Related:** Issue #55

## What We're Building

A `SortKey` builder type and new `Arrays.sort` / `Arrays.reverse` overloads that let JCP users sort struct arrays by one or more fields, with per-field direction control and nested field path support.

## Why

`Arrays.sort(Object[])` requires elements to implement `Comparable`. `StructData` does not, so users cannot sort struct arrays at all today. A field-name-based API solves this without requiring lambda support.

## API Design

### SortKey builder

```java
SortKey.by("name")               // ascending by default
SortKey.by("name").asc()
SortKey.by("name").desc()
SortKey.by("address.city").desc() // nested field path via dot notation
```

### Arrays.sort overloads

```java
// Single field
Arrays.sort(persons, SortKey.by("age"))

// Multiple fields (primary, secondary, ...)
Arrays.sort(persons, SortKey.by("lastName"), SortKey.by("firstName"))

// Mixed directions
Arrays.sort(persons, SortKey.by("age").desc(), SortKey.by("name").asc())
```

### Arrays.reverse

```java
Arrays.reverse(a)  // reverses any array in-place, returns the array
```

## Scope

### Deliverables
- `SortKey` class in `module/base/arrays/` with `by(String fieldPath)`, `.asc()`, `.desc()`
- `Arrays.sort(Object[], SortKey...)` overload — struct arrays only
- `Arrays.reverse(int[])`, `Arrays.reverse(String[])`, `Arrays.reverse(boolean[])`, `Arrays.reverse(double[])`, `Arrays.reverse(Object[])` overloads
- `BaseModuleRegister` updated to expose new overloads
- Tests for eval mode covering: single field, multi-field, nested path, ASC/DESC, null-field handling, reverse

### Out of Scope
- Sorting non-struct `Object[]` with `SortKey` (e.g. boxed `Integer[]`)
- Lambda/function-reference comparator (tracked separately, blocked on lambda support)
- Compile mode support for `SortKey` (can be added once eval is stable)

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Field path separator | Dot notation (`"address.city"`) | Familiar, mirrors JCP field access syntax |
| Applies to | Struct arrays only | Keeps scope focused; primitive arrays have dedicated overloads |
| Missing/null field | Nulls sort last (both ASC and DESC) | Predictable, non-crashing behavior |
| Direction default | ASC | Industry standard default |
| Reverse | Dedicated `Arrays.reverse()` | Orthogonal to sorting; useful independently |
| SortKey location | `module/base/arrays/SortKey.java` | Co-located with `Arrays.java` |

## Field Path Resolution

For a path like `"address.city"`:
1. Split on `.`
2. Walk `StructData.getField()` for each segment
3. If any segment returns null or a non-`StructData` mid-path, treat as null (sort last)
4. Final value compared via natural ordering of the primitive `Data` value

Supported leaf types for comparison: `int`, `String`, `boolean`, `double` (existing `SystemDataType` primitives).

## Open Questions

1. Should `SortKey` be exposed as a first-class JCP `DataType` (so it appears in the type system), or remain a pure Java helper invoked via static method call?
2. Should `Arrays.reverse` return the array (consistent with existing `sort` overloads) or `void`?

## Next Steps

Run `/gw-plan` to create implementation plan.
