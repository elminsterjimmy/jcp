# Plan Review: feat: Strings STD API for base module

**Issue:** #44 | **Reviewer:** Claude Code | **Date:** 2026-03-18

## Overall Assessment

**Status:** Approved with Enhancements
**Quality Score:** 7/10

The plan is solid and well-grounded. It correctly identifies the zero-infrastructure-change approach, confirms the `STRING_ARRAY` pipeline end-to-end, and has a complete 13-method API. The main gaps are in the eval test strategy (the plan says "verify return value" but uses the wrong test pattern) and missing `sub`/`split` int-literal construction details.

## Strengths

- ✅ Infrastructure analysis is thorough — confirmed `CompileModeClassConverter`, `TypeMapper`, and `FunCallCompiler` all work for `String[]` without changes
- ✅ `replace` keyword clash (`new` → `newStr`) pre-identified in risk analysis
- ✅ Both calling conventions (`Strings.xxx(s)` and `base::Strings.xxx(s)`) documented
- ✅ Each method has a clear Java delegation target
- ✅ Test locations, base classes, and patterns are all specified

## Areas for Improvement

### Critical Issues

- ❌ **Eval test return-value strategy is wrong.** The plan says "`length("hello")` → passes (result == 5)" but `AssertionsTest` only calls `newVisitor().visit(block)` and never reads a return value. To verify return values in eval mode, tests must use `VariableDeclarationImpl` to assign the result, then call `context.getVariable("result").get()`. The plan needs to specify this pattern explicitly, otherwise the test author will either skip assertions or use the wrong API.

- ❌ **`sub` int arguments — no literal type specified.** `sub(s, from, to)` takes `int` parameters. The plan doesn't say what `LiteralExpression` to use for `from`/`to`. The correct form is `LiteralExpression.of(Literal.of(1))` (which produces an `IntLiteral`). Same applies to `split` — testing the result array requires knowing how to assert on a `String[]` returned via compile-mode method invocation.

### Suggestions

- 💡 **Compile-mode return-value assertions need a concrete pattern.** `AssertionsCompileTest` only calls `main.invoke(...)` and checks `assertDoesNotThrow` — it never reads a non-void return value. For `Strings`, most methods return values. The correct approach in compile mode is to compile the call as a `return` statement (not just a void call), then read the return value from `main.invoke(...)`. Or alternatively, use a compiled `Assertions.assertEquals` call to assert the result inline, keeping the same void-main pattern.
- 💡 **`split` result verification in eval mode needs `Data` unwrapping details.** When `split("a,b,c", ",")` is called in eval mode, the result is a `Data<String[]>`. The test should assign it to a variable and call `(String[]) context.getVariable("result").get()`, then assert elements.
- 💡 **`contains` parameter type in `Strings.java`.** The plan says `delegating to s.contains(t)` with `String t`. The Java signature is `contains(CharSequence)` — passing `String` works fine, but the parameter type in `Strings.java` should be `String` (not `CharSequence`) to match what the reflection-based dispatcher expects from a `STRING` DataType argument.

## Specific Feedback by Section

### Overview
Clear, concise, and accurate. 13 methods, zero infrastructure changes, follows `Assertions` pattern. **Score: 9/10**

### Technical Approach
Strong — `STRING_ARRAY` pipeline confirmed, both calling conventions documented, `replace` and `split` semantics spelled out. Minor gap: doesn't mention the eval test pattern for capturing return values. **Score: 7/10**

### Implementation Phases
Phase 1 (implementation) and Phase 2 (registration) are complete and correct. Phases 3 and 4 (tests) are under-specified: eval tests say "verify return value" without explaining the `VariableDeclarationImpl` + `context.getVariable` pattern; compile tests say "verify return value" without explaining how to read a return value from a compiled `main` method. **Score: 6/10**

### Acceptance Criteria
Functional criteria are complete. Non-functional criteria are appropriate. Missing: a criterion that return values are actually asserted (not just `assertDoesNotThrow`). **Score: 7/10**

### Risk Analysis
`replace`/`new` keyword clash and `split`/`STRING_ARRAY` pipeline are well-covered. Missing: the risk that the eval and compile test patterns for return-value assertion are unclear and could lead to tests that pass without actually verifying correctness. **Score: 6/10**

## Recommended Actions

1. **Specify eval test return-value pattern explicitly** — use `VariableDeclarationImpl("result", type, new FunctionCallExpression(...))` then assert `context.getVariable("result").get()`.
2. **Specify compile test return-value pattern** — either read `main.invoke(...)` return value, or use `Assertions.assertEquals` inside the compiled program to keep the void-main pattern.
3. **Clarify int literal construction** for `sub` and `indexOf` parameters — `LiteralExpression.of(Literal.of(1))`.

## Sign-off

- [x] All critical issues addressed in enhanced plan
- [x] Plan ready for implementation
