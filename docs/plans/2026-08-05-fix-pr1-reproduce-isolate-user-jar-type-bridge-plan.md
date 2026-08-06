---
title: "fix: PR 1 — Reproduce & isolate the user-JAR compile-mode type-bridge failure"
type: fix
date: 2026-08-05
issue: 50
pr: 1 of 4
---

# fix: PR 1 — Reproduce & isolate the user-JAR compile-mode type-bridge failure

## Overview

This is **PR 1 of 4** for issue #50 (JCP↔Java type bridge). Its sole job is to
**reproduce and isolate** the real bug hit when calling a user-provided JAR in compile
mode. The user hit a mix of classloader errors (ClassNotFoundException/ClassCastException)
**and** bad round-trip values, via **both** call paths (static-module and
constructor/instance), and the **root cause is not yet isolated** between the five
candidate defects (D1–D5) found in the brainstorm audit.

**This PR fixes nothing.** It delivers a **failing (red) reproduction test suite** plus a
written diagnosis pinning exactly which defect(s) fire. The reproduction uses a **real
third-party library** (`commons-lang3` `StringUtils`, already a compile dep) invoked
**through a registered JCP type** — the exact path a user surfaces a JAR to JCP — exercised in
both same-loader and isolated-classloader scenarios. That diagnosis becomes the acceptance
oracle for PRs 2–4 — each later PR flips specific red tests green.

Deliberately scoped as tests-only so the reproduction is reviewable in isolation and the
later fixes are provably tied to a real failure rather than speculation.

## Technical Approach

### Fixture: a real third-party JAR (Apache Commons Lang3) + a minimal helper

Per the user's direction, the reproduction uses a **real third-party library** rather than a
synthetic stand-in. `org.apache.commons:commons-lang3:3.18.0` is already a **direct
compile-scope dependency** of the core module, and `StringUtils` exposes signatures that hit
the entire defect matrix cleanly:

| Commons `StringUtils` method | Signature | Probes |
|------------------------------|-----------|--------|
| `capitalize(String)` | `String → String` | D3 baseline — clean String↔String round-trip |
| `length(CharSequence)` | `CharSequence → int` | D3 — `CharSequence` param maps to `ANY` |
| `countMatches(CharSequence, char)` | `(CharSequence, char) → int` | D3 (`ANY`) + D4/D5 (`char` param) |
| `repeat(char, int)` | `(char, int) → String` | D4/D5 — `char` primitive param, boxing |

A **minimal synthetic fixture class** is added *only* to cover method shapes Commons does not
provide (e.g. a method taking `Object` and returning `long`, or a constructor for the
external-class instance path). It is the exception, not the primary vehicle.

### Invocation is through a registered JCP type — the real bridge, end to end

**Critical constraint (user direction):** the test must invoke the 3rd-party method **through
a JCP type**, not via direct Java reflection. That means:

1. Register `org.apache.commons.lang3.StringUtils` as a JCP `ExternalClassType` via
   `CompileModeClassConverter.registerClass(StringUtils.class, ctx, ...)` (compile mode) and
   the eval-mode `ClassConverter` equivalent — exactly the mechanism a user would use to
   surface a JAR to JCP.
2. Author a JCP program (AST) that calls the method **on that registered type** — e.g. a
   `StaticMethodCallExpression(Identifier.fromName("StringUtils"), "capitalize", <arg>)` — so
   the call flows the full path: type registration → `resolveType` → descriptor construction
   → `INVOKESTATIC` against the real Commons class → runtime invoke.
3. Compile via the `MultiClassLoader` entry point (`compileAndLoad` / `compileWithReturn` +
   `MultiClassLoader`) and reflectively invoke `evaluate()`/`main()` to observe the result or
   failure.

This is where D3 (descriptor built from JCP types, not the real `CharSequence`/`char`
signature), D4 (no boxing on the static call), and D5 (`char`/`CharSequence` → `ANY`) surface
as real `NoSuchMethodError`/`VerifyError` at load/invoke — through the same code the user hit.

