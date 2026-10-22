# Plan Review: Dictionary collection type and Java collection auto-conversion

**Issue:** #84 | **Reviewer:** Claude Code | **Date:** 2026-10-22

## Overall Assessment

**Status:** Approved with Enhancements
**Quality Score:** 7/10

Solid plan with good structural coverage and strong learnings integration. The plan correctly identifies the key interception points and follows existing patterns (ValueBuffer). However, it has one critical technical inaccuracy in the auto-conversion argument path, several missing implementation details, and lacks a testing strategy section.

## Strengths

- ✅ **Correct wrapping vs. inheritance decision** — Dictionary wraps HashMap rather than extending it, avoiding cascading registration of `Set<K>`, `Collection<V>`, `Map.Entry` etc.
- ✅ **Strong learnings integration** — D5, D6, and overload resolution issues are all correctly referenced and their implications analyzed
- ✅ **Correct deferral of compile-mode auto-conversion** — Recognizes that bytecode-level `CHECKCAST` + factory call is complex and unnecessary for phase 1
- ✅ **Phase separation** — Dictionary class (phase 1) is cleanly separated from auto-conversion (phases 2-3), allowing incremental delivery
- ✅ **Risk analysis covers the highest-impact risk** — Existing module functions returning raw Map/List being unintentionally converted

## Areas for Improvement

### Critical Issues

- ❌ **`getArgumentValues` modification won't work as described.** The plan says "In `ClassConverter.getArgumentValues`: if an argument's `Data.get()` returns a `Dictionary`..." — but `getArgumentValues` is a static utility (`private static Object[] getArgumentValues(Data[] arguments)`) with no access to the Java `Method`'s parameter types. It cannot know the target Java param is `java.util.Map`. The conversion logic must be placed inside each `doFunction` closure where the captured `method` reference provides `method.getParameterTypes()`.

- ❌ **`keys()` returning `String[]` is a lossy assumption.** If a Java method returns `Map<Integer, String>`, the auto-converted Dictionary will have non-String keys. Calling `keys()` that returns `String[]` would require casting each key to String, throwing ClassCastException for non-String keys. Should return `Object[]` (mapped to `ANY_ARRAY`) for safety, or document the String-key restriction and throw a clear error.

### Suggestions

- 💡 **Add a `Dictionary(Map<?, ?>)` constructor** for the auto-conversion path — avoids re-inserting entries one by one; just assign the backing map directly.
- 💡 **Consider whether `ValueBuffer.toList()` should extract all rows or flatten to single-column** — the plan says "first column only" but doesn't address the common case where VB has multiple columns. A `toList()` that returns `List<Object[]>` (list of rows) may be more useful for Java interop.
- 💡 **The plan lists `toMap()` in both Phase 1 and Phase 2 deliverables** — consolidate to avoid duplication.
- 💡 **Missing: how auto-conversion interacts with `null` return values** — if a Java method returns `null` and the type is `Map`, should it become an empty Dictionary or remain null?

## Specific Feedback by Section

### Overview
Clear problem statement, good motivation from the user perspective. Score: 9/10

### Technical Approach
Sound architecture overall. The eval-mode interception points are correctly identified (lines 159-170 and 262-282 in ClassConverter). However, the argument-unwrapping approach is technically incorrect as described. The compile-mode deferral is well-justified. Score: 6/10

### Implementation Phases
Good logical separation. Phase 1 is self-contained and testable. Phases 2 and 3 are independent and could run in parallel. Phase 4 is appropriate for cleanup. Missing: specific file paths for test classes. Score: 7/10

### Acceptance Criteria
Functional criteria are clear and testable. Non-functional criteria could be more specific (e.g., "auto-conversion overhead < 1μs per call" is more testable than "negligible"). Score: 7/10

### Risk Analysis
Covers the key risks. Missing: risk that `keys()` returning `String[]` breaks for non-String-keyed Maps; risk that auto-conversion of Map subclasses (LinkedHashMap, TreeMap) loses ordering guarantees. Score: 6/10

## Recommended Actions

1. Fix the argument-unwrapping approach: conversion must happen inside `doFunction` closures, not in `getArgumentValues`
2. Change `keys()` return type from `String[]` to `Object[]` for safety
3. Add `Dictionary(Map<?, ?>)` constructor for efficient auto-conversion
4. Add testing strategy section with specific test class locations and test fixture class
5. Add risks for non-String keys and Map-subclass ordering loss

## Sign-off

- [ ] All critical issues addressed
- [x] Plan ready for implementation (with enhancements)
