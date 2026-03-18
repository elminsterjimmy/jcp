---
title: "feat: Arrays STD API for base module (Enhanced)"
type: feat
date: 2026-03-18
issue: 46
review_date: 2026-03-18
original_plan: docs/plans/2026-03-18-feat-arrays-std-api-plan.md
---

# feat: Arrays STD API for base module (Enhanced)

> **Note:** Enhanced version incorporating feedback from plan review on 2026-03-18.
> Key change: use boxed-type overloads (`Integer[]`, `Boolean[]`, `Double[]`) instead of
> primitive array types to ensure correct `ClassConverter` type resolution.

## Overview

Add `com.elminster.jcp.module.base.arrays.Arrays` — a static utility class providing 4
DSL-friendly array operations callable from JCP programs in both eval and compile mode.
Register it in `BaseModuleRegister`. Write tests for every overload in both modes.

The 4 methods cover all first-class array types:

```
Arrays.length(a)            → int
Arrays.slice(a, from, to)   → same type as a   (from inclusive, to exclusive)
Arrays.contains(a, v)       → boolean
Arrays.sort(a)              → same type as a   (in-place, also returns sorted array)
```

## Technical Approach

**Pattern:** Identical to `Strings.java`. Static class, private constructor, all-static methods,
registered in `BaseModuleRegister`. No changes to eval or compile infrastructure.

**Critical design decision — boxed overloads:**
`ClassConverter.getDataType(Class<?>, ...)` calls `DataTypeUtils.getDataType(simpleName, ctx)`.
For primitive array types:
- `int[].class.getSimpleName()` = `"int[]"` → strips `[]` → looks up `"int"` → **not found**
  (context has `"Integer"`, not `"int"`)
- `Integer[].class.getSimpleName()` = `"Integer[]"` → strips `[]` → looks up `"Integer"` → **found** ✓

**Therefore: all typed overloads use boxed arrays** (`Integer[]`, `Boolean[]`, `Double[]`).
This matches `ArrayData`'s actual storage (which already uses `Integer[]` etc., as confirmed
by `ArrayDataTest`). `java.util.Arrays.sort` / `Arrays.copyOfRange` have boxed overloads via
`Arrays.sort(T[])` (generic). No runtime penalty.

**`length` dispatch:** Single `public static int length(Object array)` using
`java.lang.reflect.Array.getLength(array)`.
- `ClassConverter` registers this as `(ANY) → INT`
- Eval dispatch accepts any array via `isCompatibleWith(ANY)` — correct behavior

**`slice` / `contains` / `sort` dispatch:** Four boxed overloads each.
JCP's reflection-based method resolution picks the correct overload from the argument's data type.

**`slice` implementation:** `java.util.Arrays.copyOfRange(a, from, to)` — out-of-bounds throws
`ArrayIndexOutOfBoundsException` (fail-fast).

**`sort` implementation:**
- `Integer[]`, `String[]`, `Double[]` — `java.util.Arrays.sort(a)`, return `a`
- `Boolean[]` — custom in-place: count falses, fill falses first, trues second, return `a`

**`contains` implementation:** Linear scan using `.equals()` for object types. `Double[]` uses
exact `.equals()` (no epsilon).

**Type system:** `CompileModeClassConverter.mapJavaTypeToDataType` maps `int[].class → INT_ARRAY`
etc. In compile mode, `FunCallCompiler` resolves methods by matching JVM signatures against the
registered Java methods. Since the Java source uses `Integer[]`, the JVM descriptor is
`[Ljava/lang/Integer;` — but `CompileModeClassConverter` maps `Integer[].class` to... let's check:
the converter checks `int[].class`, `double[].class`, `boolean[].class` by primitive type. For
`Integer[].class` (boxed), it would fall through to `Object.class` handling → `ANY`.

**Compile-mode implication:** In compile mode, `FunCallCompiler` looks up the Java method via
reflection to emit the correct `invokestatic` call. The JVM will accept the call as long as the
bytecode passes `Integer[]` (which is what the array variables hold). The type resolution for
return types may resolve to `ANY_ARRAY` or `ANY` — acceptable since compile mode can still
emit the correct descriptor via `Object` / generic. Test by running, not by static analysis.

**Eval test approach:** Pre-seed `RootEvalContext` with `ArrayData` via `context.addVariable(data)`,
then pass a `VariableExpression` as the argument to `FunctionCallExpression`.

Exact pattern:
```java
ArrayData<Integer[]> arr = new ArrayData<>(SystemDataType.INT, Identifier.fromName("a"), new Integer[]{1,2,3});
context.addVariable(arr);
// then call Arrays.length via FunctionCallExpression(Identifier.fromName("Arrays.length"), varExpr("a"))
```

## Implementation Phases

### Phase 1: Implement `Arrays.java`

Location: `core/src/main/java/com/elminster/jcp/module/base/arrays/Arrays.java`

