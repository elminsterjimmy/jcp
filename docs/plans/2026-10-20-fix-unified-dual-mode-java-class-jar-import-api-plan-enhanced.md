---
title: "fix: unified dual-mode Java class/JAR import API for eval and compile modes (Enhanced)"
type: fix
date: 2026-10-20
issue: 49
review_date: 2026-10-20
original_plan: docs/plans/2026-10-20-fix-unified-dual-mode-java-class-jar-import-api-plan.md
---

# fix: unified dual-mode Java class/JAR import API (Enhanced)

> **Note:** This is an enhanced version of the original plan, incorporating feedback from the plan review conducted on 2026-10-20.

## Overview

DSL users who import a Java class into JCP must currently call two separate registration APIs — `ClassConverter.registerClass()` for eval mode and `CompileModeClassConverter.registerClass()` for compile mode. There is no unified entry point.

This plan creates `JcpModuleRegistry` as a single facade that registers a class (or all public classes from a JAR) into both modes, eliminating the dual-call burden. It also:
- Adds a new `LONG` basic type to the type system
- Expands type mappings for previously unmapped Java types (`char→STRING`, `float→DOUBLE`, `byte/short→INT`)
- Fixes the D5/D6 `getDataType()` NPE on unsupported parameter types

## Technical Approach

### Architecture

```
JcpModuleRegistry (new facade)
  ├── registerClass(Class<?>, module)  — stores for deferred dual registration
  ├── registerJar(Path, module)        — scans JAR → registerClass per public class
  ├── initEval(EvalContext)            — replays all registered classes via ClassConverter
  └── initCompile(CompileContext)      — replays all registered classes via CompileModeClassConverter
```

`JcpModuleRegistry` holds a `List<RegistrationEntry>` (class + module name) and replays them into whichever context is created. This decouples registration from context creation:

- `RootEvalContext` constructor calls `JcpModuleRegistry.initEval(this)` instead of its current inline loop.
- `BytecodeGenerator.registerModuleTypes(ctx)` calls `JcpModuleRegistry.initCompile(ctx)` instead of its current inline loop.
- `BaseModuleRegister.classToRegister()` is called once during `JcpModuleRegistry` static init to seed the base module classes.
- External/user classes are added via `JcpModuleRegistry.registerClass()` before context creation.

### JAR Import

`registerJar(Path jarPath, String module)`:
1. Create a `URLClassLoader` for the JAR.
2. Scan JAR entries for `.class` files, filter to `Modifier.isPublic(clazz)` and `!clazz.isMemberClass()` (unless static).
3. Call `registerClass(clazz, module)` for each.
4. Store the `URLClassLoader` so `initCompile()` can wire it into `MultiClassLoader.registerClassLoader()`.

### Type Mapping Expansion

Both `ClassConverter.getDataType()` and `CompileModeClassConverter.mapJavaTypeToDataType()` have gaps for several Java types. The complete mapping table:

| Java type | JCP type | Status | Notes |
|-----------|----------|--------|-------|
| `int`, `Integer` | `INT` | Existing | No change |
| `double`, `Double` | `DOUBLE` | Existing | No change |
| `boolean`, `Boolean` | `BOOLEAN` | Existing | No change |
| `String` | `STRING` | Existing | No change |
| `long`, `Long` | **`LONG`** (new) | **New** | New basic type, 2 JVM slots, `LLOAD`/`LSTORE`/`LRETURN` opcodes |
| `float`, `Float` | `DOUBLE` | Existing | Already mapped; JCP DOUBLE→Java float requires `D2F` narrowing at call site |
| `byte`, `Byte`, `short`, `Short` | `INT` | Existing | Already mapped; standard JVM promotion |
| `char`, `Character` | `STRING` | **Changed** | Currently `INT`; deliberate design change to `STRING` per user decision |
| `CharSequence` | `STRING` | **New** | Natural string mapping |
| `long[]` | `LONG_ARRAY` | **New** | Currently `ANY_ARRAY`; updated with new LONG type |
| `char[]` | `STRING_ARRAY` | **Changed** | Currently `INT_ARRAY`; follows char→STRING change |
| `byte[]` | `ANY_ARRAY` | **Not registered** | See Known Limitations — binary I/O via future ByteBuffer module |

