---
title: "Issue #50 — JCP↔Java type bridge isolation report"
date: 2026-08-06
issue: 50
pr: 1 of 4
status: isolation-complete
---

# Issue #50 — JCP↔Java type bridge isolation report

**PR 1 of 4.** This document records the isolation findings for every defect identified
in issue #50. Each entry states whether it was **executably reproduced** (a red test
exists that fires the real failure) or **code-verified only** (the failure mode was
confirmed by reading the code but cannot be triggered by an executable test in PR 1).

The tests live in:
`core/src/test/java/com/elminster/jcp/compile/bridge/TypeBridgeReproductionTest.java`

---

## Defect inventory

### D1 — `resolveModuleClassName` hardcoded to `base` module (structural block)

| Field | Value |
|-------|-------|
| **Status** | Code-verified only (structural block) |
| **Code path** | `FunCallCompiler.resolveModuleClassName` |
| **Description** | The `module::Type.method()` shorthand in `FunCallCompiler` only resolves class names for the `"base"` module prefix. Any user-provided JAR registered under a different module name cannot be reached through this path. |
| **Why not reproduced executably** | `resolveModuleClassName` accepts only `"base"`; calling it with a user module simply falls through with no bytecode emitted — it cannot be driven to a runtime failure from a test without patching the hardcoded string. |
| **Fix ownership** | PR 2 / PR 3 |
| **Test** | `d1_structural_resolveModuleClassName_baseOnlyBlock_documentedCodeVerified` (passes trivially; records the constraint) |

---

### D2 — `MultiClassLoader` parent chain cannot see dynamically-registered JARs

| Field | Value |
|-------|-------|
| **Status** | **Executably reproduced** |
| **Code path** | `MultiClassLoader` constructor → `StaticMethodCallCompiler.compileExternalClassCall` → INVOKESTATIC → runtime class resolution |
| **Description** | `MultiClassLoader(super(MultiClassLoader.class.getClassLoader()))` — its parent is the JCP app classloader. When a user registers a JAR that is NOT on the app classpath (via `BytecodeGenerator.registerExternalClass`), the generated INVOKESTATIC references the foreign class's internal name. At runtime, `MultiClassLoader` delegates to its parent, which cannot find the class → `NoClassDefFoundError`. |
| **Observed failure** | `java.lang.NoClassDefFoundError: bridge/ForeignHelper` at `evaluate()` invocation |
| **Test** | `d2_isolatedUrlClassLoader_noClassDefFoundErrorAtInvoke` — builds a foreign class with ASM into a temp JAR, loads via isolated URLClassLoader, compiles a JCP call, loads via `MultiClassLoader` → confirms `Throwable` at invoke |
| **Fix ownership** | PR 4 — add `registerClassLoader(ClassLoader)` hook to `MultiClassLoader`; document the contract in `JcpCompiler` Javadoc |

---

### D3 — `FunCallCompiler` builds descriptor from lossy JCP arg types

| Field | Value |
|-------|-------|
| **Status** | Code-verified only |
| **Code path** | `FunCallCompiler.compileModuleFunctionCall` — descriptor constructed from `TypeMapper.toDescriptor(argType)` using JCP types, not the real Java method descriptor |
| **Description** | When the module-shorthand path (`module::Type.method`) is used, `FunCallCompiler` builds the INVOKESTATIC descriptor from JCP arg types (which lose precision via D5). The emitted descriptor may not match the real Java method signature. |
| **Why not reproduced executably** | D1 (structural block) prevents `compileModuleFunctionCall` from being reached for user JARs in PR 1. The defect exists in the code but cannot be triggered. |
| **Fix ownership** | PR 2 — use `method.getDescriptor()` from `ExternalMethodDef` (same approach as `StaticMethodCallCompiler`) |

---

### D4 — `FunCallCompiler` does not box primitives for `ANY`-typed params

