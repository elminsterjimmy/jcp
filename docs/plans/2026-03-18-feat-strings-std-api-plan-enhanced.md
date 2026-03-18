---
title: "feat: Strings STD API for base module (Enhanced)"
type: feat
date: 2026-03-18
issue: 44
review_date: 2026-03-18
original_plan: docs/plans/2026-03-18-feat-strings-std-api-plan.md
---

# feat: Strings STD API for base module (Enhanced)

> **Note:** Enhanced version of the original plan, incorporating review feedback from 2026-03-18.

## Overview

Add `com.elminster.jcp.module.base.strings.Strings` — a static utility class providing 13 DSL-friendly string operations callable from JCP programs in both eval and compile mode. Register it in `BaseModuleRegister`. Write tests that **assert return values** (not just absence of exceptions) for every method in both modes.

## Technical Approach

**Pattern:** Identical to `Assertions.java`. Each method is `public static <ReturnType> methodName(String s, ...)` delegating directly to `s.javaMethod(...)`.

**Infrastructure:** No changes needed to eval or compile infrastructure. Both `StaticMethodCallEvaluator` and `FunCallCompiler` discover methods via reflection. `CompileModeClassConverter.mapJavaTypeToDataType` already handles `String[].class → STRING_ARRAY`.

**Calling conventions (both work today):**
- Shorthand: `Strings.length(s)` — `base::` prefix omitted (base is the only module)
- Explicit: `base::Strings.length(s)` — also valid via `FunCallCompiler`

**`split` return type:** `String.split(regex)` returns `String[]`. `CompileModeClassConverter` line 140 maps `String[].class → STRING_ARRAY`; `TypeMapper.toDescriptor(STRING_ARRAY)` emits `[Ljava/lang/String;`. Pipeline confirmed end-to-end.

**`replace` semantics:** Uses `String.replace(CharSequence, CharSequence)` — literal replacement, not regex.

**`split` semantics:** Uses `String.split(String regex)` — delimiter is a regex pattern, consistent with Java.

**`contains` parameter type in `Strings.java`:** Use `String` (not `CharSequence`) so the reflection dispatcher correctly matches `SystemDataType.STRING` arguments.

### Eval Test Pattern (return-value capture)

To verify return values in eval mode, assign the call result to a named variable via `VariableDeclarationImpl`, then read it back:

```java
EvalContext context = new RootEvalContext();
Block program = new BlockImpl();
program.addStatement(new VariableDeclarationImpl(
    "result",
    SystemDataType.STRING,           // match the method's return type
    new FunctionCallExpression(
        Identifier.fromName("Strings.upper"),
        LiteralExpression.of(StringLiteral.of("hello"))
    )
));
new EvalVisitor(context).visit(program);
assertEquals("HELLO", context.getVariable("result").get());
```

For `int` parameters (e.g., `sub`), use `LiteralExpression.of(Literal.of(1))`.

### Compile Test Pattern (return-value assertion)

`AssertionsCompileTest` uses a void `main` and verifies side-effects (throws). For `Strings`, which returns values, **use the compiled `Assertions.assertEquals` call inline** to keep the void-main pattern — no need to read the `main.invoke(...)` return value:

```java
// Compile: Assertions.assertEquals("HELLO", Strings.upper("hello"))
Block program = new BlockImpl();
program.addStatement(new ExpressionStatement(
    new StaticMethodCallExpression("Assertions", "assertEquals",
        LiteralExpression.of(StringLiteral.of("HELLO")),
        new StaticMethodCallExpression("Strings", "upper",
            LiteralExpression.of(StringLiteral.of("hello")))
    )
));
Method main = compileAndGetMain("TestStringsUpper", program);
assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
```

This keeps tests consistent with `AssertionsCompileTest` and verifies the value without needing to return it from `main`.

## Implementation Phases

### Phase 1: Implement `Strings.java`

- [ ] Create `core/src/main/java/com/elminster/jcp/module/base/strings/Strings.java`
- [ ] Add `length(String s) → int` delegating to `s.length()`
- [ ] Add `sub(String s, int from, int to) → String` delegating to `s.substring(from, to)`
- [ ] Add `concat(String a, String b) → String` delegating to `a.concat(b)`
- [ ] Add `indexOf(String s, String t) → int` delegating to `s.indexOf(t)`
- [ ] Add `contains(String s, String t) → boolean` delegating to `s.contains(t)` (param type: `String`)
- [ ] Add `upper(String s) → String` delegating to `s.toUpperCase()`
- [ ] Add `lower(String s) → String` delegating to `s.toLowerCase()`
- [ ] Add `trim(String s) → String` delegating to `s.trim()`
- [ ] Add `replace(String s, String old, String newStr) → String` delegating to `s.replace(old, newStr)`
- [ ] Add `startsWith(String s, String prefix) → boolean` delegating to `s.startsWith(prefix)`
- [ ] Add `endsWith(String s, String suffix) → boolean` delegating to `s.endsWith(suffix)`
- [ ] Add `isEmpty(String s) → boolean` delegating to `s.isEmpty()`
- [ ] Add `split(String s, String delimiter) → String[]` delegating to `s.split(delimiter)`

