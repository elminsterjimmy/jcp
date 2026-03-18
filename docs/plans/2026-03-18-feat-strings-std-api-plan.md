---
title: "feat: Strings STD API for base module"
type: feat
date: 2026-03-18
issue: 44
brainstorm: docs/brainstorms/2026-03-18-strings-std-api-brainstorm.md
---

# feat: Strings STD API for base module

## Overview

Add `com.elminster.jcp.module.base.strings.Strings` — a static utility class providing 13 DSL-friendly string operations callable from JCP programs in both eval and compile mode. Register it in `BaseModuleRegister`. Write passing + failing tests for every method in both modes.

## Technical Approach

**Pattern:** Identical to `Assertions.java`. Each method is `public static <ReturnType> methodName(String s, ...)` delegating directly to `s.javaMethod(...)`.

**Infrastructure:** No changes needed to eval or compile infrastructure. Both `StaticMethodCallEvaluator` and `FunCallCompiler` discover methods via reflection. `CompileModeClassConverter.mapJavaTypeToDataType` already handles `String[].class → STRING_ARRAY`.

**Calling conventions (both work today):**
- Shorthand: `Strings.length(s)` — `base::` prefix omitted (base is the only module)
- Explicit: `base::Strings.length(s)` — also valid via `FunCallCompiler`

**`split` return type:** `String.split(regex)` returns `String[]`. `CompileModeClassConverter` maps `String[].class` → `STRING_ARRAY`, and `TypeMapper.toDescriptor(STRING_ARRAY)` emits `[Ljava/lang/String;`. End-to-end pipeline confirmed working.

**`replace` semantics:** Uses `String.replace(CharSequence, CharSequence)` — literal replacement, not regex.

**`split` semantics:** Uses `String.split(String regex)` — delimiter is a regex pattern, consistent with Java.

## Implementation Phases

### Phase 1: Implement `Strings.java`

- [ ] Create `core/src/main/java/com/elminster/jcp/module/base/strings/Strings.java`
- [ ] Add `length(String s) → int`  delegating to `s.length()`
- [ ] Add `sub(String s, int from, int to) → String` delegating to `s.substring(from, to)`
- [ ] Add `concat(String a, String b) → String` delegating to `a.concat(b)`
- [ ] Add `indexOf(String s, String t) → int` delegating to `s.indexOf(t)`
- [ ] Add `contains(String s, String t) → boolean` delegating to `s.contains(t)`
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

Follow `AssertionsTest.java` pattern: build a `Block`, add an `ExpressionStatement(FunctionCallExpression(...))`, run with `newVisitor().visit(block)`.

- [ ] `length("hello")` → passes (result == 5); `length("")` → passes (result == 0)
- [ ] `sub("hello", 1, 3)` → passes (result == "el")
- [ ] `concat("foo", "bar")` → passes (result == "foobar")
- [ ] `indexOf("hello", "ll")` → passes (result == 2); `indexOf("hello", "x")` → passes (result == -1)
- [ ] `contains("hello", "ell")` → passes (result == true); `contains("hello", "x")` → passes (result == false)
- [ ] `upper("hello")` → passes (result == "HELLO")
- [ ] `lower("HELLO")` → passes (result == "hello")
- [ ] `trim("  hi  ")` → passes (result == "hi")
- [ ] `replace("aXbXc", "X", "-")` → passes (result == "a-b-c")
- [ ] `startsWith("hello", "he")` → passes (true); `startsWith("hello", "lo")` → passes (false)
- [ ] `endsWith("hello", "lo")` → passes (true); `endsWith("hello", "he")` → passes (false)
- [ ] `isEmpty("")` → passes (true); `isEmpty("x")` → passes (false)
- [ ] `split("a,b,c", ",")` → passes (result array length == 3, elements "a","b","c")

### Phase 4: Compile mode tests (`StringsCompileTest.java`)

Location: `core/src/test/java/com/elminster/jcp/compile/module/StringsCompileTest.java`

Follow `AssertionsCompileTest.java` pattern: use `StaticMethodCallExpression`, compile with `compiler.compileAndLoad(program, className)`, invoke `main`.

- [ ] `length("hello")` — `assertDoesNotThrow`; verify return value == 5
- [ ] `sub("hello", 1, 3)` — verify return value == "el"
- [ ] `concat("foo", "bar")` — verify return value == "foobar"
- [ ] `indexOf("hello", "ll")` — verify return value == 2
- [ ] `contains("hello", "ell")` — verify return value == true
- [ ] `upper("hello")` — verify return value == "HELLO"
- [ ] `lower("HELLO")` — verify return value == "hello"
- [ ] `trim("  hi  ")` — verify return value == "hi"
- [ ] `replace("aXbXc", "X", "-")` — verify return value == "a-b-c"
- [ ] `startsWith("hello", "he")` — verify true; `startsWith("hello", "lo")` — verify false
- [ ] `endsWith("hello", "lo")` — verify true; `endsWith("hello", "he")` — verify false
- [ ] `isEmpty("")` — verify true; `isEmpty("x")` — verify false
- [ ] `split("a,b,c", ",")` — verify returned `String[]` has length 3, elements ["a","b","c"]

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

### Non-Functional Requirements

- [ ] No changes to eval or compile infrastructure
- [ ] No changes to `TypeMapper`, `CompileModeClassConverter`, or any evaluator/compiler
- [ ] `mvn test -pl core` passes cleanly
- [ ] JaCoCo instruction coverage ≥ 80%, branch coverage ≥ 80%
- [ ] Every method has at least one test case in both eval and compile mode

## Dependencies

- `com.elminster.common.util.AssertException` — not needed (no throws on failure)
- `BaseModuleRegister` — one-line addition of `Strings.class`
- `StaticMethodCallExpression` — used in compile tests (already exists)
- `FunctionCallExpression` — used in eval tests (already exists)
- `StringLiteral.of(...)`, `LiteralExpression.of(...)` — already exist
- `AbstractCompileTest` — base class for compile tests (already exists)
- `STRING_ARRAY` in `SystemDataType` — already exists
- `CompileModeClassConverter.mapJavaTypeToDataType(String[].class)` → `STRING_ARRAY` — already works

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `split` — `String[]` return type not propagated correctly in compile mode | L | M | Confirmed: `CompileModeClassConverter` line 140 maps `String[].class → STRING_ARRAY`; `TypeMapper.toDescriptor` emits `[Ljava/lang/String;` at line 92 |
| `replace` parameter name `new` clashes with Java keyword | M | L | Use `newStr` as the Java parameter name |
| Coverage drops below 80% if edge-case branches are untested | M | M | Each method has both a positive and a negative/edge-case test |
| `contains` return type — `CharSequence` vs `String` | L | L | `s.contains(t)` accepts `CharSequence`; passing `String` is fine |
