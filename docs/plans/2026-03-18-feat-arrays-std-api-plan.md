---
title: "feat: Arrays STD API for base module"
type: feat
date: 2026-03-18
issue: 46
brainstorm: docs/brainstorms/2026-03-18-arrays-std-api-brainstorm.md
---

# feat: Arrays STD API for base module

## Overview

Add `com.elminster.jcp.module.base.arrays.Arrays` — a static utility class providing 4
DSL-friendly array operations callable from JCP programs in both eval and compile mode.
Register it in `BaseModuleRegister`. Write tests for every overload in both modes.

The 4 methods cover all first-class array types (`int[]`, `String[]`, `boolean[]`, `double[]`):

```
Arrays.length(a)            → int
Arrays.slice(a, from, to)   → same type as a   (from inclusive, to exclusive)
Arrays.contains(a, v)       → boolean
Arrays.sort(a)              → same type as a   (in-place, also returns sorted array)
```

## Technical Approach

**Pattern:** Identical to `Strings.java`. Static class, private constructor, all-static methods,
registered in `BaseModuleRegister`. No changes to eval or compile infrastructure.

**`length` dispatch:** Single `public static int length(Object array)` using
`java.lang.reflect.Array.getLength(array)` — covers all array types without overloads.

**`slice` / `contains` / `sort` dispatch:** Four typed overloads each — one per element type
(`int[]`, `String[]`, `boolean[]`, `double[]`). JCP's reflection-based method resolution picks
the correct overload from the actual argument type.

**`slice` implementation:** `java.util.Arrays.copyOfRange(a, from, to)` — out-of-bounds indices
throw `ArrayIndexOutOfBoundsException` (fail-fast).

**`sort` implementation:**
- `int[]`, `String[]`, `double[]` — delegate to `java.util.Arrays.sort(a)`, then return `a`
- `boolean[]` — custom in-place: count falses, fill falses first, trues second

**`contains` implementation:** Linear scan. `double[]` uses exact `==` equality.

**Type system:** `CompileModeClassConverter.mapJavaTypeToDataType` already maps all four array
types. `TypeMapper.toDescriptor` already emits correct JVM array descriptors. No changes needed.

**Eval test approach:** Pre-seed the `EvalContext` by calling `context.addVariable(data)` with
a pre-built `ArrayData`, then pass a `VariableExpression` as the argument to
`FunctionCallExpression`. This mirrors the approach used by `DataFactory` tests.

## Implementation Phases

### Phase 1: Implement `Arrays.java`

Location: `core/src/main/java/com/elminster/jcp/module/base/arrays/Arrays.java`

- [ ] Create `arrays/` package under `module/base/`
- [ ] Private constructor
- [ ] `length(Object array) → int` via `java.lang.reflect.Array.getLength`
- [ ] `slice(int[], int, int) → int[]` via `java.util.Arrays.copyOfRange`
- [ ] `slice(String[], int, int) → String[]` via `java.util.Arrays.copyOfRange`
- [ ] `slice(boolean[], int, int) → boolean[]` via `java.util.Arrays.copyOfRange`
- [ ] `slice(double[], int, int) → double[]` via `java.util.Arrays.copyOfRange`
- [ ] `contains(int[], int) → boolean`
- [ ] `contains(String[], String) → boolean`
- [ ] `contains(boolean[], boolean) → boolean`
- [ ] `contains(double[], double) → boolean`
- [ ] `sort(int[]) → int[]` — `java.util.Arrays.sort(a); return a`
- [ ] `sort(String[]) → String[]` — `java.util.Arrays.sort(a); return a`
- [ ] `sort(boolean[]) → boolean[]` — custom: count falses, fill falses then trues, return `a`
- [ ] `sort(double[]) → double[]` — `java.util.Arrays.sort(a); return a`

### Phase 2: Register in `BaseModuleRegister`

- [ ] Add `import com.elminster.jcp.module.base.arrays.Arrays;`
- [ ] Add `classes.add(Arrays.class);` to `classToRegister()`

### Phase 3: Eval mode tests (`ArraysTest.java`)

Location: `core/src/test/java/com/elminster/jcp/eval/function/ArraysTest.java`

Pattern: pre-seed `RootEvalContext` with `ArrayData` via `context.addVariable(data)`, then
call `Arrays.method` via `FunctionCallExpression(Identifier.fromName("Arrays.method"), varExpr)`,
capture result via `VariableDeclarationImpl`.

