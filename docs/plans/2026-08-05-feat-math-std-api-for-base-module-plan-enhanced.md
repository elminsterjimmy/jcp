---
title: "feat: Math STD API for base module (Enhanced)"
type: feat
date: 2026-08-05
issue: 43
review_date: 2026-08-05
original_plan: docs/plans/2026-08-05-feat-math-std-api-for-base-module-plan.md
---

# feat: Math STD API for base module (Enhanced)

> **Note:** Enhanced version incorporating the 2026-08-05 plan review. The material
> change: the overload-resolution fix must be applied in **two** resolvers (eval +
> compile), and the former blocking risk **R3 is resolved** — verified empirically
> that eval mode already widens int→double for double-only methods.

## Overview

Implement `Math` as a base module STD class exposing seven numeric operations —
`abs`, `sqrt`, `min`, `max`, `pow`, `floor`, `ceil` — callable from DSL code as
`Math.abs(x)` (or `base::Math.abs(x)`) in both eval and compile modes, following
the established STD pattern (`Convert`, `Arrays`, `Assertions`).

The caller writes one clean name (`Math.abs`); typed Java overloads live
underneath; dispatch selects the exact-type implementation — C `<tgmath.h>`
semantics. This requires a small, symmetric fix to overload resolution in both
execution modes.

## Technical Approach

### Verified invocation paths (corrected from original plan)

STD methods are invoked as a `FunctionCallExpression` whose identifier is the
dotted name `"Math.abs"` (confirmed in `ConvertTest`, `ArraysTest`). This routes:

- **Eval mode → `FunCallEvaluator`** (NOT `StaticMethodCallEvaluator`):
  - `getFunctionCandidates` filters by `isCompatibleWith` (hierarchy + widening).
  - `applyWideningConversions` converts INT args to `DoubleData` for DOUBLE params.
  - **Verified:** `Convert.doubleToString(5)` → `"5.0"`. Int args reach double-only
    methods correctly. → **R3 does not exist.**
  - **BUT:** when >1 candidate is compatible, it throws `FunctionAmbiguityException`
    (`FunCallEvaluator`, ~L72–76). Since `INT.isCompatibleWith(DOUBLE)` is true,
    `abs(int)`+`abs(double)` BOTH match an int arg → **ambiguity in eval too.**

- **Compile mode → `StaticMethodCallCompiler`**:
  - `ExternalClassType.getStaticMethod` → `findMethodWithOverloadResolution`
    (same compatibility scan) → throws `IllegalArgumentException("Ambiguous ...")`
    for the same reason.
  - `I2D` promotion + `INVOKESTATIC` emission already exist (L140–145). No change.

### The fix: exact-match beats promotion — in BOTH resolvers

**Definition of exact match:** for every argument `i`, `argType[i] == paramType[i]`
(or `argType[i].getName().equals(paramType[i].getName())`). Any compatible match
that is not exact is a widening/hierarchy match.

**Rule:** after collecting all compatible candidates, if more than one remains,
keep only exact-match candidates. If exactly one exact match exists, select it.
Otherwise (zero or ≥2 exact matches) fall back to the existing Ambiguous behavior.

Apply symmetrically in:
1. `ExternalClassType.findMethodWithOverloadResolution` (compile mode)
2. `FunCallEvaluator.getFunctionCandidates` / its candidate-narrowing (eval mode)

This resolves `abs(int)` vs `abs(double)` identically in both modes, preserves
INT→INT / DOUBLE→DOUBLE return types, and needs no boxing. It also retires the
project-wide numeric-overload gotcha (Convert's `intToString`/`doubleToString`
could later collapse to `toString`).

### Overload table (finalized — R3 resolved)

| Method | Overloads | Return | Notes |
|--------|-----------|--------|-------|
| `abs`   | `abs(int)`, `abs(double)` | int / double | Both meaningful; exact-match fix disambiguates. |
| `min`   | `min(int,int)`, `min(double,double)` | int / double | Mixed args (`min(1,2.0)`) promote to double-double — single candidate. |
| `max`   | `max(int,int)`, `max(double,double)` | int / double | Same as min. |
| `sqrt`  | `sqrt(double)` | double | Int arg widens (verified). |
| `pow`   | `pow(double,double)` | double | Int args widen. |
| `floor` | `floor(double)` | double | Int arg widens. |
| `ceil`  | `ceil(double)` | double | Int arg widens. |

All delegate to `java.lang.Math`. Private constructor; class is a static utility.

## Implementation Phases

### Phase 1: Symmetric overload-resolution fix (foundation)

**Deliverables:**
- [ ] Add exact-match narrowing to `ExternalClassType.findMethodWithOverloadResolution`
- [ ] Add the same narrowing to `FunCallEvaluator` candidate selection (eval)
- [ ] Extract/share the "is exact match" predicate to keep both sides identical