### Design Philosophy: Minimal Type Surface

JCP intentionally keeps its type system small to remain easy to learn. The exposed types are:
`INT`, `LONG`, `DOUBLE`, `BOOLEAN`, `STRING`, `VOID`, and arrays of those.

Java types that don't map cleanly to this set (`byte[]`, complex generics, etc.) are **not automatically registered** during class import. Instead, they are wrapped behind purpose-built module classes (e.g., `Stdio`, `ValueBuffer`) that hide Java complexity from the DSL user. If binary I/O is needed in the future, a dedicated `ByteBuffer` module will provide a JCP-friendly API without exposing `byte[]` directly.

### New `LONG` Basic Type

**Scope of changes:**

| Layer | File(s) | Change |
|-------|---------|--------|
| Type system | `DataType.java` (`SystemDataType` enum) | Add `LONG("Long", NUMERIC)`, `LONG_ARRAY("Long[]", ANY)` |
| Type promotion | `TypePromotion.java` | Uncomment `INT_TO_LONG` (I2L), add `LONG_TO_DOUBLE` (L2D) |
| Type mapper | `TypeMapper.java` | Add LONG to `toDescriptor()` → `"J"`, `getLoadOpcode()` → `LLOAD`, `getStoreOpcode()` → `LSTORE`, `getReturnOpcode()` → `LRETURN`, `getSlotSize()` → 2 |
| Compiler: arithmetic | `ArithmeticCompiler.java` (base class) | Add LONG case to `compile()` and `resolveType()` — covers Plus, Minus, Divide, Multi, Mod |
| Compiler: comparison | `CompareCompiler.java` | Add LONG comparison via `LCMP` + branch |
| Compiler: literals | `LiteralCompiler.java` | Add long literal → `LDC` with long constant |
| Compiler: postfix | `PlusPlusCompiler.java`, `MinusMinusCompiler.java` | Add LONG increment/decrement |
| Compiler: variables | `VariableDeclarationCompiler.java` | LONG default value (`LCONST_0`) |
| Compiler: calls | `FunCallCompiler.java`, `MethodCallCompiler.java`, `StaticMethodCallCompiler.java` | LONG arg passing |
| Evaluator: arithmetic | `ArithmeticEvaluator.java` (base class) | Add LONG case — covers Plus, Minus, Divide, Multi, Mod evaluators |
| Evaluator: literals | `LiteralEvaluator.java` | Parse long literals, wrap as `Data<Long>` |
| Converters | `ClassConverter.java`, `CompileModeClassConverter.java` | Map `long→LONG`, `long[]→LONG_ARRAY` |

### Compile-Mode Narrowing at Java Call Boundaries

When JCP types don't exactly match Java method parameter types, the compiler must emit conversion bytecode at the call site:

| JCP type | Java param type | Bytecode | Notes |
|----------|-----------------|----------|-------|
| `DOUBLE` | `float` | `D2F` | **Narrowing** — potential precision loss. Documented as known limitation. |
| `STRING` | `char` | `INVOKEVIRTUAL String.charAt(0)` → `I2C` | Extract first char. Multi-char strings truncated — runtime check or documented limitation. |
| `INT` | `byte` | `I2B` | Standard narrowing |
| `INT` | `short` | `I2S` | Standard narrowing |
| `LONG` | `int` | `L2I` | Narrowing (rare — only if Java method takes int but JCP has long) |

### Constructor Bug (line 235)

Already fixed in current code. The `registerConstructors` method correctly uses `constructor.getParameters()`. Verified — no action needed.

## Implementation Phases

### Phase 1A: Expand Type Mappings (Quick Fix)

**Goal:** Fix NPE for unmapped types without adding LONG infrastructure.

