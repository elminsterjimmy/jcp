# Plan Review: feat: Arrays.sort(Object[], Comparator) for JCP custom types

**Issue:** #55 | **Reviewer:** Claude Code | **Date:** 2026-10-23

## Overall Assessment

**Status:** Approved with Enhancements
**Quality Score:** 8/10

The plan is well-structured, technically sound, and demonstrates a deep understanding of the dual-mode architecture. The two-mode resolution strategy (eval: pre-resolve via EvalContext, compile: inject Class<?> via LDC) is elegant and correctly addresses the core challenge of bridging JCP function names across execution boundaries. The decision to avoid lambdas/function references and keep this as a pure library extension is pragmatic.

## Strengths

- **Clear problem-solution framing** — The "no Comparable on StructData" problem is precisely defined, and the string-based function reference is the right tradeoff for the current language semantics.
- **Dual-mode design is correct** — The eval-mode interception in `ClassConverter.doFunction()` and compile-mode `LDC Class<?>` injection in `FunCallCompiler` are both architecturally sound and leverage existing extension points.
- **End-to-end flow documentation** — Both eval and compile mode flows are traced step-by-step, making the plan auditable.
- **Minimal blast radius** — Only 3 files modified; no new AST nodes, no new types, no language grammar changes.
- **Risk table with mitigations** — Key risks (reflection perf, arity mismatch, existing test regression) are identified with concrete mitigations.

## Areas for Improvement

### Critical Issues

- **None** — The plan is fundamentally sound. No blocking issues that prevent implementation.

### Suggestions

- **Missing: Function return-type validation** — The plan states the comparator function must return `int`, but doesn't specify where or how this is validated. In eval mode, if the function returns a non-integer, `((Number) result.get()).intValue()` will throw a ClassCastException with an opaque message. In compile mode, the generated method's return type needs to be verified as `int`.
- **Missing: `SortKey.sortByFunc(String)` type resolution in `FunCallCompiler.resolveType()`** — The `resolveType()` method in `FunCallCompiler` is used by `TypeMapper` to determine expression types. When `SortKey.sortByFunc("myCompare")` is used in a variable declaration, the compiler needs to resolve this call's return type. The current plan only discusses `compile()` but not `resolveType()`. Since `discoverMethod` already handles this via reflection (returns `SortKey`), this should work automatically — but it's worth calling out explicitly as a verification step.
- **Missing: Thread safety consideration** — `SortKey` is currently immutable (field-path + direction set at construction). The `withResolvedComparator()` approach creates a copy, preserving immutability — good. But the plan should confirm that the interceptor in `convertArgumentsForJava` replaces the array element with the new copy, not just calls `withResolvedComparator` and discards the result.
- **FunctionType.java cleanup should be first step** — Deleting `FunctionType.java` at the end risks forgetting it. Move it to Phase 1 as a cleanup prerequisite since it's from a rejected approach and isn't referenced by any production code.
- **Missing: Test for mixing field-path and function-name SortKeys** — The plan mentions this test case but only as a bullet point in eval tests. It's an important integration scenario (e.g., primary sort by computed comparator, secondary by field-path) that should be verified in both modes.
- **Missing: Negative test for compile mode** — The plan lists negative tests (unknown function, wrong arity) for eval mode only. Compile mode should also have error-path tests (e.g., function name that doesn't exist → clear compile or runtime error).

## Specific Feedback by Section

### Overview
Problem clearly defined. Objectives are well-stated. The example code is immediately understandable. **Score: 9/10**

### Technical Approach
The two-mode resolution design is the plan's strongest section. The eval-mode interceptor approach and compile-mode LDC injection are both well-motivated. One minor gap: the plan references line numbers (line ~196, line 161) which is helpful but fragile. **Score: 9/10**

### Implementation Phases
The plan lists modified files but doesn't define explicit implementation phases with ordering/dependencies. The implicit order (SortKey → ClassConverter → FunCallCompiler → Tests → Cleanup) is correct but should be explicit. **Score: 7/10**

### Acceptance Criteria
No explicit acceptance criteria section. Success is implied (tests pass, coverage ≥ 85%) but not enumerated. Missing: performance criteria, backward compatibility verification, API ergonomics criteria. **Score: 5/10**

### Risk Analysis
Good coverage of the main risks. Missing: what happens if the user declares a comparator function with wrong parameter types (not `Any`) — does it silently fail or error clearly? **Score: 7/10**

## Recommended Actions

1. Add explicit acceptance criteria (functional + non-functional)
2. Add implementation phase ordering with dependencies
3. Add negative test cases for compile mode
4. Add function return-type validation in both modes
5. Move FunctionType.java cleanup to Phase 1
6. Add test for mixed field-path + function-name SortKey usage in both modes

## Sign-off

- [x] All critical issues addressed (none found)
- [x] Plan ready for implementation (with suggested enhancements)
