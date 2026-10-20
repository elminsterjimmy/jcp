---
title: "fix: unified dual-mode Java class/JAR import API for eval and compile modes"
type: fix
date: 2026-10-20
issue: 49
---

# fix: unified dual-mode Java class/JAR import API for eval and compile modes

## Overview

DSL users who import a Java class into JCP must currently call two separate registration APIs — `ClassConverter.registerClass()` for eval mode and `CompileModeClassConverter.registerClass()` for compile mode. There is no unified entry point.

This plan creates `JcpModuleRegistry` as a single facade that registers a class (or all public classes from a JAR) into both modes, eliminating the dual-call burden. It also fixes the D5/D6 `getDataType()` NPE on unsupported parameter types that will surface immediately when the unified API exercises `ClassConverter`.

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
2. Scan JAR entries for `.class` files, filter to public top-level classes.
3. Call `registerClass(clazz, module)` for each.
4. Store the `URLClassLoader` so `initCompile()` can wire it into `MultiClassLoader.registerClassLoader()`.

### D6 Fix: Expand Type Mappings for Unmapped Java Types

`ClassConverter.getDataType()` and `CompileModeClassConverter.mapJavaTypeToDataType()` return `null` for unmapped Java types (`char`, `long`, `float`, `byte`, `short`, `CharSequence`). Callers use the result without null-checking, causing NPE during function-name generation.

**Fix:** Add proper type mappings instead of falling back to `ANY`:

| Java type | JCP type | Rationale |
|-----------|----------|-----------|
| `long` | **`LONG`** (new) | New basic type — `int` is widely used, `long` deserves its own type with `LLOAD`/`LSTORE`/`LRETURN` opcodes |
| `float` | `DOUBLE` | Widen to double — ignore float/double size difference |
| `byte`, `short` | `INT` | Standard JVM promotion to int |
| `char` | `STRING` | Map to string (single character) — needs careful handling in compile mode like float→double |
| `CharSequence` | `STRING` | Natural string mapping |

**New `LONG` type requires:**
- Add `LONG` to `SystemDataType` enum (parent: `NUMERIC`, like `INT`)
- Add `LONG_ARRAY` array variant
- Add to `TypeMapper`: JVM descriptor `J`, opcodes `LLOAD`/`LSTORE`/`LRETURN`, `LCMP` for comparisons
- Add `LiteralCompiler` support for long literals
- Widening conversions: `INT` → `LONG` (implicit), `LONG` → `DOUBLE` (implicit)

**Compile-mode widening for `float→double` and `char→string`:**
- When a Java method parameter is `float`, the compiler must emit `F2D` before passing a JCP `DOUBLE` value
- When a Java method parameter is `char`, the compiler must emit `INVOKEVIRTUAL String.charAt(0)` or equivalent to extract the char from the JCP `STRING` value
- These conversions happen at the call site in the compiled bytecode, transparent to the DSL user

### Constructor Bug (line 235)

The fork analysis confirmed no `arguments` vs `parameters` bug exists in current code. The `registerConstructors` method correctly uses `constructor.getParameters()`. The issue description is stale — likely fixed in a prior commit. We will verify in tests and note as already-resolved.

## Implementation Phases

### Phase 1: Add `LONG` Basic Type and Expand Type Mappings (D6)

- [ ] Add `LONG` to `SystemDataType` enum (parent: `NUMERIC`) and `LONG_ARRAY` array variant
- [ ] Add `LONG` to `TypeMapper`: descriptor `J`, opcodes `LLOAD`/`LSTORE`/`LRETURN`
- [ ] Add widening conversions: `INT` → `LONG` (I2L), `LONG` → `DOUBLE` (L2D)
- [ ] In `ClassConverter.getDataType()`: map `long→LONG`, `float→DOUBLE`, `byte/short→INT`, `char/CharSequence→STRING`
- [ ] In `CompileModeClassConverter.mapJavaTypeToDataType(Class<?>)`: same mappings
- [ ] Add compile-mode widening bridge: `float` params emit `D2F` at call site, `char` params emit `String.charAt(0)`
- [ ] Add unit tests: register a class with `long`/`float`/`char`/`byte`/`short` parameters — confirm correct type resolution, no NPE

### Phase 2: Create `JcpModuleRegistry` Facade

- [ ] Create `core/src/main/java/com/elminster/jcp/module/JcpModuleRegistry.java`
- [ ] Store `List<RegistrationEntry>` (class + module) as instance state
- [ ] Implement `registerClass(Class<?> clazz, String module)` — adds entry
- [ ] Implement `initEval(EvalContext ctx)` — iterates entries, calls `ClassConverter.registerClass(entry.clazz, ctx, entry.module)`
- [ ] Implement `initCompile(CompileContext ctx)` — iterates entries, calls `CompileModeClassConverter.registerClass(entry.clazz, ctx, entry.module)`
- [ ] Create singleton `DEFAULT` instance pre-loaded with `BaseModuleRegister.classToRegister()` as "base" module
- [ ] Support additional `registerClass()` calls for "external" module