**Deliverables:**
- [ ] Create `arrays/` package under `module/base/`
- [ ] Private constructor
- [ ] `length(Object array) → int` via `java.lang.reflect.Array.getLength`
- [ ] `slice(Integer[], int, int) → Integer[]` via `java.util.Arrays.copyOfRange`
- [ ] `slice(String[], int, int) → String[]` via `java.util.Arrays.copyOfRange`
- [ ] `slice(Boolean[], int, int) → Boolean[]` via `java.util.Arrays.copyOfRange`
- [ ] `slice(Double[], int, int) → Double[]` via `java.util.Arrays.copyOfRange`
- [ ] `contains(Integer[], Integer) → boolean` — linear scan with `.equals()`
- [ ] `contains(String[], String) → boolean`
- [ ] `contains(Boolean[], Boolean) → boolean`
- [ ] `contains(Double[], Double) → boolean`
- [ ] `sort(Integer[]) → Integer[]` — `java.util.Arrays.sort(a); return a`
- [ ] `sort(String[]) → String[]` — `java.util.Arrays.sort(a); return a`
- [ ] `sort(Boolean[]) → Boolean[]` — custom: count falses, fill falses then trues, return `a`
- [ ] `sort(Double[]) → Double[]` — `java.util.Arrays.sort(a); return a`

**Acceptance Criteria:**
- All methods compile with no warnings
- `sort(Boolean[]{true,false,true})` returns `[false,false,true]` — wait: `{true,false,true}` has
  1 false and 2 trues → result is `[false,true,true]`
- `length(new Integer[]{})` returns 0

### Phase 2: Register in `BaseModuleRegister`

**Deliverables:**
- [ ] Add `import com.elminster.jcp.module.base.arrays.Arrays;`
- [ ] Add `classes.add(Arrays.class);` to `classToRegister()`

**Acceptance Criteria:**
- `new RootEvalContext()` registers `Arrays` functions without error

### Phase 3: Eval mode tests (`ArraysTest.java`)

Location: `core/src/test/java/com/elminster/jcp/eval/function/ArraysTest.java`

Pattern:
```java
private EvalContext newContext() { return new RootEvalContext(); }

private Object eval(String method, SystemDataType returnType, String varName, Object arrayValue,
                    SystemDataType baseType, LiteralExpression... extraArgs) {
    EvalContext ctx = newContext();
    ArrayData arr = new ArrayData<>(baseType, Identifier.fromName(varName), arrayValue);
    ctx.addVariable(arr);
    Block program = new BlockImpl();
    LiteralExpression[] allArgs = // prepend VariableExpression for varName + extraArgs
    program.addStatement(new VariableDeclarationImpl("result", returnType,
        new FunctionCallExpression(Identifier.fromName("Arrays." + method), allArgs)));
    new EvalVisitor(ctx).visit(program);
    return ctx.getVariable("result").get();
}
```

**Deliverables:**
- [ ] `length(Integer[]{1,2,3})` → 3
- [ ] `length(Integer[]{})` → 0
- [ ] `slice(Integer[]{1,2,3,4}, 1, 3)` → `[2,3]`
- [ ] `slice(String[]{"a","b","c"}, 0, 2)` → `["a","b"]`
- [ ] `slice(Integer[]{1,2}, 0, 0)` → `[]` (empty slice edge case)
- [ ] `contains(Integer[]{1,2,3}, 2)` → true
- [ ] `contains(Integer[]{1,2,3}, 9)` → false
- [ ] `contains(String[]{"a","b"}, "b")` → true
- [ ] `contains(String[]{"a","b"}, "x")` → false
- [ ] `contains(Boolean[]{true,false}, false)` → true
- [ ] `sort(Integer[]{3,1,2})` → `[1,2,3]`
- [ ] `sort(String[]{"c","a","b"})` → `["a","b","c"]`
- [ ] `sort(Boolean[]{true,false,true})` → `[false,true,true]`
- [ ] `sort(Boolean[]{true,true})` → `[true,true]` (all-true coverage)
- [ ] `sort(Boolean[]{false,false})` → `[false,false]` (all-false coverage)
- [ ] `sort(Double[]{3.0,1.0,2.0})` → `[1.0,2.0,3.0]`

**Acceptance Criteria:**
- All tests pass in `mvn test -pl core`
- `Boolean[]` sort is tested with mixed, all-true, and all-false inputs

### Phase 4: Compile mode tests (`ArraysCompileTest.java`)

Location: `core/src/test/java/com/elminster/jcp/compile/module/ArraysCompileTest.java`

Pattern: use `StaticMethodCallExpression("Arrays", "method", ...)`. For array inputs,
use `Strings.split(...)` to produce `String[]` — the only array type constructible inline
from existing AST literals. `int[]`/`boolean[]`/`double[]` overloads are not testable in
compile mode until an array-literal AST node exists.