| Field | Value |
|-------|-------|
| **Status** | Code-verified only (same D1 structural block as D3) |
| **Code path** | `FunCallCompiler.compileModuleFunctionCall` — no `boxPrimitive()` call for `ANY` params |
| **Description** | Unlike `StaticMethodCallCompiler` (line ~142), `FunCallCompiler` does not call `boxPrimitive` when a parameter type is `ANY`. Primitive values (INT, BOOLEAN) would be left on the stack without boxing when the method expects `Object`. |
| **Fix ownership** | PR 3 |

---

### D4′ — `StaticMethodCallCompiler` boxes INT for `ANY` param but descriptor expects primitive `char`

| Field | Value |
|-------|-------|
| **Status** | **Executably reproduced** |
| **Code path** | `StaticMethodCallCompiler.compileExternalClassCall` lines ~140–144 |
| **Description** | `mapJavaTypeToDataType(char.class)` returns `ANY` (D5). On the registered-JCP-type path, the INVOKESTATIC descriptor is exact (from `Type.getMethodDescriptor(realMethod)` — e.g. `(CI)Ljava/lang/String;` for `repeat(char,int)`). The compiler sees `paramType==ANY` and calls `boxPrimitive(INT arg)` — pushing an `Integer`. The descriptor demands primitive `C`. Stack/descriptor mismatch → `VerifyError` at class-load time. Same conflict applies to `CharSequence` params. |
| **Observed failure** | `java.lang.VerifyError` (or `LinkageError`) at `loadClass` or `invoke` |
| **Root cause** | D5 — `char`/`CharSequence` collapse to `ANY` in `mapJavaTypeToDataType` |
| **Tests** | `d4Prime_repeat_charInt_boxingVsExactDescriptor`, `d4Prime_countMatches_charSequenceChar_boxingVsExactDescriptor` |
| **Fix ownership** | PR 2 — fix D5 first (add `char`/`CharSequence`/`long`/`float`/`byte`/`short` mappings); then D4′ resolves automatically |

---

### D5 — `mapJavaTypeToDataType` collapses `char`, `CharSequence`, `long`, `float`, `byte`, `short` to `ANY`

| Field | Value |
|-------|-------|
| **Status** | **Executably reproduced (characterization)** |
| **Code path** | `CompileModeClassConverter.mapJavaTypeToDataType` |
| **Description** | The method has no case for `char`, `CharSequence`, `long`, `float`, `byte`, or `short` — all fall through to `return SystemDataType.ANY`. This is the root cause of D4′ (boxing-vs-exact-descriptor) and D5′ (long return opcode mismatch). |
| **Tests** | `CharacterizationTests` — six tests pin the current `ANY`-collapse behavior, annotated `// CHARACTERIZATION: PR 2 updates this` |
| **Fix ownership** | PR 2 |

---

### D5′ — `long`-returning methods: return opcode mismatch

| Field | Value |
|-------|-------|
| **Status** | **Executably reproduced** |
| **Code path** | `CompileModeClassConverter.mapJavaTypeToDataType(long.class)` → `ANY`; `TypeMapper.getReturnOpcode(ANY)` → `ARETURN`; actual descriptor ends in `J` (`LRETURN` needed) |
| **Description** | A method returning `long` has its return type recorded as `ANY`. The `compileWithReturn` path emits `ARETURN` but the stack holds a `long` (two slots). JVM verifier rejects this. |
| **Observed failure** | `java.lang.VerifyError` or `java.lang.reflect.InvocationTargetException` wrapping `VerifyError` |
| **Test** | `d5_longReturnType_returnDescriptorMismatch` using `LongReturningFixture.longValue()` |
| **Fix ownership** | PR 2 (same fix as D5) |

---

### D6 — Eval-mode `ClassConverter` NPE on unknown Java param types