**Static-module path caveat.** `FunCallCompiler.resolveModuleClassName`
(`FunCallCompiler.java:320+`) is **hardcoded** to
`com.elminster.jcp.module.base.<lowercase-type>.<Type>` and throws for any non-`base` module,
so the `module::Type.method` *shorthand* path cannot name `StringUtils` today. The invocation
therefore goes through the **external-class / registered-type path** (`ExternalClassType` +
`StaticMethodCallExpression` / `MethodCallExpression`). The hardcoded-`base` limitation is
recorded as a first-class **structural D1 finding** (informs PR 3), not worked around.

### Two loader scenarios — same-loader AND isolated (full defect coverage)

Per user direction, Commons is exercised **both ways**:

1. **Same-loader (on classpath).** Commons is on the compile/test classpath, so `Class.forName`
   resolves it and `MultiClassLoader` (parent = `MultiClassLoader.class.getClassLoader()`,
   `MultiClassLoader.java:15`) can see it. This reproduces **D3/D4/D5** (descriptor & boxing)
   faithfully via a JCP-type invocation, with no loader-visibility noise.

2. **Isolated loader.** Load the `commons-lang3` JAR (resolved from the Maven-provided
   classpath entry) through a separate `URLClassLoader` whose parent is the platform/bootstrap
   loader — **invisible** to `MultiClassLoader`'s parent chain — then register *that* `Class`
   and compile a JCP-type invocation against it. This reproduces **D1/D2**
   (loader visibility: `ClassNotFoundException` at compile from the un-parameterized
   `Class.forName`, or `NoClassDefFoundError` at runtime because the generated class can't see
   Commons). A sanity assertion confirms the isolated `Class` object `!=` the classpath one.

### Mapping defects to observable assertions

Each candidate defect gets at least one targeted test that either reproduces it or proves it
does **not** fire, so PR 1 outputs a definitive isolation table. Every D1–D4 probe invokes a
**registered JCP type mapping to a real Commons class** (or the minimal fixture):

| Defect | Reproduction probe (JCP-type invocation) | Expected observable if it fires |
|--------|-------------------------------------------|----------------------------------|
| D1 (`Class.forName` no loader) | Register a Commons class loaded via the **isolated** `URLClassLoader`; compile a JCP-type call to it | `ClassNotFoundException` at compile time from `resolveModuleClassName`/registration |
| D2 (`MultiClassLoader` parent) | Register the isolated-loader Commons `Class`, compile a JCP-type call, load+invoke via `MultiClassLoader` | `NoClassDefFoundError`/`ClassNotFoundException` at runtime invoke |
| D3 (descriptor from JCP types) | JCP-type call to `StringUtils.length(CharSequence)` / `countMatches(CharSequence,char)` — params map to `ANY` | `NoSuchMethodError` / `VerifyError` at load or invoke |
| D4 (no boxing on static call) | JCP-type call to `StringUtils.repeat(char,int)` passing a JCP value where Java param is primitive/`Object` | `VerifyError` (wrong type on stack vs descriptor) |
| D5 (`mapJavaTypeToDataType` gaps) | Assert mapping for `char`/`CharSequence` (from the real signatures above) + `long/float/byte/short` | Returns `ANY` instead of a faithful type (unit-level, no execution) |

D5 is a pure unit assertion (fast, deterministic). D1–D4 require the JCP-type invocation
harness. The suite records, per loader scenario (same-loader vs isolated), **which** of D1–D4
actually reproduced.

### Reuse existing scaffolding

- Extend `AbstractCompileTest` (gives `compiler`, `uniqueClassName`, helpers).
- Follow `TypeClassLoadingTest` and `ExternalClassCompileTest` for AST construction,
  `MultiClassLoader` define/load, and reflective invoke.
- Reuse `CompileModeClassConverter.registerClass` / `ExternalClassType` registration as in
  `ExternalClassCompileTest`.

### Output: the isolation report

