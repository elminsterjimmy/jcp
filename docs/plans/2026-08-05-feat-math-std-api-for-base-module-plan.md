---
title: "feat: Math STD API for base module"
type: feat
date: 2026-08-05
issue: 43
---

# feat: Math STD API for base module

## Overview

Implement `Math` as a base module STD class exposing seven numeric operations —
`abs`, `sqrt`, `min`, `max`, `pow`, `floor`, `ceil` — callable from DSL code as
`Math.abs(x)` (or `base::Math.abs(x)`) in both eval and compile modes, following
the established STD pattern (`Convert`, `Arrays`, `Assertions`).

The plan delivers the clean single-name interface decided in the brainstorm
(#62): the caller writes one name (`Math.abs`), typed Java overloads live
underneath, and dispatch picks the exact-type implementation — C `<tgmath.h>`
semantics. This requires one small, targeted fix to compile-mode overload
resolution.

## Technical Approach

### How STD classes are wired (verified)

- **Registration** happens in `BaseModuleRegister.classToRegister()` — one list,
  consumed by BOTH modes:
  - Eval: `RootEvalContext.registerSystemFunctions()` → `ClassConverter.registerClass`
    registers each public static method as a `Function` keyed by full name
    `Math.abs#INT`, `Math.abs#DOUBLE`, etc.
  - Compile: `BytecodeGenerator` → `CompileModeClassConverter.registerClass`
    builds an `ExternalClassType` with `ExternalMethodDef`s per overload.
- **Invocation** as `Math.abs(x)` routes through `StaticMethodCallExpression`:
  - Eval: `StaticMethodCallEvaluator` builds an **exact-string key**
    (`Math.abs#INT`) and does a direct map lookup — no compatibility scan, so
    **no ambiguity in eval mode**. Requires the exact typed overload to exist.
  - Compile: `StaticMethodCallCompiler.compileExternalClassCall` →
    `ExternalClassType.getStaticMethod(name, argTypes)` →
    `findMethodWithOverloadResolution`.

### The one real gap: compile-mode overload resolution

`findMethodWithOverloadResolution` collects every candidate whose params are
`isCompatibleWith` the args. Because `INT.isCompatibleWith(DOUBLE)` is true
(numeric promotion), an `int` argument matches BOTH `abs(int)` and `abs(double)`,
so the method throws `IllegalArgumentException("Ambiguous method call ...")`.

**Fix (chosen approach — exact-match beats promotion):** when the compatibility
scan yields more than one match, filter to candidates where every argument type
**equals** the parameter type. If exactly one exact match remains, select it.
Otherwise fall back to the existing Ambiguous throw. This is a localized change
inside `findMethodWithOverloadResolution` (also covers the constructor/instance
paths that share the same helper indirectly — only the static path is exercised
by Math, but the fix is written once in the shared method).

Compile-mode argument promotion (`I2D`) and the `INVOKESTATIC` emission already
exist in `StaticMethodCallCompiler` (lines 140–145) and need no change.

### Which methods get which overloads

`java.lang.Math` signatures drive this. To keep both eval (exact-key) and compile
(exact-match) modes clean:

| Method | Overloads exposed | Return | Notes |
|--------|-------------------|--------|-------|
| `abs`   | `abs(int)`, `abs(double)` | int / double | Both meaningful; delegates to `java.lang.Math.abs`. |
| `min`   | `min(int,int)`, `min(double,double)` | int / double | Delegates to `java.lang.Math.min`. |
| `max`   | `max(int,int)`, `max(double,double)` | int / double | Delegates to `java.lang.Math.max`. |
| `sqrt`  | `sqrt(double)` | double | Naturally double; int arg auto-promotes via I2D (compile) / widening (eval). |
| `pow`   | `pow(double,double)` | double | Naturally double; int args promote. |
| `floor` | `floor(double)` | double | Naturally double. |
| `ceil`  | `ceil(double)` | double | Naturally double. |

Rationale: `abs`/`min`/`max` are meaningful and commonly wanted on ints, so both
typed overloads are provided (the resolver fix makes them unambiguous). `sqrt`,
`pow`, `floor`, `ceil` are inherently double-valued, so a single `double` overload
suffices — an int argument promotes cleanly in both modes (eval:
`FunCallEvaluator.applyWideningConversions` / static path builds `#DOUBLE` key only
if... see Risk R3; compile: `I2D`).

### Eval-mode exact-key caveat (Risk R3)

`StaticMethodCallEvaluator` builds the lookup key from the **actual argument
types** (`Math.sqrt#INT` for `Math.sqrt(4)`), but only `Math.sqrt#DOUBLE` is
registered. This is a genuine gap for the double-only methods when called with an
int literal. The plan resolves this in Phase 1 by confirming the eval static-call
path's behavior and, if it does not already widen, providing the int overloads too
OR adding widening to the static-call key generation. This must be verified with a
test before finalizing the overload table above.

## Implementation Phases

### Phase 1: Fix + verify overload resolution (foundation)

- [ ] Add exact-match-beats-promotion logic to
      `ExternalClassType.findMethodWithOverloadResolution`
- [ ] Unit test: two overloads `foo(int)`/`foo(double)`, assert `foo` with INT arg
      resolves to the int overload (not Ambiguous), DOUBLE arg → double overload
- [ ] Unit test: genuine ambiguity (two ANY-param overloads) still throws
- [ ] **Verify eval-mode `Math.sqrt(4)`** (int→double-only method) behavior via a
      throwaway test; decide int-overload vs widening based on result (Risk R3)

### Phase 2: Implement `Math` class

- [ ] Create `com.elminster.jcp.module.base.math.Math` with private ctor and the
      static methods per the overload table
- [ ] Javadoc mirroring `Convert`'s style (document the single-name/typed-overload
      design and the resolver dependency)
