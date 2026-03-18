---
title: "Plan Review: feat: extend Assertions STD API in base module"
issue: 48
reviewer: Claude Code
date: 2026-03-10
original_plan: docs/plans/2026-03-10-feat-extend-assertions-std-api-plan.md
---

# Plan Review: feat: extend Assertions STD API in base module

**Issue:** #48 | **Reviewer:** Claude Code | **Date:** 2026-03-10

## Overall Assessment

**Status:** Approved with Enhancements
**Quality Score:** 7/10

The plan is well-structured and clearly scoped. It correctly identifies that no changes are needed to the module registration or eval/compile infrastructure. The main gaps are: (1) `double` is missing from the test plan despite being in the implementation list, (2) the null-literal risk is noted but left unresolved — `NullLiteral.INSTANCE` actually exists and is usable, so this risk can be closed out, (3) the `fastfail` generalization approach needs to be decided upfront to avoid inconsistent error messages.

## Strengths

- ✅ Correctly scoped — no infrastructure changes, just class extension + tests
- ✅ All 8 new method signatures are explicitly listed in Phase 1
- ✅ Covers both eval and compile modes in separate test phases
- ✅ Risk about `assertEquals(Object)` / `assertEquals(String)` overload resolution is correctly flagged
- ✅ Acceptance criteria map 1:1 to the implementation checklist

## Areas for Improvement

### Critical Issues

- None blocking. All issues below are improvements.

### Suggestions

- 💡 **Resolve the null-literal risk now:** `NullLiteral.INSTANCE` exists in `LiteralExpression.of(NullLiteral.INSTANCE)` — the risk entry should be resolved rather than deferred
- 💡 **Add `double` tests:** `assertEquals(double, double)` is in Phase 1 but has no test entries in Phase 2 or Phase 3
- 💡 **Decide `fastfail` strategy upfront:** the plan says "generalize or overload" — choose one. Recommended: add a generic `fastfail(Object expected, Object actual)` that uses `String.valueOf()` for the message, keeping the existing `fastfail(boolean, boolean)` for `assertTrue`/`assertFalse`
- 💡 **Add `assertEquals(boolean, boolean)` tests:** listed in Phase 1 but absent from Phase 2/3
- 💡 **Clarify null in compile mode:** the plan doesn't address how `assertNull` is represented in compile mode (what AST expression passes `null` as an argument to a static method)

## Specific Feedback by Section

### Overview
Clear and accurate. States the minimal-change approach correctly. **Score: 9/10**

### Technical Approach
Good. Correctly notes eval/compile discovery via reflection. The `fastfail` decision needs to be made concrete before coding. **Score: 7/10**

### Implementation Phases
Phase 1 is complete. Phases 2 and 3 are missing test cases for `double` and `boolean` overloads. **Score: 6/10**

### Acceptance Criteria
Functional requirements match the API. Non-functional requirements are clear. Could benefit from explicitly checking error message content (not just that an exception is thrown). **Score: 7/10**

### Risk Analysis
Good identification of the two main risks. The null-literal risk can be resolved (see Suggestions). **Score: 7/10**

## Recommended Actions

1. Resolve the null-literal risk — `NullLiteral.INSTANCE` exists, document how to use it in tests
2. Add missing test cases for `assertEquals(double, double)` and `assertEquals(boolean, boolean)` in Phases 2 and 3
3. Commit to a `fastfail` strategy (generic overload recommended)
4. Add note on how `assertNull`/`assertNotNull` are tested in compile mode (what AST node represents null)

## Sign-off

- [x] All critical issues addressed (none blocking)
- [x] Plan ready for implementation (with enhancements in enhanced plan)
