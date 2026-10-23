---
title: "Custom Comparator Sort via SortKey.sortByFunc() (Enhanced)"
type: feature
date: 2026-10-22
issue: 55
review_date: 2026-10-23
original_plan: docs/plans/2026-10-22-feat-function-references-and-comparator-sort-plan.md
---

# Issue #55: Custom Comparator Sort via SortKey.sortByFunc() (Enhanced)

> **Note:** This is an enhanced version of the original plan, incorporating feedback from the plan review conducted on 2026-10-23.

## Overview

JCP users can sort struct arrays by field path (`SortKey.by("age")`), but cannot sort by **computed comparison** — e.g. age difference, product of fields, or any logic beyond a single field lookup.

**Solution**: Extend `SortKey` with `sortByFunc(String functionName)`. The user declares a comparison function and passes its name as a string. At sort-time, the runtime resolves and invokes that function for each comparison.

```
func myCompare(a: Any, b: Any) -> int {
    return a.age - b.age
}

let k = SortKey.sortByFunc("myCompare")
Arrays.sort(persons, k)
```

Uses the existing `Arrays.sort(Object[], SortKey)` overloads — no new overload needed.

### Key Design Decisions

- **No lambdas, no function references, no FunctionType** — functions remain blocks, not values
- **No new AST nodes or type system changes** — pure library extension
- **String-based function name** — resolved at sort-time
- **Reuses existing SortKey type and Arrays.sort overloads**

---

## Design: Two-Mode Resolution

`SortKey` gains a second internal mode: **field-path** (existing) vs **function-name** (new).

### The Challenge

`SortKey.toComparator()` runs as plain Java. In eval mode, `EvalContext` holds the named function but isn't accessible from `SortKey`. In compile mode, the named function is a `public static` method on the generated class, but `SortKey` can't see that class.

### Solution

**Eval mode**: Intercept `SortKey` arguments in `ClassConverter.doFunction()` — where `EvalContext` is available — and pre-resolve the function into a `Comparator<Object>` before the reflective call to `Arrays.sort`.

**Compile mode**: `SortKey.sortByFunc(String, Class<?>)` two-arg factory takes the declaring class. `FunCallCompiler` detects the one-arg `SortKey.sortByFunc("name")` pattern and emits the two-arg version with `LDC` for the current class. At runtime, `toComparator()` uses reflection on the provided `Class<?>` to invoke the method.

---

## Implementation Phases

### Phase 1: Cleanup and SortKey Extension

