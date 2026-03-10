---
title: "feat: extend Assertions STD API in base module (Enhanced)"
type: feat
date: 2026-03-10
issue: 48
review_date: 2026-03-10
original_plan: docs/plans/2026-03-10-feat-extend-assertions-std-api-plan.md
---

# feat: extend Assertions STD API in base module (Enhanced)

> **Note:** Enhanced version of the original plan, incorporating review feedback from 2026-03-10.

## Overview

Extend the existing `Assertions` class in `com.elminster.jcp.module.base.assertions` with four additional static assertion methods: `assertFalse`, `assertEquals`, `assertNull`, and `assertNotNull`. The class already has `assertTrue` and is registered in `BaseModuleRegister` — no structural changes are needed, only new methods and tests.

## Technical Approach

The existing `Assertions.java` pattern:
- Static methods throw `AssertException` on failure
- Private `fastfail(boolean expected, boolean actual)` helper formats error messages

**`fastfail` strategy (decided):** Add a second overload `fastfail(Object expected, Object actual)` that uses `String.valueOf()` for the message. This serves `assertEquals` and the null-check methods without changing the existing boolean overload.

```java
private static void fastfail(Object expected, Object actual) {
    throw new AssertException(String.format("Assertions failed: expected [%s] but got [%s]",
            expected, actual));
}
```

**Null literal in tests:** `NullLiteral.INSTANCE` exists — use `LiteralExpression.of(NullLiteral.INSTANCE)` in both eval and compile test cases for `assertNull`/`assertNotNull`.

**No changes needed to:**
- `BaseModuleRegister` — `Assertions.class` already registered
- Eval infrastructure — `StaticMethodCallEvaluator` discovers methods via reflection
- Compile infrastructure — `FunCallCompiler`/`StaticMethodCallCompiler` discovers via reflection

## Implementation Phases

### Phase 1: Extend `Assertions.java`

- [ ] Add private `fastfail(Object expected, Object actual)` overload
- [ ] Add `assertFalse(boolean condition)`
- [ ] Add `assertEquals(int expected, int actual)`
- [ ] Add `assertEquals(double expected, double actual)`
- [ ] Add `assertEquals(boolean expected, boolean actual)`
- [ ] Add `assertEquals(String expected, String actual)`
- [ ] Add `assertEquals(Object expected, Object actual)`
- [ ] Add `assertNull(Object value)`
- [ ] Add `assertNotNull(Object value)`

### Phase 2: Eval mode tests (`AssertionsTest.java`)

- [ ] `assertFalse(false)` — passes without exception
- [ ] `assertFalse(true)` — throws `AssertException`
- [ ] `assertEquals(int, int)` — equal case passes, unequal throws with message containing values
- [ ] `assertEquals(double, double)` — equal case passes, unequal throws
- [ ] `assertEquals(boolean, boolean)` — equal case passes, unequal throws
- [ ] `assertEquals(String, String)` — equal case passes, unequal throws
- [ ] `assertNull(null)` — passes (use `LiteralExpression.of(NullLiteral.INSTANCE)`)
- [ ] `assertNull(nonNull)` — throws `AssertException`
- [ ] `assertNotNull(nonNull)` — passes
- [ ] `assertNotNull(null)` — throws `AssertException` (use `LiteralExpression.of(NullLiteral.INSTANCE)`)

### Phase 3: Compile mode tests (`AssertionsCompileTest.java`)

- [ ] `assertFalse(false)` — `assertDoesNotThrow`
- [ ] `assertFalse(true)` — `InvocationTargetException` wrapping `AssertException`
- [ ] `assertEquals(int, int)` — equal case passes, unequal throws
- [ ] `assertEquals(double, double)` — equal case passes, unequal throws (use `DoubleLiteral`)
- [ ] `assertEquals(boolean, boolean)` — equal case passes, unequal throws
- [ ] `assertEquals(String, String)` — equal case passes, unequal throws (use `StringLiteral`)
- [ ] `assertNull(null)` — passes (use `LiteralExpression.of(NullLiteral.INSTANCE)`)
- [ ] `assertNull(nonNull)` — throws
- [ ] `assertNotNull(nonNull)` — passes
- [ ] `assertNotNull(null)` — throws

## Acceptance Criteria

### Functional Requirements

- [ ] `assertFalse(boolean)` works in eval and compile modes
- [ ] `assertEquals` overloads for `int`, `double`, `boolean`, `String`, `Object` all work
- [ ] `assertNull(Object)` works in eval and compile modes
- [ ] `assertNotNull(Object)` works in eval and compile modes
- [ ] All methods throw `AssertException` with a message containing both expected and actual values on failure

### Non-Functional Requirements

- [ ] No changes to `BaseModuleRegister`, eval infrastructure, or compile infrastructure
- [ ] JaCoCo coverage ≥ 80% instructions and branches on `core` module
- [ ] `mvn test -pl core` passes cleanly
- [ ] Every new method has at least one passing test and one failing test (both paths covered)

## Dependencies

- `AssertException` — `com.elminster.common.util.AssertException` (exists)
- `NullLiteral.INSTANCE` — `com.elminster.jcp.ast.expression.literal.NullLiteral` (exists, singleton)
- `LiteralExpression.of(NullLiteral.INSTANCE)` — usable directly in tests
- `AbstractCompileTest` — base class for compile tests (exists)
- `EvalVisitor` + `RootEvalContext` — eval test infrastructure (exists)

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `assertEquals(Object, Object)` may receive call intended for `assertEquals(String, String)` via reflection | L | M | Java resolves most-specific overload first; covered by testing String overload explicitly |
| `assertNull`/`assertNotNull` in compile mode — passing null via `StaticMethodCallExpression` | L | M | **Resolved:** use `LiteralExpression.of(NullLiteral.INSTANCE)` |
| Coverage drop if fail-path tests are missing | M | M | Each new method has explicit passing + failing test case in both modes |
| `assertEquals(double, double)` floating-point precision edge cases | L | L | Use exact-equality literals in tests (e.g. `1.0 == 1.0`) to avoid FP precision issues |

## Changes from Original Plan

1. **`fastfail` strategy decided** — generic `fastfail(Object, Object)` overload added to Phase 1 instead of leaving "generalize or overload" ambiguous
2. **Null-literal risk resolved** — `NullLiteral.INSTANCE` documented as the concrete solution; risk moved from open to resolved
3. **Missing test cases added** — `assertEquals(double, double)` and `assertEquals(boolean, boolean)` tests added to Phases 2 and 3
4. **Compile-mode null tests clarified** — explicit note on using `LiteralExpression.of(NullLiteral.INSTANCE)` in compile mode tests

## References

- Original plan: `docs/plans/2026-03-10-feat-extend-assertions-std-api-plan.md`
- Plan review: `docs/plans/2026-03-10-feat-extend-assertions-std-api-plan-review.md`
- Issue: https://github.com/elminsterjimmy/jcp/issues/48