**Deliverables:**
- [ ] `length(Strings.split("a,b,c", ","))` → `assertEquals(3, ...)`
- [ ] `contains(Strings.split("a,b,c",","), "b")` → `assertTrue(...)`
- [ ] `contains(Strings.split("a,b,c",","), "x")` → `assertFalse(...)`
- [ ] `sort(Strings.split("c,a,b",","))` — compiles and invokes without throw
- [ ] `slice(Strings.split("a,b,c",","), 0, 2)` — compiles and invokes without throw

**Acceptance Criteria:**
- All compile tests pass
- `assertDoesNotThrow` wraps each `main.invoke(...)` call

### Phase 5: Verify coverage

**Deliverables:**
- [ ] `mvn test -pl core` — all tests pass
- [ ] `mvn verify -pl core` — JaCoCo ≥ 80% instruction and branch coverage

## Acceptance Criteria

### Functional Requirements

- [ ] All 4 methods callable as `Arrays.xxx(...)` in eval mode with boxed array types
- [ ] All 4 methods callable as `Arrays.xxx(...)` in compile mode (String[] overloads verified)
- [ ] `length` accepts any array type via `ANY` parameter dispatch
- [ ] `slice` uses from-inclusive, to-exclusive semantics; out-of-bounds throws
- [ ] `sort(Boolean[])` orders false before true; tested with mixed, all-true, all-false inputs
- [ ] `sort` is in-place and also returns the sorted array
- [ ] `contains` uses `.equals()` for object equality

### Non-Functional Requirements

- [ ] No changes to eval or compile infrastructure
- [ ] No changes to `TypeMapper`, `CompileModeClassConverter`, `ClassConverter`, or `DataTypeUtils`
- [ ] `mvn test -pl core` passes cleanly
- [ ] JaCoCo instruction coverage ≥ 80%, branch coverage ≥ 80%
- [ ] Every overload covered by at least one eval test

## Dependencies

- `java.util.Arrays` — `sort(T[])`, `copyOfRange`
- `java.lang.reflect.Array` — `getLength`
- `BaseModuleRegister` — one-line addition
- `ArrayData` + `context.addVariable()` — for eval test setup (stores boxed `Integer[]` etc.)
- `StaticMethodCallExpression` — compile tests (already exists)
- `AbstractCompileTest` — base class (already exists)
- `INT_ARRAY`, `DOUBLE_ARRAY`, `STRING_ARRAY`, `BOOLEAN_ARRAY` in `SystemDataType` — already exist
- Array type mappings in `CompileModeClassConverter` — already exist
- Array descriptors in `TypeMapper` — already exist

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `ClassConverter` can't resolve `Integer[]` to `INT_ARRAY` (maps to `ANY`) | M | M | Accept `ANY_ARRAY` dispatch; eval still works because `isCompatibleWith(ANY)` accepts all arrays. Functional correctness preserved. |
| `sort(Boolean[])` custom impl has off-by-one | L | L | Test all-true, all-false, and mixed inputs explicitly |
| `Boolean[]` sort branches miss 80% threshold | M | M | Three test cases (mixed, all-true, all-false) cover all branches |
| Compile mode `Integer[]`/`Boolean[]`/`Double[]` overloads untestable | H | L | Accepted limitation; documented. Will be addressed when array-literal AST node is added |
| `contains` `.equals()` on `Double` exact match surprises DSL users | L | L | Document in Javadoc; epsilon variant deferred |
| `sort` returns same reference (not copy) — DSL user mutates original | L | L | Document "in-place" semantics; by design |

## Testing Strategy

| Method | Eval tests | Compile tests |
|--------|-----------|---------------|
| `length` | Integer[], String[] (empty + non-empty) | String[] via Strings.split |
| `slice` | Integer[], String[] (including empty slice) | String[] via Strings.split |
| `contains` | Integer[], String[], Boolean[] (true/false) | String[] via Strings.split |
| `sort` | Integer[], String[], Boolean[] (3 cases), Double[] | String[] via Strings.split |

## Changes from Original Plan

1. **Boxed overloads** — switched from `int[]`/`boolean[]`/`double[]` to `Integer[]`/`Boolean[]`/`Double[]` to fix `ClassConverter` type resolution via `DataTypeUtils.getDataType(simpleName)`
2. **Explicit `ArrayData` constructor pattern** — added exact Java code snippet for eval test setup
3. **`boolean[]` sort coverage** — added all-true and all-false test cases explicitly
4. **Empty slice edge case** — added `slice(a, 0, 0)` test
5. **Compile-mode limitation documented** — explicitly scoped to `String[]` only until array-literal AST support is added
6. **`length(Object)` type registration behavior** — documented that it registers as `(ANY) → INT`, which is correct and expected

## References

- Original plan: `docs/plans/2026-03-18-feat-arrays-std-api-plan.md`
- Plan review: `docs/plans/2026-03-18-feat-arrays-std-api-plan-review.md`
- Issue: https://github.com/elminsterjimmy/jcp/issues/46
