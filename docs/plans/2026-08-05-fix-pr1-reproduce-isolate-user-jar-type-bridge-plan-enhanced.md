---
title: "fix: PR 1 — Reproduce & isolate the user-JAR compile-mode type-bridge failure (Enhanced)"
type: fix
date: 2026-08-05
issue: 50
pr: 1 of 4
review_date: 2026-08-05
original_plan: docs/plans/2026-08-05-fix-pr1-reproduce-isolate-user-jar-type-bridge-plan.md
---

# fix: PR 1 — Reproduce & isolate the user-JAR compile-mode type-bridge failure (Enhanced)

> **Note:** Enhanced after a code-level review (2026-08-05) that found the original plan
> attributed the JCP-type invocation to the wrong compiler and therefore predicted the wrong
> failure modes. This version re-maps the defect model to the code path actually exercised.

## Overview

**PR 1 of 4** for issue #50 (JCP↔Java type bridge). Its sole job is to **reproduce and
isolate** the real bug hit when calling a user-provided JAR in compile mode: a mix of
classloader errors and bad round-trip values, root cause not yet isolated.

**Tests-only. Fixes nothing.** It delivers a red reproduction suite + a written isolation
report. The reproduction uses a **real library** (`commons-lang3` `StringUtils`, already a
compile dep) invoked **through a registered JCP type** — the exact path a user surfaces a JAR
to JCP — in both same-loader and isolated-classloader scenarios. The isolation report is the
acceptance oracle for PRs 2–4.

## Technical Approach

### Two distinct code paths — the review's key correction

The defect catalogue (D1–D6) was written against `FunCallCompiler.compileModuleFunctionCall`.
**But a registered JCP type does not use that compiler.** Verified against the code:

| JCP construct | Compiler | Descriptor source | Boxing |
|---------------|----------|-------------------|--------|
| `StaticMethodCallExpression` on a registered `ExternalClassType` | `StaticMethodCallCompiler.compileExternalClassCall` (`:120`) | `method.getDescriptor()` = `Type.getMethodDescriptor(realMethod)` — **exact** (`CompileModeClassConverter.java:96`) | **Yes**, for `ANY` params (`:142–144`) |
| `module::Type.method(...)` shorthand | `FunCallCompiler.compileModuleFunctionCall` | Built from **JCP arg types** (lossy) — D3 | **No** — D4 |

**Consequences for PR 1:**

- The registered-JCP-type path (what this PR exercises) is **descriptor-correct** — the
  emitted `INVOKESTATIC` descriptor matches the real Java method regardless of the lossy
  `mapJavaTypeToDataType`. **D3-as-catalogued and D4-as-catalogued do NOT fire on this path.**
- The **module-shorthand path** *does* carry D3/D4, but `resolveModuleClassName` is hardcoded
  to `com.elminster.jcp.module.base.*` and rejects non-`base` types, so it **cannot be reached
  for a user JAR today**. D3/D4 there are **verified by code reading** and handed to PR 2/3;
  they are not executably reproduced in PR 1.

### The real reproducible defect on the JCP-type path: D4′ (boxing vs exact primitive descriptor)

`mapJavaTypeToDataType` collapses `char`/`CharSequence`/`long`/… to `ANY` (D5). On the
registered-type path this produces a **conflict**, because the descriptor is exact:

- `StringUtils.repeat(char, int) → String`: reflected descriptor `(CI)Ljava/lang/String;`.
  JCP records the first param as `ANY` (char→ANY, D5). At `StaticMethodCallCompiler:142`,
  `paramType==ANY` ⇒ `boxPrimitive(argType)` pushes an `Integer`. The `INVOKESTATIC`
  descriptor demands primitive `char` (`C`). **Stack/descriptor mismatch → VerifyError.**
- `StringUtils.countMatches(CharSequence, char) → int`: same conflict on the `char` arg; the
  `CharSequence` arg maps to `ANY` too, and overload resolution runs on **lossy `argTypes`**,
  risking wrong-overload selection before bytecode is even emitted.

This is a genuine, executable failure through a real JCP-type call — call it **D4′** to
distinguish it from `FunCallCompiler`'s D4. It is the concrete manifestation the user likely
hit, and it is driven by D5.

### Green control

`StringUtils.capitalize(String) → String` is descriptor-exact and needs no boxing → this
**must pass** on the same-loader scenario. It is an explicit green control: a failure there
means the harness is broken, not the product.

### Fixture