- [ ] In `ClassConverter.getDataType()`: change `char→INT` to `char→STRING`, add `CharSequence→STRING`
- [ ] In `CompileModeClassConverter.mapJavaTypeToDataType()`: same changes
- [ ] Update array mappings: `char[]→STRING_ARRAY` (was `INT_ARRAY`)
- [ ] Map `long→ANY` temporarily (will become `LONG` in Phase 1B)
- [ ] Add unit tests: register a class with `char`/`float`/`byte`/`short` parameters — verify correct type resolution

**Acceptance criteria:**
- No NPE when registering classes with any primitive parameter type
- `char` parameters resolve to `STRING` in both modes
- All existing tests pass (verify no regressions from char→STRING change)

### Phase 1B: Add `LONG` Basic Type

**Goal:** Full LONG type support across eval and compile modes.

**Type system:**
- [ ] Add `LONG("Long", NUMERIC)` and `LONG_ARRAY("Long[]", ANY)` to `SystemDataType` enum
- [ ] Uncomment `INT_TO_LONG(INT, LONG, Opcodes.I2L)` in `TypePromotion`
- [ ] Add `LONG_TO_DOUBLE(LONG, DOUBLE, Opcodes.L2D)` to `TypePromotion`

**TypeMapper:**
- [ ] `toDescriptor(LONG)` → `"J"`, `toDescriptor(LONG_ARRAY)` → `"[J"`
- [ ] `getLoadOpcode(LONG)` → `LLOAD`
- [ ] `getStoreOpcode(LONG)` → `LSTORE`
- [ ] `getReturnOpcode(LONG)` → `LRETURN`
- [ ] `getSlotSize(LONG)` → `2` (LONG uses 2 JVM slots, like DOUBLE)
- [ ] `getArrayElementType(LONG_ARRAY)` → `LONG`

**Compilers (9 files):**
- [ ] `ArithmeticCompiler.java` — add LONG case to `compile()` (`LADD`/`LSUB`/`LMUL`/`LDIV`/`LREM`) and `resolveType()`. This base class covers Plus, Minus, Divide, Multi, Mod.
- [ ] `CompareCompiler.java` — add LONG comparison via `LCMP` + branch instruction
- [ ] `LiteralCompiler.java` — add long literal support via `LDC` with long constant
- [ ] `PlusPlusCompiler.java` — LONG increment (`LCONST_1` + `LADD`)
- [ ] `MinusMinusCompiler.java` — LONG decrement (`LCONST_1` + `LSUB`)
- [ ] `VariableDeclarationCompiler.java` — LONG default value (`LCONST_0`)
- [ ] `FunCallCompiler.java`, `MethodCallCompiler.java`, `StaticMethodCallCompiler.java` — LONG arg passing

**Evaluators:**
- [ ] `ArithmeticEvaluator.java` — add LONG case to arithmetic dispatch (covers all 5 ops)
- [ ] `LiteralEvaluator.java` — parse long literals, wrap as `Data<Long>`

**Converters:**
- [ ] `ClassConverter.getDataType()`: change `long→ANY` to `long→LONG`, `long[]→LONG_ARRAY`
- [ ] `CompileModeClassConverter.mapJavaTypeToDataType()`: same changes

**Tests:**
- [ ] LONG literal assignment and retrieval (eval + compile)
- [ ] LONG arithmetic: +, -, *, /, % (eval + compile)
- [ ] LONG comparison: ==, !=, <, >, <=, >= (eval + compile)
- [ ] LONG increment/decrement (compile)
- [ ] INT→LONG widening (implicit promotion)
- [ ] LONG→DOUBLE widening (implicit promotion)
- [ ] Register Java class with `long` parameter — verify LONG type resolution

**Acceptance criteria:**
- LONG values work end-to-end in both eval and compile modes
- Arithmetic, comparison, and increment/decrement operators work with LONG
- Type promotion chain INT→LONG→DOUBLE works correctly
- `long` and `long[]` Java types map to `LONG` and `LONG_ARRAY`

### Phase 2: Create `JcpModuleRegistry` Facade