**Dependencies:** None (SortKey field-path sorting already merged via issue #56)

**Deliverables:**
- [ ] Delete `core/src/main/java/com/elminster/jcp/eval/data/FunctionType.java` (rejected approach artifact)
- [ ] Extend `SortKey.java` with function-name mode

**SortKey.java changes:**

New fields:
- `String functionName` — null for field-path mode
- `Class<?> declaringClass` — the class containing the named function (compile mode)
- `Comparator<Object> resolvedComparator` — pre-built comparator (eval mode)

New factories:
- `sortByFunc(String functionName)` — eval mode (resolved later via `EvalContext`)
- `sortByFunc(String functionName, Class<?> declaringClass)` — compile mode

New methods:
- `getFunctionName()` — returns function name or null
- `withResolvedComparator(Comparator<Object>)` — returns copy with pre-built comparator

`toComparator()` changes:
1. If `resolvedComparator != null` → return it (eval mode, pre-resolved)
2. If `functionName != null && declaringClass != null` → resolve via reflection:
   `declaringClass.getMethod(functionName, Object.class, Object.class)`, wrap in `Comparator`. **Validate return type is int** — throw `IllegalStateException` if not.
3. If `functionName != null && declaringClass == null` → throw `IllegalStateException("Function-name SortKey not resolved — use withResolvedComparator or provide declaringClass")`
4. Otherwise → existing field-path logic (unchanged)

**Acceptance Criteria:**
- `SortKey.sortByFunc("name")` creates a SortKey in function-name mode
- `SortKey.by("field")` behavior is unchanged (regression check)
- `toComparator()` throws clear error when function-name SortKey not resolved
- Immutability preserved: `withResolvedComparator` returns a new instance

### Phase 2: Eval-Mode Integration

**Dependencies:** Phase 1

**Deliverables:**
- [ ] Add `EvalContext`-aware `convertArgumentsForJava` overload in `ClassConverter.java`
- [ ] Wire the overload through `doFunction` in `registerStaticMethod`

**ClassConverter.java changes:**

Add a new overload: `convertArgumentsForJava(Data[] arguments, Method method, EvalContext evalContext)`.

In the new overload, after the existing Dictionary→Map / ValueBuffer→List conversions:

```java
if (val instanceof SortKey && ((SortKey) val).getFunctionName() != null) {
    SortKey sk = (SortKey) val;
    String fname = sk.getFunctionName();
    Function func = evalContext.getFunction(fname);
    if (func == null) {
        throw new EvaluationException("Comparator function not found: " + fname);
    }
    // Validate arity: must accept exactly 2 parameters
    ParameterDef[] params = func.getParameterDefs();
    if (params.length != 2) {
        throw new EvaluationException("Comparator function '" + fname
            + "' must accept exactly 2 parameters, got " + params.length);
    }
    Comparator<Object> adapter = (a, b) -> {
        // Wrap a, b as AnyData(SystemDataType.ANY)
        // Invoke function body via FunctionEvaluator pattern
        // Validate result is numeric, return intValue()
    };
    argValues[i] = sk.withResolvedComparator(adapter);
}
```

In `registerStaticMethod` (line ~161), the `doFunction` lambda already has `evalContext` — update the call from `convertArgumentsForJava(arguments, method)` to `convertArgumentsForJava(arguments, method, evalContext)`.

**Important:** The adapter must validate that the function returns a numeric value. If non-numeric, throw `EvaluationException("Comparator function 'X' must return int, got: <actual type>")`.

**Acceptance Criteria:**
- Eval-mode sort by function comparator works end-to-end
- Unknown function name → clear `EvaluationException`
- Wrong function arity → clear `EvaluationException`
- Non-integer return from comparator → clear `EvaluationException`
- Existing field-path sorts are unaffected

### Phase 3: Compile-Mode Integration

**Dependencies:** Phase 1

**Deliverables:**
- [ ] Add pattern match in `FunCallCompiler.compileModuleFunctionCall()` for `SortKey.sortByFunc(String)`
- [ ] Verify `resolveType()` handles `SortKey.sortByFunc` correctly (should work automatically via reflection)

**FunCallCompiler.java changes:**

In `compileModuleFunctionCall()`, after `discoverMethod` resolves the method, add a targeted pattern match:

When the resolved method is `SortKey.sortByFunc(String)`:
- Instead of emitting `INVOKESTATIC SortKey.sortByFunc(String)SortKey`, emit:
  - The string argument is already compiled/on stack
  - `LDC Type.getType("L" + ctx.getClassName() + ";")` → pushes `Class<?>` reference
  - `INVOKESTATIC SortKey.sortByFunc(String, Class)SortKey` → two-arg version

This is tightly scoped — only applies to this one method signature.

**Verification for `resolveType()`**: The `discoverMethod` in `compileModuleFunctionCall` already discovers the Java method via reflection and returns the correct return type (`SortKey`). TypeMapper should resolve this automatically. Verify this in testing — if not, add a special case.

**Acceptance Criteria:**
- Compile-mode sort by function comparator works end-to-end
- Generated bytecode calls two-arg `sortByFunc(String, Class<?>)`
- Comparator function resolved at runtime via reflection
- Function name that doesn't exist → clear runtime error
- Existing field-path sorts compile correctly (regression check)

### Phase 4: Testing

**Dependencies:** Phases 2, 3

**Deliverables:**
- [ ] `ArraysSortByFuncTest.java` — eval-mode integration tests
- [ ] `ArraysSortByFuncCompileTest.java` — compile-mode integration tests
- [ ] Coverage ≥ 85% instruction and branch

**Test Cases — Eval Mode (`ArraysSortByFuncTest.java`):**

| Test | Description |
|------|-------------|
| Sort descending by age | Comparator returns `a.age - b.age`, verify order |
| Sort ascending by name length | Computed comparison, not a field lookup |
| Mixed SortKeys: function + field-path | Primary: function comparator, secondary: `SortKey.by("name")` |
| Unknown function name | → `EvaluationException` with clear message |
| Wrong function arity (1 param) | → `EvaluationException` |
| Wrong function arity (3 params) | → `EvaluationException` |
| Non-integer return | → `EvaluationException` |

**Test Cases — Compile Mode (`ArraysSortByFuncCompileTest.java`):**

| Test | Description |
|------|-------------|
| Sort by compiled comparator | End-to-end `JcpCompiler` test |
| Verify runtime reflection resolution | `SortKey.sortByFunc(String, Class)` resolves method correctly |
| Mixed SortKeys: function + field-path | Both modes coexist in a single sort |
| Non-existent function at runtime | → clear runtime error |

---

## End-to-End Flows

### Eval Mode
1. User declares `func myCompare(a: Any, b: Any) -> int { ... }`
2. `SortKey.sortByFunc("myCompare")` → SortKey with `functionName = "myCompare"`
3. `Arrays.sort(persons, k)` dispatches to auto-registered module function
4. `doFunction(args, evalContext)` → `convertArgumentsForJava` detects SortKey function name
5. Looks up `"myCompare"` in `evalContext.getFunction()` → validates arity (2) → builds Comparator adapter
6. Adapter wraps each invocation: wrap `a`, `b` as `AnyData(ANY)`, invoke function body, validate int return
7. Replaces arg with `sortKey.withResolvedComparator(adapter)`
8. `ReflectUtil.invoke` → `Arrays.sort(arr, resolvedKey)` → `toComparator()` returns adapter

### Compile Mode
1. `func myCompare(a: Any, b: Any) -> int` compiles as `public static int myCompare(Object, Object)`
2. `SortKey.sortByFunc("myCompare")` → compiler detects pattern, emits two-arg `sortByFunc("myCompare", MyClass.class)` with `LDC`
3. `Arrays.sort(persons, k)` → `INVOKESTATIC Arrays.sort(Object[], SortKey)`
4. Runtime: `toComparator()` → reflectively finds `MyClass.myCompare(Object, Object)` → validates return type is int → wraps in Comparator

---

## Acceptance Criteria

### Functional Requirements

- [ ] `SortKey.sortByFunc("functionName")` creates a function-name mode SortKey
- [ ] Eval mode: comparator function invoked correctly during sort, elements reordered
- [ ] Compile mode: comparator function resolved via reflection at runtime, elements reordered
- [ ] Mixed SortKeys (field-path + function-name) work together in a single sort call
- [ ] Existing `SortKey.by("field")` sorts are completely unaffected in both modes
- [ ] Unknown function name produces a clear, actionable error message in both modes
- [ ] Wrong function arity produces a clear error message in both modes

### Non-Functional Requirements

- [ ] JaCoCo coverage ≥ 85% instruction and 85% branch on core module
- [ ] All existing tests pass (no regressions)
- [ ] `SortKey` remains immutable — `withResolvedComparator()` creates a copy
- [ ] No new AST nodes, no new types, no grammar changes
- [ ] `FunctionType.java` (rejected approach) is deleted

---

## Dependencies

| Dependency | Status | Notes |
|------------|--------|-------|
| Issue #56 (SortKey field-path sorting) | **Closed/Merged** | Baseline — `SortKey.by()`, `toComparator()`, `Arrays.sort(Object[], SortKey...)` |
| PR #54 (Arrays STD API) | **Merged** | `Arrays` class with sort overloads |
| ASM 9.6 | Available | Used by `FunCallCompiler` for bytecode emission |

---

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Reflection perf in compile mode | Low | Low | Method resolved once per sort call, cached in Comparator lambda — not per comparison |
| Function name/arity mismatch | Medium | Low | Eval: validated in interceptor with clear error. Compile: `NoSuchMethodException` → clear message |
| Breaks existing field-path sorts | Low | High | New fields default to null; existing `toComparator()` path unchanged; all existing tests must pass |
| FunCallCompiler special case grows | Low | Medium | Tightly scoped; only matches `SortKey.sortByFunc(String)` — no pattern match for any other method |
| Classloader visibility (compile) | Low | Medium | `declaringClass` passed at construction via `LDC` — no cross-classloader lookup |
| Wrong parameter types in comparator function | Medium | Low | Eval: function parameters are ANY, so type mismatch deferred to function body. Compile: method signature is `(Object, Object)int`, wrong types → compile error |
| Non-int return from comparator | Medium | Medium | Both modes validate return is numeric; clear error message if not |

---

## Testing Strategy

**Unit tests:** SortKey new factories and `toComparator()` branches (function-name mode)

**Integration tests (eval):** Full JCP runtime path: declare function → create SortKey → call `Arrays.sort` → verify order. Covers happy path + error paths.

**Integration tests (compile):** Full `JcpCompiler.compileAndLoad` path: compile program with function + sort → invoke `main` → verify via return value or side effect.

**Regression:** All existing `ArraysSortStructTest` cases must continue to pass.

---

## Success Metrics

- All 11+ new test cases pass
- Zero regressions in existing test suite
- Coverage remains ≥ 85% instruction and branch
- Plan implemented in ≤ 3 files modified + 2 new test files + 1 deleted file

---

## Changes from Original Plan

**Improvements made based on review feedback:**

1. **Added explicit implementation phases** — Four phases with clear dependencies and ordering, replacing the implicit file-list approach.
2. **Added comprehensive acceptance criteria** — Functional and non-functional requirements enumerated separately, all testable.
3. **Added return-type validation** — Both modes now validate that comparator functions return int/numeric, with clear error messages.
4. **Added negative test cases for compile mode** — Original plan only had error-path tests for eval mode.
5. **Moved FunctionType.java cleanup to Phase 1** — Prevents forgetting the cleanup; establishes a clean starting point.
6. **Added mixed SortKey test cases** — Explicit coverage for combining function-name and field-path SortKeys in both modes.
7. **Added dependency status table** — Confirms baseline requirements (issue #56, PR #54) are met.
8. **Expanded risk analysis** — Added likelihood/impact columns, added risks for wrong parameter types and non-int return.

---

## References

- Original plan: `docs/plans/2026-10-22-feat-function-references-and-comparator-sort-plan.md`
- Plan review: `docs/plans/2026-10-22-feat-function-references-and-comparator-sort-plan-review.md`
- Issue: https://github.com/elminsterjimmy/jcp/issues/55
