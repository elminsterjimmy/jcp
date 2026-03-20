# Brainstorm: Arrays STD API for Base Module

**Date:** 2026-03-18
**Status:** Ready for planning
**Issue:** #46

## What We're Building

A static `Arrays` utility class (`com.elminster.jcp.module.base.arrays.Arrays`) providing four
array operations callable from JCP DSL programs as `Arrays.length(a)`, etc. Follows the same
pattern as the existing `Strings` STD class.

## Why

Arrays are a first-class type in JCP (`INT_ARRAY`, `STRING_ARRAY`, `BOOLEAN_ARRAY`,
`DOUBLE_ARRAY`). Without a standard library, DSL authors have no way to query length, extract
slices, test membership, or sort — operations they'd expect for free. This closes that gap.

## Scope

### Deliverables

- `Arrays.java` with all 4 methods, typed overloads for each array element type
- Registered in `BaseModuleRegister.classToRegister()`
- `ArraysTest.java` — eval mode tests
- `ArraysCompileTest.java` — compile mode tests
- Coverage ≥ 80%

### Out of Scope

- `get` / `set` — direct element access is handled by the language itself
- Multi-dimensional arrays
- Stream / functional operations (filter, map, reduce)

## API

```
Arrays.length(a)            → int
Arrays.slice(a, from, to)   → same type as a  (from inclusive, to exclusive)
Arrays.contains(a, v)       → boolean
Arrays.sort(a)              → same type as a  (in-place, also returns sorted array)
```

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Dispatch strategy | Typed overloads per array type | Fits JCP's statically-typed resolution; avoids reflection overhead at runtime |
| `length` implementation | `java.lang.reflect.Array.getLength(Object)` single method | One implementation covers all array types cleanly |
| `boolean[]` sort order | `false` before `true` | Consistent with natural ordering (false=0, true=1) |
| `sort` mutation | In-place + returns array | Convenient for chaining; mirrors Java `Arrays.sort` semantics with added return |
| `slice` return type | Same type as input (per overload) | Preserves type safety; compiler `TypeMapper` can use overload signature directly |
| `DOUBLE_ARRAY` support | Yes, include in all methods | `DOUBLE_ARRAY` is already a first-class system type; consistency demands it |

## Method Signatures

```java
// length — single Object overload sufficient
public static int length(Object array)            // uses reflect.Array.getLength

// slice
public static int[]     slice(int[] a, int from, int to)
public static String[]  slice(String[] a, int from, int to)
public static boolean[] slice(boolean[] a, int from, int to)
public static double[]  slice(double[] a, int from, int to)

// contains
public static boolean contains(int[] a, int v)
public static boolean contains(String[] a, String v)
public static boolean contains(boolean[] a, boolean v)
public static boolean contains(double[] a, double v)

// sort
public static int[]     sort(int[] a)
public static String[]  sort(String[] a)
public static boolean[] sort(boolean[] a)     // false before true
public static double[]  sort(double[] a)
```

## Open Questions

1. Should `slice` with out-of-bounds indices throw (fail-fast) or clamp silently? — **Recommendation: throw**, consistent with Java `Arrays.copyOfRange` / Python slice semantics (Python clamps, Java throws). Keeping it explicit is safer for DSL users.
2. Does `contains` on `double[]` need floating-point epsilon comparison, or exact equality? — **Recommendation: exact equality** for now; epsilon comparison can be added later if needed.

## Next Steps

Run `/gw-plan` to create implementation plan.
