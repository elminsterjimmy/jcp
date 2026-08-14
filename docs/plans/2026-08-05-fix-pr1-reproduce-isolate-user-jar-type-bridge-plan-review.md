# Plan Review: fix: PR 1 — Reproduce & isolate the user-JAR compile-mode type-bridge failure

**Issue:** #50 | **Reviewer:** Claude Code | **Date:** 2026-08-05

## Overall Assessment

**Status:** Needs Revision (correctness fix required before implementation)
**Quality Score:** 6/10

The plan is well-structured, correctly scoped (tests-only, real 3rd-party JAR via a JCP
type, both loader scenarios), and applies relevant solution-doc learnings. **However, a
code-level verification of its central technical claim found a material inaccuracy:** the
plan asserts that invoking a registered JCP type will reproduce **D3 (descriptor built from
JCP arg types)** and **D4 (no boxing)** as `NoSuchMethodError`/`VerifyError`. Tracing the
actual code shows the registered-JCP-type path does **not** use the defective
`FunCallCompiler.compileModuleFunctionCall` logic at all — it uses
`StaticMethodCallCompiler.compileExternalClassCall`, which is **descriptor-correct** and
**already boxes**. So the plan's probes would largely come back green and produce a
**misleading isolation table** — the exact failure mode PR 1 exists to prevent.

The fix is not a redesign; it's a re-targeting of what each probe expects, plus adding the
probe for the *real* failure mode on this path (a boxing/exact-descriptor conflict driven by
the `char`/`CharSequence`→`ANY` gap). With that correction the plan is strong.

## Strengths

- ✅ Correct high-level strategy: real library (`commons-lang3`, already a compile dep),
  invocation through a registered JCP type, both same-loader and isolated-loader scenarios.
- ✅ Tests-only scoping keeps the reproduction reviewable and ties later fixes to real red tests.
- ✅ Applies the `noclassdeffounderror-struct-classes-not-loaded.md` learning (define-then-load
  order, `MultiClassLoader` delegation, the separate loader in `compileAndLoadWithReturn`).
- ✅ Good risk instinct on loader realism (isolated `Class != ` classpath `Class` sanity assertion).
- ✅ Correctly flags the `resolveModuleClassName` hardcoded-`base` limitation as structural.

## Areas for Improvement

### Critical Issues

- ❌ **Wrong code path attributed to the JCP-type invocation.** A `StaticMethodCallExpression`
  on a registered `ExternalClassType` compiles via
  `StaticMethodCallCompiler.compileExternalClassCall` (`StaticMethodCallCompiler.java:120`),
  **not** `FunCallCompiler.compileModuleFunctionCall`. D3/D4 as catalogued live in the latter.
- ❌ **D3 will not fire on this path.** The emitted descriptor is `method.getDescriptor()` =
  `Type.getMethodDescriptor(method)` (`CompileModeClassConverter.java:96`) — the **exact real
  Java signature**, built at registration from reflection, independent of the lossy
  `mapJavaTypeToDataType`. The `INVOKESTATIC` descriptor therefore matches the real method.
- ❌ **D4 (“no boxing”) will not fire on this path.** `compileExternalClassCall` **does** box
  for `ANY` params (`StaticMethodCallCompiler.java:142–144`).
- ❌ **The real failure mode on this path is mis-described.** Because `char`/`CharSequence`
  collapse to `ANY` in `mapJavaTypeToDataType` but the descriptor stays exact
  (`(CI)Ljava/lang/String;` for `repeat(char,int)`), the `paramType==ANY` branch boxes the
  arg to `Integer` while the descriptor demands a primitive `char` → **VerifyError from a
  boxing/exact-descriptor conflict**, plus overload-resolution ambiguity from `argTypes`
  computed with lossy types. This is the genuine, reproducible defect — and the plan should
  target it explicitly rather than the mis-attributed D3/D4.

### Suggestions

- 💡 Split the defect model by **code path**: (a) registered-JCP-type path
  (`StaticMethodCallCompiler`) — the path this PR actually exercises; (b) module-shorthand
  path (`FunCallCompiler.compileModuleFunctionCall`) — where D3/D4-as-catalogued live but
  which is unreachable for non-`base` types today. State clearly that the module-shorthand
  path's descriptor/boxing bug is **verified by code reading**, not by an executable probe in
  PR 1 (because it can't be reached), and hand it to PR 2/3.
- 💡 Add a probe for the **eval-mode** equivalent: register `StringUtils` via
  `ClassConverter` and call through a JCP type in the interpreter, to confirm whether the
  `char`/`CharSequence` unwrapping round-trips (the "bad round-trip value" symptom the user hit).
- 💡 For the same-loader baseline, `capitalize(String)→String` should **pass** (String is
  descriptor-exact and needs no boxing) — make that an explicit *green* control so a failure
  there signals harness breakage, not a product defect.
- 💡 Name the boxing/exact-descriptor conflict as its own defect id (**D4′**) so PR 2's fix
  (respect the exact param type / don't box when descriptor is primitive) has a clear target
  distinct from the `FunCallCompiler` D4.

## Specific Feedback by Section

### Overview — 8/10
Clear problem, correct scoping and intent. Slightly overstates that D3/D4 will reproduce
through the JCP-type path.

### Technical Approach — 5/10
Fixture choice, loader scenarios, and "invoke through a JCP type" are right. The
defect→observable mapping is the weak point: it routes D3/D4 through the wrong compiler and
predicts the wrong exceptions for the registered-type path. Needs the path-split above.

### Implementation Phases — 6/10
Phase structure is sound. Phase 2's probes need re-pointing at the real failure mode (D4′
VerifyError + overload ambiguity), and an explicit green control. Phase 3 (isolated loader,
D1/D2) is accurate as written. Consider adding an eval-mode phase.

### Acceptance Criteria — 6/10
Good on "invoke through a JCP type" and "both loader scenarios". The "each of D1–D5 fires"
framing is misleading — reframe to "each defect is either reproduced *or* proven-not-to-fire
on this path, with the reason recorded" (the plan's prose says this; the criteria should too).

### Risk Analysis — 7/10
Solid on loader realism and coverage. Missing the biggest risk: **misattributing the failing
path and producing a wrong isolation table** — the very outcome this review caught. Add it.

## Recommended Actions

1. Re-map the defect table to the **registered-JCP-type path** (`StaticMethodCallCompiler`):
   D3/D4-as-catalogued do **not** fire here; the real defect is **D4′** (boxing vs exact
   primitive descriptor) + overload ambiguity from lossy `argTypes`.
2. Add a **green control** (`capitalize(String)`) and re-point the `repeat(char,int)` /
   `countMatches(CharSequence,char)` probes at D4′.
3. State that `FunCallCompiler` D3/D4 are **code-verified, not executably reproduced** in PR 1
   (path unreachable for non-`base`), and assign them to PR 2/3.
4. Add an **eval-mode** probe for the char/CharSequence round-trip.
5. Add the "wrong isolation table" risk with mitigation (verify each probe's expected
   exception against the compiler it actually hits before asserting).

## Sign-off

- [ ] Critical path-attribution issue addressed (defect table re-mapped)
- [ ] Green control + D4′ probes added
- [ ] Enhanced plan ready for implementation