### Phase 3: Wire Into Existing Init Paths

- [ ] Refactor `RootEvalContext.registerSystemFunctions()` to call `JcpModuleRegistry.DEFAULT.initEval(this)`
- [ ] Refactor `BytecodeGenerator.registerModuleTypes(ctx)` to call `JcpModuleRegistry.DEFAULT.initCompile(ctx)` (plus replay any `additionalExternalClasses`)
- [ ] Preserve `BytecodeGenerator.additionalExternalClasses` support — these are registered via `JcpModuleRegistry` before compilation
- [ ] Verify all existing tests pass with zero behavioral change

### Phase 4: JAR Import Support

- [ ] Implement `registerJar(Path jarPath, String module)` in `JcpModuleRegistry`
- [ ] Create `URLClassLoader` for JAR, scan entries for public classes
- [ ] Store JAR classloaders; wire into `MultiClassLoader.registerClassLoader()` during `initCompile()`
- [ ] Add test: create a small test JAR with a public class, register via `registerJar()`, use from both eval and compile modes

### Phase 5: Tests & Coverage

- [ ] Test: single class registration via unified API — eval mode works
- [ ] Test: single class registration via unified API — compile mode works
- [ ] Test: JAR import via unified API — both modes work
- [ ] Test: class with `long`/`float`/`char`/`byte`/`short` params — correct type mapping, no NPE
- [ ] Test: `LONG` type works end-to-end in eval and compile modes (arithmetic, assignment, comparison)
- [ ] Test: `float→double` and `char→string` widening at Java call boundaries
- [ ] Test: verify constructor bug is not present (document as already fixed)
- [ ] Verify JaCoCo coverage ≥ 80% instruction and branch

## Acceptance Criteria

### Functional Requirements

- [ ] `JcpModuleRegistry.registerClass(Class<?>, String)` registers for both modes
- [ ] `JcpModuleRegistry.registerJar(Path, String)` scans and registers all public classes
- [ ] Eval mode: registered classes work via `EvalVisitor`
- [ ] Compile mode: registered classes work via `JcpCompiler.compileAndLoad()`
- [ ] `ClassConverter` NPE on unsupported param types is fixed with proper type mappings
- [ ] New `LONG` basic type works in both eval and compile modes
- [ ] `float→DOUBLE`, `char→STRING`, `byte/short→INT` widening works at Java call boundaries
- [ ] Existing module functions (Stdio, Math, Strings, etc.) work unchanged

### Non-Functional Requirements

- [ ] JaCoCo ≥ 80% instruction and ≥ 80% branch coverage
- [ ] No breaking changes to existing public APIs (both converters remain usable directly)
- [ ] Registration order preserved (e.g., SortKey before Arrays dependency)

## Dependencies

- `MultiClassLoader.registerClassLoader()` already exists — no new work needed there
- `BaseModuleRegister.classToRegister()` already provides ordered class list
- `ExternalClassType` shared between both modes — no changes needed

## Known Learnings Applied

- `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md` — Compile mode requires BOTH `addDataType()` AND classloader visibility. `JcpModuleRegistry.initCompile()` will do both: call `CompileModeClassConverter.registerClass()` (which calls `ctx.addDataType()`) AND wire JAR classloaders into `MultiClassLoader`.

- `docs/solutions/runtime-errors/noclassdeffounderror-struct-classes-not-loaded.md` — Context instance matters. `JcpModuleRegistry` passes the caller-provided context through, never creating its own local copy.

- `docs/solutions/runtime-errors/50-type-bridge-isolation-report.md` (D5/D6) — `getDataType()` NPE on unmapped types. Fixed in Phase 1 by adding proper type mappings: `LONG` (new), `float→DOUBLE`, `char→STRING`, `byte/short→INT`.

- `docs/solutions/logic-errors/struct-type-resolution-in-variable-declarations.md` — Type resolution must check context for custom types. The unified registry populates context before any compilation step, ensuring types are available.

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking existing module init order (SortKey before Arrays) | M | H | `JcpModuleRegistry` preserves insertion order of `BaseModuleRegister.classToRegister()` list |
| JAR scanning picks up non-public or inner classes | L | M | Filter to `Modifier.isPublic(clazz)` and `!clazz.isMemberClass()` unless static |
| `LONG` type adds complexity across TypeMapper, evaluators, and compilers | M | M | Follow exact pattern of existing `INT`/`DOUBLE` types — same opcodes, same evaluator structure. Scope to basic arithmetic/comparison only |
| `char→STRING` widening loses information (multi-char strings passed to char param) | L | M | Emit runtime check or truncate to first char at call site. Document as known limitation |
| `MultiClassLoader` parent chain doesn't see JAR classes at compile time | M | H | `initCompile()` explicitly calls `registerClassLoader()` on `MultiClassLoader` before class loading |
| Singleton `DEFAULT` instance causes test isolation issues | L | L | Provide `JcpModuleRegistry()` constructor for test-local instances; `DEFAULT` only used in production init paths |
