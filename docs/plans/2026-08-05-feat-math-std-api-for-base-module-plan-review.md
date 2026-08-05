# Plan Review: feat: Math STD API for base module

**Issue:** #43 | **Reviewer:** Claude Code | **Date:** 2026-08-05

## Overall Assessment

**Status:** Needs Revision (one material correction; otherwise sound)
**Quality Score:** 7/10

The original plan is well-structured and correctly identifies the core mechanism
(single clean name + typed overloads + exact-match resolution). However, its
central risk analysis — **R3** and the claim that "eval mode needs no ambiguity
fix" — was based on the **wrong evaluator**. Empirical verification during this
review overturned that assumption and revealed the fix must land in **two**
resolvers, not one. This is a correctness-affecting gap that would have surfaced
mid-implementation. Correcting it makes the plan solid.

## What the review verified (empirically)

A throwaway probe (`Convert.doubleToString(5)` with an int literal) and a code
trace of `FunCallEvaluator` established the ground truth:

| Question | Original plan assumed | Verified reality |
|----------|----------------------|------------------|
| Which eval path do STD calls (`Math.abs(x)`) use? | `StaticMethodCallEvaluator` (exact-string key) | **`FunCallEvaluator`** — STD calls are `FunctionCallExpression` with dotted id `"Math.abs"` (see `ConvertTest`/`ArraysTest`) |
| Does eval widen int→double for a double-only method? | Uncertain (R3 flagged as blocker) | **Yes** — `Convert.doubleToString(5)` returns `"5.0"` |
| Does eval throw ambiguity on `abs(int)`+`abs(double)`? | "No — eval is safe" | **Yes** — `FunCallEvaluator.getFunction` throws `FunctionAmbiguityException` when >1 candidate; `INT.isCompatibleWith(DOUBLE)` makes both match |

**Consequence:** R3 (double-only methods rejecting int args) is a **non-issue** —
eval widens correctly. But the ambiguity fix is needed in **BOTH**
`ExternalClassType.findMethodWithOverloadResolution` (compile) **and**
`FunCallEvaluator.getFunctionCandidates` (eval).

## Strengths

- ✅ Correctly frames the design as C `<tgmath.h>`-style single-name dispatch
- ✅ Accurately identifies that compile-mode promotion/boxing (`I2D`) already exists
- ✅ Sensible overload table (typed `abs`/`min`/`max`; double-only `sqrt`/`pow`/`floor`/`ceil`)
- ✅ Good test strategy split (eval `MathTest` + compile `MathCompileTest` mirroring `ConvertCompileTest`)
- ✅ Applies the project's overload-gotcha learning rather than repeating the distinct-name workaround

## Areas for Improvement

### Critical Issues

- ❌ **Resolver fix scope was understated.** Plan said the fix is compile-mode-only
  (`ExternalClassType`). Verified: eval mode (`FunCallEvaluator`) has the identical
  ambiguity and must get the symmetric fix. Enhanced plan moves this into Phase 1
  as a two-file change with tests in both modes.
- ❌ **R3 was mis-scoped as a blocker.** It was based on `StaticMethodCallEvaluator`,
  which is not the STD invocation path. Enhanced plan removes R3 as a blocker and
  documents the verified widening behavior instead.

### Suggestions

- 💡 Define "exact match" precisely for both resolvers: `argType == paramType` (or
  equal `getName()`); everything else compatible is a widening/hierarchy match.
- 💡 Add a regression guard: run the full existing STD suite (`ConvertTest`,
  `ArraysTest`, `StringsTest`, `AssertionsTest`, and their compile counterparts)
  after the resolver change, since both resolvers are shared infrastructure.
- 💡 Add an explicit mixed-arg test (`Math.min(1, 2.0)`) — should resolve to
  `(double,double)` with no ambiguity, in both modes.

## Specific Feedback by Section

### Overview
Clear problem and objective; correctly ties to brainstorm #62. **8/10**

### Technical Approach
Sound mechanism, but the eval/compile path analysis had the critical error above.
Now corrected. **6/10** (→ 9/10 enhanced)

### Implementation Phases
Logical. Phase 1 needed expansion to cover both resolvers + full-suite regression.
**7/10**

### Acceptance Criteria
Testable and concrete. Added mixed-arg + both-mode ambiguity criteria. **8/10**

### Risk Analysis
R1/R2/R4/R5 are good. R3 was wrong (removed/rewritten). Added R6 (shared-resolver
regression) as the real top risk. **6/10** (→ 9/10 enhanced)

## Recommended Actions

1. Apply the exact-match-beats-promotion fix in **both** `ExternalClassType` and
   `FunCallEvaluator`.
2. Remove R3 as a blocker; document verified widening.
3. Run the full existing STD test suite as a Phase 1 regression gate.
4. Add mixed-arg and both-mode ambiguity-resolution tests.

## Sign-off

- [x] All critical issues addressed (in enhanced plan)
- [x] Plan ready for implementation
