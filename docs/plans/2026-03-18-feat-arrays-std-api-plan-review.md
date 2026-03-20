# Plan Review: feat: Arrays STD API for base module

**Issue:** #46 | **Reviewer:** Claude Code | **Date:** 2026-03-18

## Overall Assessment

**Status:** Approved with Enhancements
**Quality Score:** 7/10

The plan is well-structured and follows the established `Strings` STD API pattern closely. Phases
are logically ordered and acceptance criteria are testable. The main gap is an unresolved **critical
risk** in the `length` method: `Object.class.getSimpleName()` is `"Object"`, which maps to
`SystemDataType.ANY` via `ClassConverter.getDataType`. This means the registered function signature
for `length` will be `(ANY) → INT`, not `(INT_ARRAY) → INT`, which is actually fine for dispatch
but needs to be documented clearly. The more critical boxing issue — `int[].class.getSimpleName()`
= `"int[]"` looks up `"int"` in the context, which is not registered (`"Integer"` is) — means the
typed overload signatures for `slice`/`contains`/`sort(int[])` may not resolve correctly through
`ClassConverter`. This needs a concrete resolution strategy before implementation begins.

Compile-mode test coverage is thin — only `String[]` overloads are tested since there's no direct
way to construct `int[]`/`boolean[]`/`double[]` literals in the AST. This is noted as a risk but
the plan doesn't offer a resolution path.

## Strengths

- ✅ Clear method API table with return types and semantics documented
- ✅ `boolean[]` sort edge case (false-before-true) explicitly called out
- ✅ Eval test setup approach (`context.addVariable` + `VariableExpression`) is concrete
- ✅ `slice` out-of-bounds behavior decided (throw, not clamp)
- ✅ All four array element types included, consistent with the type system

## Areas for Improvement

### Critical Issues

- ❌ **Boxing/unboxing gap not resolved:** `ClassConverter.getDataType(int[].class, ...)` calls
  `DataTypeUtils.getDataType("int[]", ctx)` → strips `[]` → looks up `"int"` → not in context
  (only `"Integer"` is). This means `sort(int[])`, `slice(int[], int, int)`, etc. will be
  registered with an unresolved or wrong type, causing `UndeclaredException` at eval time.
  **Resolution:** Use `Integer[]`/`Boolean[]`/`Double[]` overloads instead of primitive array
  overloads in the Java source — these map cleanly to `"Integer[]"` → `"Integer"` → found.

- ❌ **Compile-mode test gap not resolved:** The plan acknowledges that only `String[]` overloads
  can be tested in compile mode, but does not address `int[]`/`boolean[]`/`double[]` compile
  coverage. If those overloads are registered with wrong types, compile mode will also fail.

### Suggestions

- 💡 **Clarify `ArrayData` constructor usage in eval tests:** The plan says "pre-seed with
  `ArrayData`" but doesn't specify the constructor call. Since `ArrayData` stores `Integer[]` not
  `int[]` (based on the test in `ArrayDataTest`), show the exact pattern:
  `new ArrayData<>(SystemDataType.INT, Identifier.fromName("a"), new Integer[]{1,2,3})`.

- 💡 **Add `slice` edge cases to test plan:** Test `slice(a, 0, 0)` (empty result) and
  `slice(a, 0, length)` (full copy) to ensure branch coverage on the `copyOfRange` path.

- 💡 **Document `length(Object)` type registration side-effect:** When `ClassConverter` processes
  `length(Object array)`, it will look up `"Object"` → `SystemDataType.ANY`. The function registers
  as `(ANY) → INT`. Eval dispatch will accept any array via `isCompatibleWith(ANY)`. This is
  actually correct behavior — note it in the plan so the implementer isn't surprised.

## Specific Feedback by Section

### Overview
Clear and concise. API surface is well-specified. Score: 9/10

### Technical Approach
Mostly sound, but the `ClassConverter`/`DataTypeUtils` interaction with primitive array simple
names is a blocking gap not addressed. Score: 5/10

### Implementation Phases
Deliverables are concrete and checkable. Compile-mode test phase is thin for non-String types.
Score: 7/10

### Acceptance Criteria
Testable and complete for what's listed. Missing a criterion for `int[]`/`boolean[]`/`double[]`
coverage in compile mode. Score: 7/10

### Risk Analysis
The boxing risk is correctly identified but the mitigation is vague ("may need to adjust overload
signature"). Needs a concrete decision. Score: 6/10

## Recommended Actions

1. **Switch to boxed-type overloads** (`Integer[]`, `Boolean[]`, `Double[]`) for all typed methods
   so `ClassConverter` maps them correctly via `"Integer[]"` → `"Integer"` → `SystemDataType.INT`.
   Note: the actual Java array values stored in `ArrayData` are already boxed (`Integer[]` not
   `int[]`) per the `ArrayDataTest` patterns.
2. **Add explicit constructor patterns** to the eval test section showing how to build `ArrayData`
   with boxed arrays.
3. **Expand compile-mode test coverage** — at minimum, test `length` with a `String[]` input and
   note that `int[]`/`boolean[]`/`double[]` overloads are not directly testable in compile mode
   without an array-literal AST node.

## Sign-off

- [ ] All critical issues addressed
- [ ] Plan ready for implementation