- [ ] Create `core/src/main/java/com/elminster/jcp/module/JcpModuleRegistry.java`
- [ ] Inner class `RegistrationEntry` (class + module) as immutable record
- [ ] Store `List<RegistrationEntry>` as instance state (preserves insertion order)
- [ ] Store `List<URLClassLoader>` for JAR classloaders
- [ ] Implement `registerClass(Class<?> clazz, String module)` — adds entry to list
- [ ] Implement `initEval(EvalContext ctx)` — iterates entries, calls `ClassConverter.registerClass(entry.clazz, ctx, entry.module)`
- [ ] Implement `initCompile(CompileContext ctx)` — iterates entries, calls `CompileModeClassConverter.registerClass(entry.clazz, ctx, entry.module)`; wires JAR classloaders into `MultiClassLoader`
- [ ] Create singleton `DEFAULT` instance pre-loaded with `BaseModuleRegister.classToRegister()` as "base" module
- [ ] Provide public constructor for test-local instances (avoids singleton isolation issues)

**Tests:**
- [ ] Unit test: `registerClass` + `initEval` → class available in eval context
- [ ] Unit test: `registerClass` + `initCompile` → class available in compile context
- [ ] Unit test: registration order preserved

**Acceptance criteria:**
- Single `registerClass()` call makes a class available in both modes
- Registration order matches `BaseModuleRegister.classToRegister()` ordering

### Phase 3: Wire Into Existing Init Paths

- [ ] Refactor `RootEvalContext.registerSystemFunctions()` to call `JcpModuleRegistry.DEFAULT.initEval(this)`
- [ ] Refactor `BytecodeGenerator.registerModuleTypes(ctx)` to call `JcpModuleRegistry.DEFAULT.initCompile(ctx)`
- [ ] Migrate `BytecodeGenerator.additionalExternalClasses` — callers use `JcpModuleRegistry.DEFAULT.registerClass(clazz, "external")` before compilation instead of `BytecodeGenerator.registerExternalClass(clazz)`
- [ ] Deprecate (but keep) `BytecodeGenerator.registerExternalClass()` for backwards compatibility
- [ ] Verify all existing tests pass with zero behavioral change

**Acceptance criteria:**
- All existing tests pass unchanged
- `RootEvalContext` and `BytecodeGenerator` no longer directly call `ClassConverter`/`CompileModeClassConverter`
- `additionalExternalClasses` usage migrated to registry

### Phase 4: JAR Import Support

- [ ] Implement `registerJar(Path jarPath, String module)` in `JcpModuleRegistry`
- [ ] Create `URLClassLoader` for JAR via `new URLClassLoader(new URL[]{jarPath.toUri().toURL()})`
- [ ] Scan JAR entries: filter to `.class` files, load via URLClassLoader, filter to `Modifier.isPublic` and non-inner-class
- [ ] Store JAR classloaders in registry; `initCompile()` wires them into `MultiClassLoader.registerClassLoader()`
- [ ] `initEval()` also needs the JAR classloader for reflection-based eval calls

**Tests:**
- [ ] Create a small test JAR (build in test resources or use maven-jar-plugin in test scope)
- [ ] Test: register JAR → class available in eval mode
- [ ] Test: register JAR → class available in compile mode
- [ ] Test: JAR with non-public/inner classes → only public top-level classes registered

**Acceptance criteria:**
- `registerJar()` scans and registers all public top-level classes
- Both eval and compile modes can call methods from JAR classes
- Non-public and inner classes are filtered out

### Phase 5: Compile-Mode Call-Site Narrowing Bridges

- [ ] In method call compilers: detect when JCP type doesn't match Java parameter type
- [ ] Emit `D2F` when passing JCP `DOUBLE` to Java `float` parameter
- [ ] Emit `String.charAt(0)` + `I2C` when passing JCP `STRING` to Java `char` parameter
- [ ] Emit `I2B`/`I2S` when passing JCP `INT` to Java `byte`/`short` parameter
- [ ] Emit `L2I` when passing JCP `LONG` to Java `int` parameter (rare case)