### Phase 2: Register in `BaseModuleRegister`

- [ ] Add `Strings.class` to `BaseModuleRegister.classToRegister()`

### Phase 3: Eval mode tests (`StringsTest.java`)

Location: `core/src/test/java/com/elminster/jcp/eval/function/StringsTest.java`

**Pattern:** `VariableDeclarationImpl("result", <type>, new FunctionCallExpression(Identifier.fromName("Strings.xxx"), args...))` then assert `context.getVariable("result").get()`.

Int literal construction: `LiteralExpression.of(Literal.of(1))`.

- [ ] `length("hello")` → assign to `int result`, assert `result == 5`
- [ ] `length("")` → assign to `int result`, assert `result == 0`
- [ ] `sub("hello", 1, 3)` → assign to `String result`, assert `result.equals("el")`
- [ ] `concat("foo", "bar")` → assign to `String result`, assert `result.equals("foobar")`
- [ ] `indexOf("hello", "ll")` → assign to `int result`, assert `result == 2`
- [ ] `indexOf("hello", "x")` → assign to `int result`, assert `result == -1`
- [ ] `contains("hello", "ell")` → assign to `boolean result`, assert `result == true`
- [ ] `contains("hello", "x")` → assign to `boolean result`, assert `result == false`
- [ ] `upper("hello")` → assign to `String result`, assert `result.equals("HELLO")`
- [ ] `lower("HELLO")` → assign to `String result`, assert `result.equals("hello")`
- [ ] `trim("  hi  ")` → assign to `String result`, assert `result.equals("hi")`
- [ ] `replace("aXbXc", "X", "-")` → assign to `String result`, assert `result.equals("a-b-c")`
- [ ] `startsWith("hello", "he")` → assign to `boolean result`, assert `true`
- [ ] `startsWith("hello", "lo")` → assign to `boolean result`, assert `false`
- [ ] `endsWith("hello", "lo")` → assign to `boolean result`, assert `true`
- [ ] `endsWith("hello", "he")` → assign to `boolean result`, assert `false`
- [ ] `isEmpty("")` → assign to `boolean result`, assert `true`
- [ ] `isEmpty("x")` → assign to `boolean result`, assert `false`
- [ ] `split("a,b,c", ",")` → assign to `String[] result`, assert `result.length == 3`, `result[0].equals("a")`, `result[1].equals("b")`, `result[2].equals("c")`

### Phase 4: Compile mode tests (`StringsCompileTest.java`)

Location: `core/src/test/java/com/elminster/jcp/compile/module/StringsCompileTest.java`

**Pattern:** Wrap the `Strings.xxx(...)` call inside `Assertions.assertEquals(expected, Strings.xxx(...))` to assert the value within the compiled program. This keeps the void-main convention of `AssertionsCompileTest`.

```java
private StaticMethodCallExpression assertEq(LiteralExpression expected,
                                             StaticMethodCallExpression actual) {
    return new StaticMethodCallExpression("Assertions", "assertEquals", expected, actual);
}
```

- [ ] `upper("hello")` → `Assertions.assertEquals("HELLO", Strings.upper("hello"))` — `assertDoesNotThrow`
- [ ] `lower("HELLO")` → `Assertions.assertEquals("hello", Strings.lower("HELLO"))` — `assertDoesNotThrow`
- [ ] `trim("  hi  ")` → `Assertions.assertEquals("hi", Strings.trim("  hi  "))` — `assertDoesNotThrow`
- [ ] `concat("foo", "bar")` → `Assertions.assertEquals("foobar", Strings.concat("foo", "bar"))` — `assertDoesNotThrow`
- [ ] `replace("aXbXc", "X", "-")` → `Assertions.assertEquals("a-b-c", Strings.replace(...))` — `assertDoesNotThrow`
- [ ] `sub("hello", 1, 3)` → `Assertions.assertEquals("el", Strings.sub("hello", 1, 3))` — `assertDoesNotThrow`
- [ ] `length("hello")` — verify via `Assertions.assertEquals(5, Strings.length("hello"))` — `assertDoesNotThrow`
- [ ] `length("")` — verify via `Assertions.assertEquals(0, Strings.length(""))` — `assertDoesNotThrow`
- [ ] `indexOf("hello", "ll")` → `Assertions.assertEquals(2, Strings.indexOf("hello", "ll"))` — `assertDoesNotThrow`
- [ ] `indexOf("hello", "x")` → `Assertions.assertEquals(-1, Strings.indexOf(...))` — `assertDoesNotThrow`
- [ ] `contains("hello", "ell")` — assertTrue passes → `assertDoesNotThrow`; `contains("hello","x")` — assertFalse variant
- [ ] `startsWith("hello", "he")` — assertTrue → `assertDoesNotThrow`; `startsWith("hello", "lo")` — assertFalse
- [ ] `endsWith("hello", "lo")` — assertTrue → `assertDoesNotThrow`; `endsWith("hello", "he")` — assertFalse
- [ ] `isEmpty("")` — assertTrue → `assertDoesNotThrow`; `isEmpty("x")` — assertFalse
- [ ] `split("a,b,c", ",")` — compile and invoke, cast `main.invoke(...)` return to `String[]`, assert length 3 and elements (this method returns `String[]` from `main`, so the return-value approach is appropriate here)

