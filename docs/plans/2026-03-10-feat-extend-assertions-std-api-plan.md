---
title: "feat: extend Assertions STD API in base module"
type: feat
date: 2026-03-10
issue: 48
---

# feat: extend Assertions STD API in base module

## Overview

Extend the existing `Assertions` class in `com.elminster.jcp.module.base.assertions` with four additional static assertion methods: `assertFalse`, `assertEquals`, `assertNull`, and `assertNotNull`. The class already has `assertTrue` and is registered in `BaseModuleRegister` — no structural changes are needed, only new methods and tests.

## Technical Approach

The existing `Assertions.java` pattern is simple and consistent:
- Static methods that throw `AssertException` on failure
- A private `fastfail(...)` helper formats the error message

New methods follow the same pattern:
- `assertFalse(boolean)` — negate the condition check
- `assertEquals` — overloads for `int`, `double`, `boolean`, `String`, `Object`
- `assertNull(Object)` — check `value == null`
- `assertNotNull(Object)` — check `value != null`

`fastfail` needs to be generalized (or overloaded) to support non-boolean expected/actual messages (e.g. for `assertEquals` and null checks).

**No changes needed to:**
- `BaseModuleRegister` — `Assertions.class` already registered
- Eval infrastructure — `StaticMethodCallEvaluator` discovers methods via reflection
- Compile infrastructure — `FunCallCompiler` / `StaticMethodCallCompiler` discovers via reflection

## Implementation Phases

### Phase 1: Extend `Assertions.java`

- [ ] Add `assertFalse(boolean condition)`
- [ ] Add `assertEquals(int expected, int actual)`
- [ ] Add `assertEquals(double expected, double actual)`
- [ ] Add `assertEquals(boolean expected, boolean actual)`
- [ ] Add `assertEquals(String expected, String actual)`
- [ ] Add `assertEquals(Object expected, Object actual)`
- [ ] Add `assertNull(Object value)`
- [ ] Add `assertNotNull(Object value)`
- [ ] Generalize `fastfail` (or add overloads) to produce clear messages for all new failure cases

### Phase 2: Eval mode tests

- [ ] Test `assertFalse(false)` passes
- [ ] Test `assertFalse(true)` throws `AssertException`
- [ ] Test `assertEquals(int, int)` passes when equal, throws when not equal
- [ ] Test `assertEquals(String, String)` passes when equal, throws when not equal
- [ ] Test `assertNull(null)` passes, `assertNull(nonNull)` throws
- [ ] Test `assertNotNull(nonNull)` passes, `assertNotNull(null)` throws

### Phase 3: Compile mode tests

- [ ] Test `assertFalse(false)` passes (using `StaticMethodCallExpression`)
- [ ] Test `assertFalse(true)` throws `AssertException` (via `InvocationTargetException`)
- [ ] Test `assertEquals(int, int)` — equal and unequal cases
- [ ] Test `assertEquals(String, String)` — equal and unequal cases
- [ ] Test `assertNull` / `assertNotNull` — pass and fail cases

## Acceptance Criteria

### Functional Requirements

- [ ] `assertFalse(boolean)` added and works in eval and compile modes
- [ ] `assertEquals` added with overloads for `int`, `double`, `boolean`, `String`, `Object`
- [ ] `assertNull(Object)` added and works in eval and compile modes
- [ ] `assertNotNull(Object)` added and works in eval and compile modes
- [ ] All methods throw `AssertException` with a descriptive message on failure

### Non-Functional Requirements

- [ ] No changes to `BaseModuleRegister` (class already registered)
- [ ] No changes to eval or compile infrastructure
- [ ] JaCoCo coverage ≥ 80% instructions and branches on `core` module
- [ ] `mvn test -pl core` passes cleanly

## Dependencies

- `AssertException` already exists at `com.elminster.common.util.AssertException`
- `Assertions` class already exists and is registered — no scaffolding required
- Existing test infrastructure (`AbstractCompileTest`, `EvalVisitor`) already in place

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `assertEquals(Object, Object)` may clash with `assertEquals(String, String)` via reflection | L | M | Java resolves most-specific overload; test both cases to confirm |
| Null literal passing via eval mode may need `NullLiteral` AST node | L | M | Check how `assertNull` arg is represented; may use `LiteralExpression.of(Literal.ofNull())` or similar |
| Coverage drop if fail-path tests are missing | M | M | Ensure every new method has at least one passing and one failing test case |