**Acceptance Criteria:**
- [ ] Unit test (compile): `foo(int)`/`foo(double)`; INT arg → int overload, DOUBLE → double
- [ ] Unit test (eval): same, via `FunCallEvaluator`
- [ ] Genuine ambiguity (two `ANY`-param overloads, no exact match) still throws in both modes
- [ ] **Full existing STD suite green** — `ConvertTest`, `ArraysTest`, `StringsTest`,
      `AssertionsTest` + compile counterparts (regression gate for shared resolver)

### Phase 2: Implement `Math` class

**Deliverables:**
- [ ] `com.elminster.jcp.module.base.math.Math` with private ctor + methods per table
- [ ] Javadoc in `Convert` style (document single-name/typed-overload design)
- [ ] Register `Math.class` in `BaseModuleRegister.classToRegister()`

**Acceptance Criteria:**
- [ ] Class compiles; registered in both eval and compile registration flows

### Phase 3: Tests (both modes)

**Deliverables:**
- [ ] `eval/function/MathTest.java` — every method, int + double args, edge cases
      (negative abs, `sqrt(0)`, `pow(2,10)`, `floor(2.7)`, `ceil(2.1)`)
- [ ] `compile/module/MathCompileTest.java` (extends `AbstractCompileTest`, mirrors
      `ConvertCompileTest`) — same coverage via inline `Assertions.assertEquals`
- [ ] Dispatch test: `Math.abs(-5)`→INT `5`; `Math.abs(-5.0)`→DOUBLE `5.0` (both modes)
- [ ] Mixed-arg test: `Math.min(1, 2.0)` → `1.0` (double), no ambiguity (both modes)

**Acceptance Criteria:**
- [ ] `mvn verify -pl core` passes JaCoCo ≥ 80% instruction & branch

## Acceptance Criteria

### Functional Requirements

- [ ] All 7 methods callable in eval AND compile modes
- [ ] `Math.abs(int)`→INT, `Math.abs(double)`→DOUBLE (no type loss)
- [ ] `sqrt`/`pow`/`floor`/`ceil` accept int args (widen to double) in both modes
- [ ] `Math.min(1, 2.0)` resolves unambiguously in both modes
- [ ] Resolver fix breaks no existing STD test

### Non-Functional Requirements

- [ ] Coverage ≥ 80% instruction and branch (JaCoCo)
- [ ] No new runtime dependencies (delegates to `java.lang.Math`)
- [ ] Work on branch `feat/43-math-std-api`; PR to master (never commit to master)
- [ ] Shared resolver change reviewed for blast radius before merge

## Dependencies

- Phase 1 (both-resolver fix) blocks Phases 2–3
- Existing `AbstractCompileTest`, `StaticMethodCallExpression`, `Literal` helpers
- Independent of IO (#45)

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| R1: Resolver change alters matching for other STD classes | M | H | Fix only narrows the >1-match case to prefer exact; Phase 1 full-suite regression gate |
| R2: `min(1, 2.0)` mixed args ambiguous | L | M | Only `(double,double)` matches a double arg → single candidate; explicit test both modes |
| R4: `pow`/`sqrt` returning DOUBLE surprises int-expecting users | L | L | Documented in Javadoc; matches Java and most languages |
| R5: Coverage dip from untested overload branches | L | M | Phase 3 tests every overload with int and double literals |
| **R6 (new): eval + compile resolvers drift out of sync** | M | H | Share one exact-match predicate; test the identical scenario in both modes in Phase 1 |
| R7: eval fix accidentally suppresses a legitimate ambiguity error | L | M | Fall back to existing throw when zero or ≥2 exact matches; negative test asserts throw preserved |

## Testing Strategy

- **Phase 1 unit tests** target the resolver directly (both modes) — fastest feedback.
- **Regression gate**: existing STD suites must stay green before Phase 2.
- **Phase 3** exercises Math end-to-end in both modes, including the two dispatch
  edge cases (exact-int vs exact-double, mixed promotion).
- Coverage verified via `mvn verify -pl core`.

## Success Metrics

- 7/7 Math methods pass in both modes
- Both dispatch edge-case tests pass in both modes
- Zero regressions in existing STD suites
- JaCoCo ≥ 80%/80% maintained

## Changes from Original Plan

1. **Two-resolver fix (was one)** — verified eval STD calls go through
   `FunCallEvaluator`, which throws the same ambiguity as compile mode; the fix is
   now symmetric across `ExternalClassType` and `FunCallEvaluator`.
2. **R3 resolved and removed as blocker** — empirically confirmed eval widens
   int→double (`Convert.doubleToString(5)` → `"5.0"`); double-only methods accept
   int args, so no int overloads or key-path changes are needed.
3. **Added R6/R7** — resolver-drift and false-negative-ambiguity risks, with a
   shared predicate and preserved-throw fallback as mitigations.
4. **Added regression gate + mixed-arg tests** — full STD suite green as a Phase 1
   exit condition; `Math.min(1,2.0)` tested in both modes.

## References

- Original plan: `docs/plans/2026-08-05-feat-math-std-api-for-base-module-plan.md`
- Plan review: `docs/plans/2026-08-05-feat-math-std-api-for-base-module-plan-review.md`
- Issue: https://github.com/elminsterjimmy/jcp/issues/43