- [ ] `length(int[]{1,2,3})` → 3; `length(int[]{})` → 0
- [ ] `slice(int[]{1,2,3,4}, 1, 3)` → `[2,3]`
- [ ] `slice(String[]{"a","b","c"}, 0, 2)` → `["a","b"]`
- [ ] `contains(int[]{1,2,3}, 2)` → true; `contains(int[]{1,2,3}, 9)` → false
- [ ] `contains(String[]{"a","b"}, "b")` → true; `contains(String[]{"a","b"}, "x")` → false
- [ ] `contains(boolean[]{true,false}, false)` → true
- [ ] `sort(int[]{3,1,2})` → `[1,2,3]`
- [ ] `sort(String[]{"c","a","b"})` → `["a","b","c"]`
- [ ] `sort(boolean[]{true,false,true})` → `[false,true,true]`
- [ ] `sort(double[]{3.0,1.0,2.0})` → `[1.0,2.0,3.0]`

### Phase 4: Compile mode tests (`ArraysCompileTest.java`)

Location: `core/src/test/java/com/elminster/jcp/compile/module/ArraysCompileTest.java`

Pattern: use `StaticMethodCallExpression("Arrays", "method", ...)`. For array inputs, chain
`Arrays.sort(Arrays.slice(...))` etc., or use `Strings.split` to produce a `String[]` input.
Verify with `Assertions.assertEquals` / `Assertions.assertTrue` / `Assertions.assertFalse`.

- [ ] `length` on result of `Strings.split("a,b,c", ",")` → `assertEquals(3, ...)`
- [ ] `contains(Strings.split("a,b,c",","), "b")` → `assertTrue(...)`
- [ ] `contains(Strings.split("a,b,c",","), "x")` → `assertFalse(...)`
- [ ] `sort(Strings.split("c,a,b",","))` — compiles and invokes without throw
- [ ] `slice(Strings.split("a,b,c",","), 0, 2)` — compiles and invokes without throw

### Phase 5: Verify coverage

- [ ] `mvn test -pl core` — all tests pass
- [ ] `mvn verify -pl core` — JaCoCo ≥ 80% instruction and branch coverage

## Acceptance Criteria

### Functional Requirements

- [ ] All 4 methods callable as `Arrays.xxx(...)` in eval mode
- [ ] All 4 methods callable as `Arrays.xxx(...)` in compile mode
- [ ] `length` works on all array element types via single `Object` overload
- [ ] `slice` uses from-inclusive, to-exclusive semantics; out-of-bounds throws
- [ ] `sort(boolean[])` orders false before true
- [ ] `sort` is in-place and also returns the sorted array
- [ ] `contains(double[])` uses exact equality

### Non-Functional Requirements

- [ ] No changes to eval or compile infrastructure
- [ ] No changes to `TypeMapper` or `CompileModeClassConverter`
- [ ] `mvn test -pl core` passes cleanly
- [ ] JaCoCo instruction coverage ≥ 80%, branch coverage ≥ 80%
- [ ] Every overload covered by at least one test in each mode

## Dependencies

- `java.util.Arrays` — `sort`, `copyOfRange`
- `java.lang.reflect.Array` — `getLength`
- `BaseModuleRegister` — one-line addition
- `ArrayData` + `context.addVariable()` — for eval test setup
- `StaticMethodCallExpression` — used in compile tests (already exists)
- `AbstractCompileTest` — base class for compile tests (already exists)
- `INT_ARRAY`, `DOUBLE_ARRAY`, `STRING_ARRAY`, `BOOLEAN_ARRAY` in `SystemDataType` — already exist
- Array type mappings in `CompileModeClassConverter` — already exist
- Array descriptors in `TypeMapper` — already exist

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Eval tests: `ArrayData` wraps `Integer[]` not `int[]`; `Arrays.contains(int[],int)` won't match | M | M | Check `ArrayData` actual stored type; may need to store as `int[]` or adjust overload signature to `Integer[]` |
| Compile tests have no direct way to construct `int[]` / `boolean[]` inline | M | M | Use `Strings.split` to produce `String[]`; for `int[]` / `boolean[]`, test only the `String[]` overloads in compile mode |
| `boolean[]` sort custom implementation has off-by-one | L | L | Test with `[true,false,true]` → `[false,true,true]` and all-true / all-false inputs |
| Coverage drops below 80% on `boolean` sort branches | M | M | Add all-true, all-false, and mixed test cases |
| `double` exact equality surprises in `contains` | L | L | Document in Javadoc; epsilon variant out of scope |
