# Plan Review: feat: Arrays.sort for Struct Arrays with SortKey Builder

**Issue:** #56 | **Reviewer:** Claude Code | **Date:** 2026-03-20

## Overall Assessment

**Status:** Needs Revision
**Quality Score:** 6/10

The plan is well-structured and covers the right scope, but contains a **critical blocking flaw**: the `SortKey...` varargs approach is incompatible with JCP's reflection-based dispatch mechanism. Without addressing this, the implementation cannot work. The rest of the plan is solid.

## Strengths

- ✅ Clear rationale for `SortKey` as a pure Java builder (no `SystemDataType` changes)
- ✅ Field path resolution algorithm is well-specified (split, walk, null-last)
- ✅ Null-last semantics are explicitly designed for both ASC and DESC
- ✅ `Arrays.reverse` correctly identified as orthogonal and scoped as typed overloads
- ✅ Risk table is honest and mentions the varargs dispatch concern — but the mitigation is insufficient

## Areas for Improvement

### Critical Issues

- ❌ **Varargs `SortKey...` is incompatible with JCP's runtime dispatch.** `ClassConverter.getParameterDefs()` introspects via `method.getParameters()`, which sees `SortKey...` as `SortKey[]` — a single fixed-arity array parameter. `FunCallEvaluator.hasSameParameterDefinition()` matches by `parameterDefs.length == arguments.length`. A caller passing 2 `SortKey` objects will have `argumentData.length == 2`, but the registered function has `parameterDefs.length == 1` (`SortKey[]`). This is a dispatch mismatch that will produce `UndeclaredException` at runtime.

- ❌ **No resolution strategy for the varargs problem is provided.** The risk entry says "Java method resolution prefers the more specific overload" — this applies to Java compile-time resolution, not to JCP's reflection-based runtime dispatch. The mitigation is wrong.

### Suggestions

- 💡 Replace `SortKey...` with explicit fixed-arity overloads: `sort(Object[], SortKey)`, `sort(Object[], SortKey, SortKey)`, `sort(Object[], SortKey, SortKey, SortKey)`. This is the same pattern used throughout `Arrays.java` for typed overloads. Covers the common cases (1–3 sort keys) without varargs complexity.
- 💡 Alternatively, accept a `SortKey[]` parameter explicitly — but then callers must construct the array explicitly, which is awkward at the JCP DSL level.
- 💡 Add a test that explicitly exercises `FunctionCallExpression` dispatch (not just unit-testing `SortKey.toComparator()` directly), since that is the only way to catch dispatch failures.
- 💡 The `getArgumentValues(arguments)` call in `ClassConverter.registerStaticMethod` unwraps `Data.get()` for each argument. For `SortKey`, this means the `SortKey` object itself is passed to the Java method — which is correct. But this chain should be explicitly verified in the integration test.

## Specific Feedback by Section

### Overview
Clear, concise, accurate. Score: 9/10

### Technical Approach
Field path resolution and comparator construction are well-specified. The `SortKey` registration rationale is sound. However, the varargs assumption is wrong and is the root of the critical issue. Score: 5/10

### Implementation Phases
Phases are logical and ordered correctly. Phase 1 (SortKey) before Phase 2 (Arrays) is right. Missing: a phase for evaluating the dispatch strategy before writing code. Score: 6/10

### Acceptance Criteria
Functional criteria are comprehensive. Non-functional criteria correctly exclude compile-mode. Missing: a criterion for "calling via FunctionCallExpression dispatch works end-to-end". Score: 7/10

### Risk Analysis
The varargs risk is identified but the mitigation is incorrect (Java compile-time resolution ≠ JCP runtime dispatch). Score: 4/10

## Recommended Actions

1. **Replace `SortKey...` varargs with fixed-arity overloads** — `sort(Object[], SortKey)`, `sort(Object[], SortKey, SortKey)`, `sort(Object[], SortKey, SortKey, SortKey)` in `Arrays.java`. Consistent with existing `Arrays` pattern.
2. **Add an integration test** that calls `Arrays.sort` via `FunctionCallExpression` with 1, 2, and 3 `SortKey` arguments, not just direct Java method calls. This validates the full dispatch path.
3. **Update risk table** to remove the incorrect Java-compile-time mitigation and document the fixed-arity approach as the resolution.

## Sign-off

- [ ] All critical issues addressed
- [ ] Plan ready for implementation