The suite is accompanied by a short diagnosis written to
`docs/solutions/runtime-errors/` capturing: which defects fired, on which path, exact
exception + stack, and the recommended fix ownership across PRs 2–4. This is the concrete
deliverable that unblocks planning the fixes.

## Implementation Phases

### Phase 1: Harness scaffolding

- [ ] Create `core/src/test/java/com/elminster/jcp/compile/bridge/` package for PR-1 tests
- [ ] Add a helper that registers a Java `Class` as a JCP `ExternalClassType`
      (`CompileModeClassConverter.registerClass`) and builds a JCP-type invocation AST
      (`StaticMethodCallExpression` / `MethodCallExpression`) against it
- [ ] Add an isolating `URLClassLoader` helper that loads the `commons-lang3` JAR (from the
      Maven classpath entry) with a parent invisible to `MultiClassLoader`'s parent chain
- [ ] Add a minimal synthetic fixture class **only** for shapes Commons lacks (e.g.
      `Object`-param / `long`-return method, constructor for the instance path)
- [ ] Sanity assertion: the isolated-loader `StringUtils.class` `!=` the classpath
      `StringUtils.class` — fail loudly if the boundary isn't real

### Phase 2: Same-loader reproduction via JCP type (D3, D4, D5)

- [ ] Register classpath `StringUtils` as a JCP type; compile+run a JCP-type call to
      `capitalize(String)` → establish the working baseline (or capture failure)
- [ ] JCP-type call to `length(CharSequence)` / `countMatches(CharSequence,char)` →
      capture D3 (`ANY` descriptor mismatch)
- [ ] JCP-type call to `repeat(char,int)` → capture D4 (missing boxing) / D5 (`char`→`ANY`)
- [ ] Record exact exception type + message + originating line per case

### Phase 3: Isolated-loader reproduction via JCP type (D1, D2)

- [ ] Register the **isolated-loader** `StringUtils`; compile a JCP-type call → capture D1
      (compile-time `ClassNotFoundException` from un-parameterized `Class.forName`)
- [ ] Where compile succeeds, load+invoke via `MultiClassLoader` → capture D2 runtime
      `NoClassDefFoundError`
- [ ] Document the `resolveModuleClassName` hardcoded-`base` structural block (D1-structural)
- [ ] Record exact failure per case

### Phase 4: Unit-level mapping gap (D5) + diagnosis writeup

- [ ] Characterization tests asserting `mapJavaTypeToDataType` current behavior for
      `char`, `CharSequence`, `long`, `float`, `byte`, `short` (+boxed/array) — documenting
      today's `ANY` collapse (annotated as CHARACTERIZATION, to be updated by PR 2)
- [ ] Write isolation report to `docs/solutions/runtime-errors/` (defect × loader-scenario ×
      observed failure, exceptions/stacks, fix ownership across PR 2/3/4)
- [ ] Update issue #50 with the isolation result

## Acceptance Criteria

### Functional Requirements

- [ ] Reproduction invokes the 3rd-party method **through a registered JCP type**
      (`ExternalClassType` + `StaticMethodCallExpression`/`MethodCallExpression`), NOT via
      direct Java reflection
- [ ] Uses the **real** `commons-lang3` `StringUtils` for the descriptor/boxing probes; a
      minimal synthetic fixture only for shapes Commons lacks
- [ ] Commons is exercised in **both** loader scenarios: same-loader (D3/D4/D5) and
      isolated `URLClassLoader` (D1/D2)
- [ ] The isolated scenario crosses a **genuine** boundary (asserted: isolated `Class` `!=`
      classpath `Class`)
- [ ] Each of D1–D5 has a dedicated probe that either reproduces the defect or proves it
      does not fire
- [ ] The suite produces a definitive **isolation table**: defect × loader-scenario ×
      observed failure (exception type + message)
- [ ] Characterization tests pin current `mapJavaTypeToDataType` behavior for the gap types
      (`char`, `CharSequence`, `long`, `float`, `byte`, `short`)
- [ ] Isolation report written to `docs/solutions/` and linked from issue #50

### Non-Functional Requirements