Primary: `commons-lang3` `StringUtils` (`capitalize`, `length(CharSequence)`,
`countMatches(CharSequence,char)`, `repeat(char,int)`). A **minimal synthetic fixture** only
for shapes Commons lacks (e.g. a `long`-returning method for return-side `ANY` handling, a
constructor for the instance path).

### Loader scenarios

1. **Same-loader** (Commons on classpath): reproduces **D4′/D5** and the green control.
   `getInternalName()` = `javaClass.getName().replace('.','/')`, resolvable by `MultiClassLoader`.
2. **Isolated `URLClassLoader`** (JAR located via
   `StringUtils.class.getProtectionDomain().getCodeSource().getLocation()`, parent invisible to
   `MultiClassLoader`'s parent): reproduces **D1/D2** — the generated class references the
   Commons internal name but cannot resolve it at runtime → `NoClassDefFoundError`. Sanity
   assertion: isolated `Class != ` classpath `Class`.

### Eval-mode probe

Register `StringUtils` via eval-mode `ClassConverter` and call `repeat`/`countMatches`
through a JCP type in the interpreter, to confirm whether `char`/`CharSequence`
marshal/unmarshal round-trips correctly (the "bad round-trip value" symptom) — covering D6-adjacent behavior.

## Implementation Phases

### Phase 1: Harness scaffolding

**Deliverables:**
- [ ] `core/src/test/java/com/elminster/jcp/compile/bridge/` package
- [ ] Helper: register a `Class` via `CompileModeClassConverter.registerClass` and build a
      `StaticMethodCallExpression` against it; compile via `MultiClassLoader` and invoke
- [ ] Isolating `URLClassLoader` helper (JAR URL from `getCodeSource()`, explicit non-JCP parent)
- [ ] Minimal synthetic fixture for `long`-return / `Object`-param / constructor shapes

**Acceptance Criteria:**
- [ ] Sanity assertion passes: isolated `StringUtils.class != ` classpath `StringUtils.class`

### Phase 2: Same-loader reproduction via JCP type (green control, D4′, D5)

**Deliverables:**
- [ ] **Green control**: `capitalize(String)` compiles, loads, and returns the correct value
- [ ] `repeat(char,int)` probe → capture **D4′** VerifyError (boxed `Integer` vs `C` descriptor)
- [ ] `countMatches(CharSequence,char)` probe → capture D4′ and/or overload-resolution mismatch
- [ ] Record exact exception type + message + originating compiler line per case

**Acceptance Criteria:**
- [ ] Green control is green; D4′ probes are red with the predicted exception verified against
      `StaticMethodCallCompiler` (not assumed)

### Phase 3: Isolated-loader reproduction via JCP type (D1, D2)

**Deliverables:**
- [ ] Register the isolated-loader `StringUtils`; compile a JCP-type call, load+invoke via
      `MultiClassLoader` → capture **D2** `NoClassDefFoundError` at runtime
- [ ] Document the `resolveModuleClassName` hardcoded-`base` structural block (D1-structural),
      and note `FunCallCompiler` D3/D4 as **code-verified, unreachable in PR 1**

**Acceptance Criteria:**
- [ ] D2 reproduces at runtime invoke, not compile time; exact exception recorded

### Phase 4: Eval-mode probe + D5 characterization + diagnosis writeup

**Deliverables:**
- [ ] Eval-mode: register `StringUtils` via `ClassConverter`, call `repeat`/`countMatches`
      through a JCP type; record round-trip correctness
- [ ] Characterization tests: `mapJavaTypeToDataType(char/CharSequence/long/float/byte/short)`
      returns `ANY` (annotated `// CHARACTERIZATION: current buggy ANY-collapse; PR 2 updates`)
- [ ] Isolation report → `docs/solutions/runtime-errors/` (defect × path × loader-scenario ×
      observed failure + which are code-verified-only; fix ownership across PR 2/3/4)
- [ ] Update issue #50 with the isolation result

## Acceptance Criteria

### Functional Requirements

- [ ] Reproduction invokes the 3rd-party method **through a registered JCP type**
      (`StaticMethodCallExpression` on an `ExternalClassType`), NOT direct Java reflection
- [ ] Uses real `commons-lang3` `StringUtils`; synthetic fixture only for gaps
- [ ] Includes an explicit **green control** (`capitalize(String)`) that must pass
- [ ] `repeat(char,int)` / `countMatches(CharSequence,char)` probes reproduce **D4′**
      (boxing-vs-exact-descriptor VerifyError) with the exception **verified against
      `StaticMethodCallCompiler`**, not assumed from the D-catalogue
- [ ] Isolated `URLClassLoader` scenario reproduces **D2** at runtime; sanity assertion for a
      genuine boundary
- [ ] Eval-mode round-trip probe for `char`/`CharSequence` exists
- [ ] Characterization tests pin current `mapJavaTypeToDataType` behavior for gap types
- [ ] Isolation report distinguishes **executably reproduced** vs **code-verified-only**
      defects, and records the code path each belongs to
- [ ] Report written to `docs/solutions/` and linked from issue #50

### Non-Functional Requirements

- [ ] Tests-only — no production code changed in PR 1
- [ ] Red tests annotated with the defect id + the compiler line they hit
- [ ] Runs within `mvn test -pl core`; no new dependency
- [ ] JaCoCo core 80/80 thresholds still pass (`mvn verify -pl core`)
- [ ] Deterministic: JAR URL from `getCodeSource()`, explicit loader parent, assert on
      exception *types* not fragile stack text

## Dependencies

- Brainstorm doc `docs/brainstorms/2026-08-05-jcp-java-type-bridge-classloader-brainstorm.md`
- `commons-lang3:3.18.0` — already a direct compile dep of `core` (no new dependency)
- Test infra: `AbstractCompileTest`, `MultiClassLoader`, `CompileModeClassConverter`,
  `ClassConverter` (eval), `ExternalClassType`, `StaticMethodCallExpression`
- New feature branch off `master` (currently on `feat/45-...`)

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| **Misattributing the failing path → wrong isolation table** (the flaw this review caught) | M | H | For each red probe, verify the predicted exception against the *actual* compiler it hits (`StaticMethodCallCompiler`), not the D-catalogue, before asserting |
| D4′ VerifyError doesn't materialize because ASM/JVM verifier is lenient about the specific op | L | M | Assert on load-time `VerifyError`/`LinkageError` broadly; if absent, capture the *actual* observed behavior (e.g. silent wrong overload) as the finding |
| Isolated loader shares JCP's parent → boundary not real | M | H | Assert isolated `Class != ` classpath `Class`; fail loudly |
| Locating the commons JAR is environment-dependent | M | M | Derive URL from `getProtectionDomain().getCodeSource().getLocation()`, never a hardcoded m2 path |
| `FunCallCompiler` D3/D4 treated as "not a bug" because PR 1 can't reproduce them | M | M | Report explicitly marks them **code-verified, unreachable-in-PR-1**, assigned to PR 2/3 — not "not fired" |
| Characterization tests mistaken for desired behavior | M | M | Annotate `// CHARACTERIZATION: PR 2 updates this` |
| Coverage dips | L | M | Tests-only; run `mvn verify -pl core` before pushing |

## Testing Strategy

- **Unit** (fast, no execution): `mapJavaTypeToDataType` characterization for gap types.
- **Compile+load+invoke** (same-loader): green control + D4′ probes via `MultiClassLoader`.
- **Compile+load+invoke** (isolated loader): D2 via `URLClassLoader`.
- **Eval**: JCP-type call through `ClassConverter`-registered `StringUtils` for round-trip.
- Assertions target exception **types** and originating compiler line, not stack text.

## Success Metrics

- A committed isolation report that, for every defect D1–D6, states: reproduced-executably /
  code-verified-only / not-applicable, **with the code path named** and the exception recorded.
- PRs 2/3/4 can each point to specific red tests (or code-verified findings) they will resolve.

## Changes from Original Plan

1. **Corrected the code-path attribution** — the JCP-type invocation uses
   `StaticMethodCallCompiler.compileExternalClassCall`, not `FunCallCompiler`. D3/D4 as
   catalogued do not fire on this path.
2. **Introduced D4′** — the real reproducible defect on this path: `ANY`-driven boxing
   conflicting with an exact primitive descriptor (`char`) → VerifyError, rooted in D5.
3. **Added a green control** (`capitalize`) to distinguish harness failure from product defect.
4. **Reclassified `FunCallCompiler` D3/D4** as code-verified-but-unreachable in PR 1, assigned
   to PR 2/3, so the isolation table isn't misread as "no bug".
5. **Added an eval-mode round-trip probe** for the char/CharSequence "bad value" symptom.
6. **Added the top risk** (wrong isolation table) with a concrete per-probe verification step.

## References

- Original plan: `docs/plans/2026-08-05-fix-pr1-reproduce-isolate-user-jar-type-bridge-plan.md`
- Review: `docs/plans/2026-08-05-fix-pr1-reproduce-isolate-user-jar-type-bridge-plan-review.md`
- Issue: https://github.com/elminsterjimmy/jcp/issues/50