**Tests:**
- [ ] Call Java method with `float` param from JCP DOUBLE value
- [ ] Call Java method with `char` param from JCP STRING value
- [ ] Call Java method with `byte`/`short` param from JCP INT value

**Acceptance criteria:**
- JCP values correctly narrow to Java parameter types at call boundaries
- No `VerifyError` or `ClassCastException` at runtime

### Phase 6: Coverage Verification

- [ ] Run full test suite: `mvn test -pl core`
- [ ] Verify JaCoCo ≥ 80% instruction and ≥ 80% branch coverage
- [ ] Verify constructor bug is not present (document as already fixed in issue)
- [ ] Run `mvn verify -pl core` to confirm coverage thresholds pass

## Acceptance Criteria

### Functional Requirements

- [ ] `JcpModuleRegistry.registerClass(Class<?>, String)` registers for both modes
- [ ] `JcpModuleRegistry.registerJar(Path, String)` scans and registers all public classes
- [ ] Eval mode: registered classes work via `EvalVisitor`
- [ ] Compile mode: registered classes work via `JcpCompiler.compileAndLoad()`
- [ ] New `LONG` basic type works end-to-end: literals, arithmetic, comparison, increment/decrement
- [ ] Type promotion chain: `INT→LONG→DOUBLE` works in both modes
- [ ] `char→STRING` mapping works in both modes
- [ ] `float→DOUBLE` mapping with `D2F` narrowing at Java call boundary
- [ ] `long[]→LONG_ARRAY` and `char[]→STRING_ARRAY` array mappings
- [ ] Existing module functions (Stdio, Math, Strings, etc.) work unchanged

### Non-Functional Requirements

- [ ] JaCoCo ≥ 80% instruction and ≥ 80% branch coverage
- [ ] No breaking changes to existing public APIs (both converters remain usable directly)
- [ ] Registration order preserved (e.g., SortKey before Arrays dependency)
- [ ] LONG correctly uses 2 JVM slots (no local variable index corruption)

## Dependencies

- `MultiClassLoader.registerClassLoader()` already exists — no new work needed
- `BaseModuleRegister.classToRegister()` already provides ordered class list
- `ExternalClassType` shared between both modes — no changes needed
- `TypePromotion` already has commented-out LONG entries (lines 54-58) — minimal new code

## Known Limitations

These are deliberate design trade-offs to keep JCP's type surface minimal and the language easy to learn.

### 1. `byte[]` not automatically registered

Java methods with `byte[]` parameters are **not registered** during class import. `byte[]` maps to `ANY_ARRAY` and such methods are skipped.

**Reason:** `byte[]` is a mutable buffer pattern in Java I/O. Passing a JCP `INT_ARRAY` converted to `byte[]` would break out-parameter semantics (Java writes into the array, but the JCP copy never sees the changes). Binary I/O will be supported via a dedicated `ByteBuffer` module class in a future release.

### 2. `char→STRING` overload ambiguity

If a Java class has both `foo(char)` and `foo(String)`, both resolve to JCP `STRING` parameter type, causing ambiguous overload resolution.

**Reason:** Rare in practice. Document as a known limitation. Future fix: store the original Java parameter type in `ExternalMethodDef` for disambiguation.

### 3. `D2F` narrowing precision loss

When JCP passes a `DOUBLE` value to a Java method expecting `float`, the compiler emits `D2F` which may lose precision for values outside float range.

**Reason:** JCP has no separate `float` type. This mirrors how Python handles float — single precision is an implementation detail, not a user concern.

### 4. `char` is not a user-facing type

JCP does not expose `char` as a DSL type. Users should not write `char`-style numeric comparisons (`'c' == 42`). The `char→STRING` mapping means characters are always strings in JCP. At Java call boundaries, `String.charAt(0)` extracts the char for the Java method, so third-party library code that does numeric char comparisons internally works correctly.

## Known Learnings Applied

- `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md` — Compile mode requires BOTH `addDataType()` AND classloader visibility. `JcpModuleRegistry.initCompile()` does both: calls `CompileModeClassConverter.registerClass()` (which calls `ctx.addDataType()`) AND wires JAR classloaders into `MultiClassLoader`.

