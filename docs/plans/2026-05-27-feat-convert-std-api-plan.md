---
title: "feat: Convert STD API for base module"
type: feat
date: 2026-05-27
issue: 47
---

# feat: Convert STD API for base module

## Overview

Add `com.elminster.jcp.module.base.convert.Convert` — a static utility class providing simple type conversion utilities (between primitives and `String`) callable from JCP programs in both eval and compile modes. Register the class in `BaseModuleRegister`. Provide passing tests for every method (and every overload) in both modes.

API surface:

```
Convert.toString(int)      → String
Convert.toString(double)   → String
Convert.toString(boolean)  → String
Convert.toInt(String)      → int
Convert.toDouble(String)   → double
Convert.toBoolean(String)  → boolean
```

## Technical Approach

**Pattern:** Identical to `Strings.java` and `Assertions.java`. Each method is `public static <ReturnType> methodName(<args>)` and delegates directly to a single Java standard-library call. No null guarding, no locale-aware parsing, no number bases.

**Delegation table:**

| JCP method | Java delegate |
|------------|--------------|
| `toString(int)` | `String.valueOf(int)` |
| `toString(double)` | `String.valueOf(double)` |
| `toString(boolean)` | `String.valueOf(boolean)` |
| `toInt(String)` | `Integer.parseInt(s)` |
| `toDouble(String)` | `Double.parseDouble(s)` |
| `toBoolean(String)` | `Boolean.parseBoolean(s)` |

**Infrastructure:** No changes needed in eval or compile infrastructure. Both `StaticMethodCallEvaluator` and `FunCallCompiler` discover methods via reflection. Critically, `FunCallCompiler.discoverReturnType` (around line 367) already performs overload resolution by matching method name + parameter-compatible types, so the three `toString` overloads will be selected based on the JCP argument type without additional plumbing.

**Calling conventions (both work today):**
- Shorthand: `Convert.toInt(s)` — `base::` prefix omitted (base is the only module)
- Explicit: `base::Convert.toInt(s)` — also valid via `FunCallCompiler`

**Type mapping (already covered by `CompileModeClassConverter`):**
- `int` ↔ `SystemDataType.INT`
- `double` ↔ `SystemDataType.DOUBLE`
- `boolean` ↔ `SystemDataType.BOOLEAN`
- `String` ↔ `SystemDataType.STRING`