- [ ] Tests are **tests-only** — no production code changed in PR 1
- [ ] Red tests that represent bugs to fix are clearly annotated (comment + descriptive name)
      so PRs 2–4 know which to flip green; characterization tests that assert current
      behavior are annotated as such (they will be *updated*, not merely flipped, later)
- [ ] Suite runs within the existing `mvn test -pl core` flow; no new external deps
- [ ] JaCoCo core thresholds (80%/80%) still pass — PR 1 adds tests, does not lower coverage
- [ ] Deterministic: no reliance on filesystem JAR layout, ordering, or timing

## Dependencies

- Brainstorm doc `docs/brainstorms/2026-08-05-jcp-java-type-bridge-classloader-brainstorm.md`
  (defect catalog D1–D6)
- `org.apache.commons:commons-lang3:3.18.0` — already a direct compile-scope dependency of
  `core` (confirmed via `mvn dependency:tree`); no new dependency needed
- Existing test infra: `AbstractCompileTest`, `MultiClassLoader`, `CompileModeClassConverter`,
  `ExternalClassType` registration, `StaticMethodCallExpression`/`MethodCallExpression`
- New feature branch off `master` (currently on `feat/45-...`); do NOT build on the #45 branch

## Known Learnings Applied

- `docs/solutions/runtime-errors/noclassdeffounderror-struct-classes-not-loaded.md` —
  documents `MultiClassLoader`'s parent-delegation model, the class-load-order requirement
  (define all classes, then load), and that multiple compile entry points
  (`compile` vs `compileWithReturn` vs `compileAndLoad`) load classes differently. The
  harness must (a) define all generated + fixture classes before invoking, and (b) probe
  the specific entry point the bug used (`compileAndLoad` / `MultiClassLoader`), since
  `compileAndLoadWithReturn` uses a *separate* private `ByteArrayClassLoader` (open
  question #3 in the brainstorm).
- `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md` and
  `.../struct-type-resolution-in-variable-declarations.md` — confirm that external/struct
  types must be registered in the `CompileContext` before use; the external-class probe
  must register `UserWidget` via `CompileModeClassConverter.registerClass` first.

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Isolating loader accidentally shares JCP's parent, so the boundary isn't real and D1/D2 don't reproduce | M | H | Assert the isolated `StringUtils.class` `!=` the classpath one; fail the harness loudly if they match |
| `resolveModuleClassName` hardcoded to `base` blocks the static-module shorthand for Commons | H | M | Invoke via the external-class/registered-type path (`StaticMethodCallExpression`); document the hardcoded-`base` limit as structural D1 — informs PR 3 |
| Commons already on classpath ⇒ same-loader tests can't show D1/D2 | H | L | That's expected — same-loader tests target D3/D4/D5; the isolated `URLClassLoader` scenario covers D1/D2 |
| Locating the `commons-lang3` JAR path for the `URLClassLoader` is environment-dependent | M | M | Derive the JAR URL from `StringUtils.class.getProtectionDomain().getCodeSource().getLocation()` rather than hardcoding an m2 path |
| Bug turns out to be same-loader D3/D4 only, making the isolated harness look like overkill | M | L | Isolation table simply records D1/D2 as "not fired"; still a valid, decisive outcome for PR-3 scoping |
| Characterization tests mistaken for desired behavior and never updated | M | M | Annotate each `// CHARACTERIZATION: current buggy ANY-collapse; PR 2 updates this` |
| Coverage dips because tests add no production lines | L | M | PR 1 only adds tests; run `mvn verify -pl core` to confirm 80/80 thresholds before pushing |
| Reproduction proves non-deterministic (loader identity varies by JVM) | L | H | Use a fixed `URLClassLoader` with explicit parent; assert on exception *types*, not fragile stack text |

## Next Steps

Implement with `/gw-work`. On completion, the isolation table decides the exact scope of
**PR 2 (descriptor & boxing)** vs **PR 3 (classloader isolation)** — resolving brainstorm
open questions #1 and #3.