- [ ] Register `Math.class` in `BaseModuleRegister.classToRegister()`

### Phase 3: Tests (both modes)

- [ ] Eval test `eval/function/MathTest.java` — every method, int and double
      arguments where applicable, plus edge cases (negative abs, `sqrt(0)`,
      `pow(2,10)`, `floor(2.7)`, `ceil(2.1)`)
- [ ] Compile test `compile/module/MathCompileTest.java` (extends
      `AbstractCompileTest`, mirrors `ConvertCompileTest`) — same coverage, asserts
      via inline `Assertions.assertEquals`
- [ ] Explicit overload-dispatch test: `Math.abs(-5)` returns INT `5`,
      `Math.abs(-5.0)` returns DOUBLE `5.0` — proves exact-match resolution
- [ ] `mvn verify -pl core` passes JaCoCo ≥ 80% instruction & branch

## Acceptance Criteria

### Functional Requirements

- [ ] `Math.abs/sqrt/min/max/pow/floor/ceil` callable in eval mode
- [ ] Same callable in compile mode
- [ ] `Math.abs(int)` returns INT; `Math.abs(double)` returns DOUBLE (no type loss)
- [ ] `Math` registered in `BaseModuleRegister`
- [ ] Ambiguity fix does not break any existing STD (Convert/Arrays/Assertions) test

### Non-Functional Requirements

- [ ] Coverage ≥ 80% instruction and branch on core (JaCoCo)
- [ ] No new dependencies (delegates to `java.lang.Math`)
- [ ] Feature branch `feat/43-math-std-api`; PR to master (never commit to master)

## Dependencies

- Compile-mode resolver fix (Phase 1) unblocks the clean interface — must land first
- Existing `AbstractCompileTest`, `StaticMethodCallExpression`, `Literal` helpers
- No dependency on IO (#45); can proceed independently

## Known Learnings Applied

Searched `docs/solutions/` — the three existing docs are struct-type specific
(compile-context registration, NoClassDefFound) and not directly applicable to
`java.lang.Math` delegation. The transferable lesson — **a type/method must be
registered in the compile context before it can be resolved** — is already
satisfied by adding `Math.class` to `BaseModuleRegister` (Phase 2), which both
modes consume.

- `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md`
  — reinforces that registration in `BaseModuleRegister` is the single required
  wiring step for compile-mode resolution.

Additional project learning applied (from memory): the *Ambiguous module function*
gotcha on numeric overloads is exactly what Phase 1 fixes; `Convert` only uses
distinct names (`intToString`/`doubleToString`) to dodge it, and this plan does
NOT repeat that workaround for `Math`.

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| R1: Resolver fix changes behavior for other STD classes (Arrays/Convert) that rely on current matching | M | H | Fix only narrows >1-match cases to prefer exact; run full existing STD test suite in Phase 1 before proceeding |
| R2: `min(int,int)`/`min(double,double)` still ambiguous for mixed args e.g. `min(1, 2.0)` | L | M | Mixed args promote to `(double,double)` via I2D; no int-int candidate matches a double arg, so only one candidate — no ambiguity. Add a mixed-arg test |
| R3: Eval `Math.sqrt(4)` builds key `sqrt#INT` but only `sqrt#DOUBLE` registered → not found | M | M | Verified in Phase 1; resolve by adding int overloads for double-only methods OR widening in the eval static-call key path. Blocks finalizing overload table |
| R4: `pow`/`sqrt` return DOUBLE surprises users expecting int from int args | L | L | Documented in Javadoc; matches Java/most languages |
| R5: Coverage dip from untested overload branches | L | M | Phase 3 tests every overload with both int and double literals |

## Next Steps

Run `/gw-work` to begin implementation on branch `feat/43-math-std-api`,
starting with Phase 1 (resolver fix + eval-mode verification).