### Phase 5: Verify coverage

- [ ] Run `mvn test -pl core` — all tests pass
- [ ] Run `mvn verify -pl core` — JaCoCo thresholds pass (≥ 80% instruction and branch)

## Acceptance Criteria

### Functional Requirements

- [ ] All 13 methods implemented and callable via `Strings.xxx(...)` in eval mode
- [ ] All 13 methods callable via `Strings.xxx(...)` in compile mode
- [ ] `base::Strings.xxx(...)` explicit module syntax also works (via existing `FunCallCompiler` path)
- [ ] `split` returns `String[]` correctly in both modes
- [ ] `replace` uses literal replacement (not regex)
- [ ] Null input throws `NullPointerException` naturally (no extra handling needed)
- [ ] Return values are **asserted** in tests (not just `assertDoesNotThrow`)

### Non-Functional Requirements

- [ ] No changes to eval or compile infrastructure
- [ ] No changes to `TypeMapper`, `CompileModeClassConverter`, or any evaluator/compiler
- [ ] `mvn test -pl core` passes cleanly
- [ ] JaCoCo instruction coverage ≥ 80%, branch coverage ≥ 80%
- [ ] Every method has at least one test case in both eval and compile mode

## Dependencies

- `BaseModuleRegister` — one-line addition of `Strings.class`
- `StaticMethodCallExpression` — used in compile tests (already exists)
- `FunctionCallExpression` — used in eval tests (already exists)
- `VariableDeclarationImpl` — used in eval tests to capture return values (already exists)
- `StringLiteral.of(...)`, `LiteralExpression.of(...)`, `Literal.of(int)` — already exist
- `AbstractCompileTest` — base class for compile tests (already exists)
- `STRING_ARRAY` in `SystemDataType` — already exists
- `CompileModeClassConverter.mapJavaTypeToDataType(String[].class)` → `STRING_ARRAY` — already works
- `Assertions.assertEquals(Object, Object)` — already exists, used in compile tests as inline assertion

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `split` — `String[]` return type not propagated correctly in compile mode | L | M | Confirmed: `CompileModeClassConverter` line 140 maps `String[].class → STRING_ARRAY`; `TypeMapper.toDescriptor` emits `[Ljava/lang/String;` at line 92 |
| `replace` parameter name `new` clashes with Java keyword | M | L | Use `newStr` as the Java parameter name |
| Coverage drops below 80% if edge-case branches are untested | M | M | Each method has both a positive and a negative/edge-case test |
| `contains` parameter type mismatch | L | L | Use `String` (not `CharSequence`) in `Strings.java` — passes to `s.contains(t)` correctly |
| Eval tests assert nothing (only `visit` without reading result) | M | M | **Resolved:** use `VariableDeclarationImpl` + `context.getVariable("result").get()` pattern |
| Compile tests assert nothing (only `assertDoesNotThrow`) | M | M | **Resolved:** wrap calls in `Assertions.assertEquals(expected, Strings.xxx(...))` inside compiled program |

## Changes from Original Plan

1. **Eval test pattern clarified** — specified `VariableDeclarationImpl` + `context.getVariable` as the correct return-value capture mechanism
2. **Compile test pattern clarified** — specified inline `Assertions.assertEquals(expected, Strings.xxx(...))` wrapping to assert values within the compiled program
3. **Int literal construction documented** — `LiteralExpression.of(Literal.of(1))` for `sub`/`indexOf` int parameters
4. **`contains` parameter type noted** — use `String` not `CharSequence` in `Strings.java`
5. **Return-value assertion added as acceptance criterion** — tests must assert values, not just absence of throws
6. **`split` compile test clarified** — use direct `main.invoke(...)` return-value cast for the `String[]` case

## References

- Original plan: `docs/plans/2026-03-18-feat-strings-std-api-plan.md`
- Plan review: `docs/plans/2026-03-18-feat-strings-std-api-plan-review.md`
- Brainstorm: `docs/brainstorms/2026-03-18-strings-std-api-brainstorm.md`
- Issue: https://github.com/elminsterjimmy/jcp/issues/44
