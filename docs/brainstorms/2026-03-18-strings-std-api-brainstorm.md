# Brainstorm: Strings STD API for base module

**Date:** 2026-03-18
**Status:** Ready for planning
**Issue:** #44

## What We're Building

A `Strings` utility class at `com.elminster.jcp.module.base.strings.Strings` providing 13 static string-manipulation methods callable from JCP programs in both eval and compile mode. Methods delegate directly to `java.lang.String` instance methods with DSL-friendly names.

## Why

JCP DSL programs need common string operations. Without a `Strings` STD class, DSL authors must use raw Java reflection or work around the lack of string utilities. This follows the same pattern as `Assertions`, `Logger`, and `ValueBuffer` — static Java methods registered in `BaseModuleRegister` and auto-discovered by both eval and compile infrastructure.

## API

```
Strings.length(s)              # string length → int
Strings.sub(s, from, to)       # substring, from inclusive, to exclusive → String
Strings.concat(a, b)           # concatenate two strings → String
Strings.indexOf(s, t)          # index of t in s, -1 if not found → int
Strings.contains(s, t)         # true if s contains t → boolean
Strings.upper(s)               # to upper case → String
Strings.lower(s)               # to lower case → String
Strings.trim(s)                # trim whitespace → String
Strings.replace(s, old, new)   # replace all occurrences → String
Strings.startsWith(s, prefix)  # true if s starts with prefix → boolean
Strings.endsWith(s, suffix)    # true if s ends with suffix → boolean
Strings.isEmpty(s)             # true if s is empty → boolean
Strings.split(s, delimiter)    # split by delimiter → String[]
```

## Scope

### Deliverables
- `Strings.java` with 13 static methods
- `BaseModuleRegister` updated to include `Strings.class`
- Eval mode tests: `StringsTest.java` (passing + failing for each method)
- Compile mode tests: `StringsCompileTest.java` (passing + failing for each method)
- JaCoCo coverage ≥ 80%

### Out of Scope
- `indexOf(s, t, fromIndex)` — keep it simple, single-occurrence search only
- Regex-based methods (e.g., `matches`, `replaceAll` with regex)
- Null-safe wrappers — null inputs throw `NullPointerException` naturally (consistent with Java String)
- `format` / `printf` — out of scope for this issue

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Return types | Native Java types (`int`, `boolean`, `String`, `String[]`) | Consistent with `Assertions`; module infrastructure handles type bridging |
| Null handling | Let Java throw `NullPointerException` | Minimal code, consistent with Java semantics |
| `indexOf` overloads | Single `indexOf(s, t)` only | KISS; fromIndex can be added later if needed |
| Extra methods | `startsWith`, `endsWith`, `isEmpty`, `split` | Natural companions; `split` returns `String[]` which `STRING_ARRAY` in TypeMapper already supports |
| `split` scope | Include in this issue | `STRING_ARRAY` already handled by `TypeMapper` and `CompileModeClassConverter` |
| Calling convention | `Strings.length(s)` — utility class, uppercase | Instance-style `s.length()` would require extending `MethodCallEvaluator` + `MethodCallCompiler` to handle `SystemDataType.STRING` specially. `::` syntax would require AST/parser changes. Utility approach works today with zero infrastructure changes, consistent with `Assertions`, `Logger`, `ValueBuffer` |

## Open Questions

1. Should `split` use a regex delimiter (like `String.split()`) or a literal delimiter (like `StringUtils.splitByWholeSeparator`)? — Default: use `String.split()` directly (regex), document it.
2. Does `Strings.replace(s, old, new)` use `String.replace(CharSequence, CharSequence)` — yes, literal replacement, not regex.

## Future Consideration

Instance-style method calls on built-in types (`s.length()`, `s.upper()`) are a natural future enhancement. It would require:
- `MethodCallEvaluator`: detect `SystemDataType.STRING` target and map to registered `Strings` module methods (or directly reflect on `java.lang.String`)
- `MethodCallCompiler`: same detection, emit `INVOKEVIRTUAL java/lang/String.xxx` directly

This is a separate issue and should be tracked independently once the utility class is in place.

## Implementation Notes

- Pattern follows `Assertions.java` exactly: `public static <ReturnType> method(String s, ...)` delegating to `s.method(...)`
- Register: add `Strings.class` to `BaseModuleRegister.classToRegister()`
- No changes needed to eval or compile infrastructure — reflection-based dispatch handles it automatically
- Test pattern follows `AssertionsTest.java` (eval) and `AssertionsCompileTest.java` (compile)
- `split` test: verify `String[]` result has expected length and elements (in eval mode via `DataFactory`; in compile mode via method return value)

## Next Steps

Run `/gw-plan` to create implementation plan.