- `docs/solutions/runtime-errors/noclassdeffounderror-struct-classes-not-loaded.md` — Context instance matters. `JcpModuleRegistry` passes the caller-provided context through, never creating its own local copy.

- `docs/solutions/runtime-errors/50-type-bridge-isolation-report.md` (D5/D6) — `getDataType()` NPE on unmapped types. Fixed in Phase 1A/1B with proper type mappings: `LONG` (new), `char→STRING`, `long[]→LONG_ARRAY`.

- `docs/solutions/logic-errors/struct-type-resolution-in-variable-declarations.md` — Type resolution must check context for custom types. The unified registry populates context before any compilation step.

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking existing module init order (SortKey before Arrays) | M | H | `JcpModuleRegistry` preserves insertion order of `BaseModuleRegister.classToRegister()` list |
| JAR scanning picks up non-public or inner classes | L | M | Filter to `Modifier.isPublic(clazz)` and `!clazz.isMemberClass()` unless static |
| `LONG` type adds complexity across ~12 files | M | M | `ArithmeticCompiler`/`ArithmeticEvaluator` are base classes — one edit covers 5 arithmetic ops. `TypePromotion` entries already pre-written. Follow exact INT/DOUBLE patterns. |
| LONG 2-slot allocation corrupts local variable indices | M | H | `TypeMapper.getSlotSize()` already handles DOUBLE (2 slots). Add LONG to same check. Verify `CompileContext.allocateLocal()` uses `getSlotSize()`. |
| `char→STRING` change breaks existing registered Java classes | L | M | Deliberate design change per user decision. Existing JCP DSL code using `char` as `INT` is unlikely. Add backwards-compatibility note in release. |
| `D2F` narrowing at call site loses precision | L | L | Document as known limitation. Only affects Java methods with `float` params called from JCP — rare in practice. |
| `STRING→char` truncation (multi-char strings) | L | M | Emit `String.charAt(0)` — truncates silently. Document as known limitation. |
| `MultiClassLoader` parent chain doesn't see JAR classes | M | H | `initCompile()` explicitly calls `registerClassLoader()` on `MultiClassLoader` before class loading |
| Singleton `DEFAULT` instance causes test isolation issues | L | L | Public constructor for test-local instances; `DEFAULT` only used in production init paths |
| INT→LONG→DOUBLE promotion chain: transitive widening may surprise users | L | L | Consistent with JVM semantics. Document in type system docs. |

## Changes from Original Plan

1. **Split Phase 1 into 1A (type mappings) and 1B (LONG type)** — Phase 1 was overloaded; splitting allows the registry to proceed without blocking on LONG
2. **Added explicit file-by-file LONG deliverables** — Original underestimated scope; ~12 files need changes, with `ArithmeticCompiler`/`ArithmeticEvaluator` as the key base classes
3. **Fixed D2F direction** — Corrected "widening" to "narrowing" at Java call boundary; DOUBLE→float is precision-losing
4. **Added Phase 5 for call-site narrowing bridges** — Was implicit in original; now explicit with specific bytecode instructions
5. **Added eval-mode LONG support** — Original focused on compile-mode; `ArithmeticEvaluator` and `LiteralEvaluator` now explicitly covered
6. **Added `long[]→LONG_ARRAY` and `char[]→STRING_ARRAY`** — Array companion mappings were missing
7. **Referenced TypePromotion pre-written entries** — Lines 54-58 already have LONG constants commented out
8. **Added LONG 2-slot risk** — Missing from original risk analysis; LONG uses 2 JVM slots like DOUBLE

## References

- Original plan: `docs/plans/2026-10-20-fix-unified-dual-mode-java-class-jar-import-api-plan.md`
- Plan review: `docs/plans/2026-10-20-fix-unified-dual-mode-java-class-jar-import-api-plan-review.md`
- Issue: https://github.com/elminsterjimmy/jcp/issues/49