**Failure semantics:** No defensive try/catch. Bad inputs propagate naturally:
- `Convert.toInt("abc")` → `NumberFormatException` (from `Integer.parseInt`)
- `Convert.toDouble("abc")` → `NumberFormatException`
- `Convert.toBoolean("yes")` → returns `false` (Java semantics — only `"true"` is true, case-insensitive)
- `Convert.toString(null)` (boolean overload not callable with null; primitive overloads can't receive null)

## Implementation Phases

### Phase 1: Implement `Convert.java`

Location: `core/src/main/java/com/elminster/jcp/module/base/convert/Convert.java`

- [ ] Create new package directory `module/base/convert/`
- [ ] Create `Convert.java` with private constructor
- [ ] Add `public static String toString(int v)` → `String.valueOf(v)`
- [ ] Add `public static String toString(double v)` → `String.valueOf(v)`
- [ ] Add `public static String toString(boolean v)` → `String.valueOf(v)`
- [ ] Add `public static int toInt(String s)` → `Integer.parseInt(s)`
- [ ] Add `public static double toDouble(String s)` → `Double.parseDouble(s)`
- [ ] Add `public static boolean toBoolean(String s)` → `Boolean.parseBoolean(s)`
- [ ] Add a class-level Javadoc describing usage from JCP (`Convert.toInt(s)` / `base::Convert.toInt(s)`)

### Phase 2: Register in `BaseModuleRegister`

- [ ] Add `import com.elminster.jcp.module.base.convert.Convert;`
- [ ] Add `classes.add(Convert.class);` to `BaseModuleRegister.classToRegister()` (place alphabetically after `Assertions` and before `Logger` for consistency with existing order)

### Phase 3: Eval mode tests (`ConvertTest.java`)

Location: `core/src/test/java/com/elminster/jcp/eval/function/ConvertTest.java`

Follow `StringsTest.java` pattern: build a `Block`, declare a `result` variable assigned from `FunctionCallExpression(Identifier.fromName("Convert.<method>"), <args>)`, run with `new EvalVisitor(context).visit(program)`, then assert on `context.getVariable("result").get()`.

Helpers to mirror from `StringsTest`:
- `eval(String method, SystemDataType returnType, LiteralExpression... args)`
- `str(String s)`, `int_(int n)`, plus new `dbl(double d)` and `bool(boolean b)` helpers using `Literal.of(...)`

- [ ] `toString(42)` → `"42"`
- [ ] `toString(-7)` → `"-7"`
- [ ] `toString(3.14)` → `"3.14"`
- [ ] `toString(0.0)` → `"0.0"`
- [ ] `toString(true)` → `"true"`
- [ ] `toString(false)` → `"false"`
- [ ] `toInt("42")` → `42`
- [ ] `toInt("-7")` → `-7`
- [ ] `toInt("0")` → `0`
- [ ] `toDouble("3.14")` → `3.14`
- [ ] `toDouble("-0.5")` → `-0.5`
- [ ] `toBoolean("true")` → `true`
- [ ] `toBoolean("false")` → `false`
- [ ] `toBoolean("TRUE")` → `true` (Boolean.parseBoolean is case-insensitive)
- [ ] `toBoolean("yes")` → `false` (anything not `"true"` → false)

### Phase 4: Compile mode tests (`ConvertCompileTest.java`)

Location: `core/src/test/java/com/elminster/jcp/compile/module/ConvertCompileTest.java`

Follow `StringsCompileTest.java` pattern: build a list of `StaticMethodCallExpression` calls (most wrapped in `Assertions.assertEquals`), compile via `compiler.compileAndLoad(program, className)`, invoke `main` and assert `assertDoesNotThrow`.

Helpers:
- `convert(String method, LiteralExpression... args)` → `new StaticMethodCallExpression("Convert", method, args)`
- `assertEq(LiteralExpression expected, StaticMethodCallExpression actual)`
- `str(String)`, `int_(int)`, `dbl(double)`, `bool(boolean)` literal builders

- [ ] `assertEq(str("42"), convert("toString", int_(42)))` — int overload selected
- [ ] `assertEq(str("3.14"), convert("toString", dbl(3.14)))` — double overload selected
- [ ] `assertEq(str("true"), convert("toString", bool(true)))` — boolean overload selected
- [ ] `assertEq(str("false"), convert("toString", bool(false)))`
- [ ] `assertEq(int_(42), convert("toInt", str("42")))`
- [ ] `assertEq(int_(-7), convert("toInt", str("-7")))`
- [ ] `assertEq(dbl(3.14), convert("toDouble", str("3.14")))`
- [ ] `assertTrue(convert("toBoolean", str("true")))`
- [ ] `assertFalse(convert("toBoolean", str("false")))`
- [ ] `assertFalse(convert("toBoolean", str("yes")))`

### Phase 5: Verify build, tests, and coverage

- [ ] `mvn test -pl core` — all tests pass
- [ ] `mvn verify -pl core` — JaCoCo thresholds pass (≥ 80% instruction and branch)
- [ ] Open `core/target/site/jacoco/index.html` and confirm `Convert` shows ≥ 80% on both axes

## Acceptance Criteria

### Functional Requirements

- [ ] `Convert` class implemented with all 4 method names and all 3 `toString` overloads
- [ ] All methods callable via `Convert.xxx(...)` in eval mode
- [ ] All methods callable via `Convert.xxx(...)` in compile mode
- [ ] `base::Convert.xxx(...)` explicit module syntax also works (via existing `FunCallCompiler` path)
- [ ] Overload resolution selects the correct `toString(int|double|boolean)` based on JCP argument type
- [ ] `Boolean.parseBoolean` case-insensitivity preserved (e.g., `"TRUE"` → `true`)
- [ ] Bad numeric input throws `NumberFormatException` naturally (no extra handling)

### Non-Functional Requirements

- [ ] No changes to eval or compile infrastructure
- [ ] No changes to `TypeMapper`, `CompileModeClassConverter`, or any evaluator/compiler
- [ ] `mvn test -pl core` passes cleanly
- [ ] JaCoCo instruction coverage ≥ 80%, branch coverage ≥ 80%
- [ ] Every method (and every `toString` overload) has at least one test case in both eval and compile modes

## Dependencies

- `BaseModuleRegister` — one-line addition of `Convert.class`
- `StaticMethodCallExpression` — used in compile tests (already exists)
- `FunctionCallExpression` — used in eval tests (already exists)
- `StringLiteral.of(...)`, `Literal.of(int|double|boolean)`, `LiteralExpression.of(...)` — already exist
- `AbstractCompileTest` — base class for compile tests (already exists)
- `SystemDataType.INT`, `DOUBLE`, `BOOLEAN`, `STRING` — already exist
- `CompileModeClassConverter.mapJavaTypeToDataType(int.class | double.class | boolean.class | String.class)` — already works
- `FunCallCompiler.discoverReturnType` overload resolution — already works (matches by name + parameter-compatible types)

## Known Learnings Applied

`docs/solutions/` was searched for keywords related to this feature (convert, parse, toString, toInt, toDouble, toBoolean, overload, module, register, std, base). Existing solution docs concern struct type resolution and classloading; none directly apply to a static utility class with primitive/String signatures.

- None directly applicable. The existing `Strings`/`Assertions` modules are the strongest precedent — both work without infrastructure changes, which sets the expectation that this work is also infrastructure-free.

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Overload resolution picks the wrong `toString` variant in compile mode | L | M | `FunCallCompiler.discoverReturnType` already handles this for `Strings.sub(String, int, int)`. Cover all three overloads with explicit compile-mode tests; `Ambiguous module function` would fail loudly at compile time. |
| `Convert.toString(int)` clashes with `Object.toString()` inherited on the class | L | L | Methods are `static`, so they live on the class not instances; reflection scans `getDeclaredMethods()` which excludes inherited members. No conflict. |
| `Double.parseDouble` parses `"3,14"` differently across locales | L | L | `Double.parseDouble` is locale-independent (always `.`-decimal). Document in Javadoc. |
| `Integer.parseInt` does not accept `"+1"` on JDK < 7, or accepts it on ≥ 7 | L | L | Project uses modern JDK; default behavior accepts leading `+`. Tests use unsigned and `-` cases only. |
| Coverage drops below 80% if some overloads or branches are untested | M | M | Each overload + parsing target has at least one positive test; `toBoolean` covered with both `"true"`, `"false"`, and a non-`"true"` case to exercise both Boolean.parseBoolean branches. |
| `toString` shadows `java.lang.Object.toString()` confusing the static-method scanner | L | L | The scanner filters by `Modifier.isStatic`; Object's `toString` is instance-level and excluded. Verified by inspection of `FunCallCompiler.discoverReturnType`. |
| Adding `Convert` between alphabetical positions in `BaseModuleRegister` breaks the `SortKey` ordering comment | L | L | Insert AFTER `Assertions`, leaving the `SortKey`-before-`Arrays` invariant untouched. |