| Field | Value |
|-------|-------|
| **Status** | **Executably reproduced** |
| **Code path** | `ClassConverter.getDataType(Class, EvalContext, module)` → `DataTypeUtils.getDataType(simpleName, ctx)` returns `null` → `ParameterDef(name, null)` → `FunctionUtils.generateFunctionFullName` NPE on `paramDef.getDataType().getName()` |
| **Description** | When `ClassConverter.registerClass` is called on a class with `CharSequence`, `char`, `long`, `float`, `byte`, or `short` params, `DataTypeUtils.getDataType` returns `null` for the unknown type name. This null is stored in `ParameterDef.dataType`. The next step — generating the function's full name — calls `.getName()` on the null datatype and throws `NullPointerException`. Registration fails entirely, including for unrelated `String`-param methods on the same class. |
| **Observed failure** | `java.lang.NullPointerException: Cannot invoke "DataType.getName()" because the return value of "ParameterDef.getDataType()" is null` |
| **Test** | `d6Adjacent_classConverter_registerClass_npeForUnknownParamType` — calls `ClassConverter.registerClass(StringUtils.class, ctx, "user")` and asserts `NullPointerException` |
| **Fix ownership** | PR 2 — `ClassConverter.getDataType` should fall back to `DataTypeUtils.getDataTypeAndCreateOnMissing` (or map primitives explicitly) instead of returning `null` |

---

## Summary table

| Defect | Status | Code path | Fix PR |
|--------|--------|-----------|--------|
| D1 — `resolveModuleClassName` base-only block | Code-verified | `FunCallCompiler` | PR 2/3 |
| D2 — `MultiClassLoader` parent chain gap | **Reproduced** | `MultiClassLoader` | PR 4 |
| D3 — `FunCallCompiler` lossy descriptor | Code-verified | `FunCallCompiler` | PR 2 |
| D4 — `FunCallCompiler` no boxing | Code-verified | `FunCallCompiler` | PR 3 |
| D4′ — `StaticMethodCallCompiler` boxing-vs-char-descriptor | **Reproduced** | `StaticMethodCallCompiler` lines ~142–144 | PR 2 |
| D5 — `mapJavaTypeToDataType` ANY-collapse | **Reproduced (characterization)** | `CompileModeClassConverter` | PR 2 |
| D5′ — `long` return opcode mismatch | **Reproduced** | `CompileModeClassConverter` + `TypeMapper` | PR 2 |
| D6 — `ClassConverter` NPE on unknown param types | **Reproduced** | `ClassConverter` | PR 2 |

5 defects executably reproduced. 3 code-verified only (blocked by D1 structural).

## What PRs 2–4 need to do

**PR 2** (core type-mapping fixes):
- Extend `CompileModeClassConverter.mapJavaTypeToDataType` with mappings for `char`, `long`, `float`, `byte`, `short`, `CharSequence` (and their boxed equivalents).
- Fix `ClassConverter.getDataType` to use `getDataTypeAndCreateOnMissing` or explicit primitive mapping instead of returning `null`.
- After D5 is fixed, D4′, D5′, and D6 will all resolve; the characterization tests become green and must be updated to assert the correct types.
- Also fix `FunCallCompiler` D3: use `ExternalMethodDef.getDescriptor()` instead of building from JCP arg types.

**PR 3** (boxing in `FunCallCompiler`):
- Add `boxPrimitive` calls for `ANY`-typed params, mirroring `StaticMethodCallCompiler` lines ~142–144.
- Remove the D1 structural block in `resolveModuleClassName`.

**PR 4** (classloader isolation):
- Add `MultiClassLoader.registerClassLoader(ClassLoader)` hook that adds the given loader to the parent chain.
- Document the contract in `JcpCompiler` Javadoc: *"To call methods on classes from a dynamically-loaded JAR, register its classloader via `MultiClassLoader.registerClassLoader` before invoking `compileAndLoad`."*
- The D2 reproduction test in this PR will turn green once the hook is wired in.
