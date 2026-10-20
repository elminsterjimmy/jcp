# Plan Review: fix: unified dual-mode Java class/JAR import API

**Issue:** #49 | **Reviewer:** Claude Code | **Date:** 2026-10-20

## Overall Assessment

**Status:** Approved with Enhancements
**Quality Score:** 7/10

The plan is well-structured, covers all issue requirements, and correctly incorporates historical learnings from `docs/solutions/`. The facade pattern is appropriate and the phased approach is sound. However, the plan underestimates the scope of adding a new `LONG` basic type and has a technical error in the widening direction for float parameters.

## Strengths

- ✅ Facade pattern (`JcpModuleRegistry`) cleanly decouples registration from context creation
- ✅ Excellent use of past learnings from `docs/solutions/` — D6 NPE, context instance pitfalls, and dual-registration requirements all addressed
- ✅ JAR import design is solid: URLClassLoader + MultiClassLoader wiring is exactly right
- ✅ Deferred registration (store entries, replay into context) handles the chicken-and-egg problem well
- ✅ Risk analysis covers the key concerns (init order, singleton isolation, classloader chain)

## Areas for Improvement

### Critical Issues

- ❌ **LONG type scope is underestimated**: Adding LONG touches ~9 compiler files (`ArithmeticCompiler` base class, `CompareCompiler`, `LiteralCompiler`, `PlusPlusCompiler`, `MinusMinusCompiler`, `VariableDeclarationCompiler`, `FunCallCompiler`, `MethodCallCompiler`, `StaticMethodCallCompiler`) plus `TypeMapper` (4 methods), `TypePromotion` (uncomment 2 entries), and corresponding evaluators (`ArithmeticEvaluator` base, `LiteralEvaluator`). Phase 1 is overloaded and should be split.

- ❌ **float→double widening direction is backwards at Java boundary**: Plan says "float params emit `D2F`" but this is the narrowing direction. When JCP holds a DOUBLE value and the Java method wants `float`, the compiler must emit `D2F` (double-to-float **narrowing**, losing precision). The plan frames this as widening but it's actually a narrowing conversion at the call boundary. This needs to be clearly documented as a potential precision-loss point.

- ❌ **`char→STRING` is a deliberate design change**: Both converters currently map `char→INT` (matching JVM semantics). The user has explicitly decided to change this to `char→STRING`, which is valid but needs: (a) backwards-compatibility note, (b) `char[]` array mapping change from `INT_ARRAY` to `STRING_ARRAY`, (c) compile-mode bridge at call sites where Java expects `char` but JCP passes `STRING`.

### Suggestions

- 💡 **Split Phase 1 into two sub-phases**: Phase 1A: expand type mappings for existing types (float→DOUBLE already done, byte/short→INT already done, char→STRING new, long→ANY interim). Phase 1B: add LONG basic type as full infrastructure. This unblocks the unified registry work without waiting for LONG.

- 💡 **`TypePromotion` already has LONG entries**: Lines 54-58 have pre-written enum constants `INT_TO_LONG`, `LONG_TO_DOUBLE` ready to uncomment. This significantly reduces implementation effort for widening support.

- 💡 **`ArithmeticCompiler` is the choke point**: It's the abstract base class used by Plus, Minus, Divide, Multi, Mod compilers. Adding LONG support there covers all 5 arithmetic ops in one edit. Same for `ArithmeticEvaluator` on eval side. The plan should explicitly call these out.

- 💡 **LONG takes 2 JVM slots**: `TypeMapper` already has `getSlotSize()` that handles DOUBLE's 2-slot case. LONG needs the same treatment. `CompileContext.allocateLocal()` may need attention.

- 💡 **Add `long[]→LONG_ARRAY`** to the type mapping table — both converters currently map `long[]→ANY_ARRAY`.

- 💡 **Add eval-mode LONG support explicitly**: `LiteralEvaluator`, `ArithmeticEvaluator`, and `Data<Long>` value wrapping need coverage.

## Specific Feedback by Section

### Overview
Clear problem statement, good motivation. Score: 9/10

### Technical Approach
Architecture diagram is excellent. D6 fix correctly proposes proper type mappings. Missing: ArithmeticCompiler/Evaluator as key files, eval-mode LONG handling, and the narrowing (not widening) nature of DOUBLE→float at Java boundary. Score: 6/10

### Implementation Phases
Logical ordering but Phase 1 is overloaded — combines type mapping fixes with a full new basic type across 12+ files. Should be split. Tests in Phase 5 should be distributed into each phase. Score: 6/10

### Acceptance Criteria
Good functional requirements but missing: eval-mode LONG arithmetic, LONG literal support, `long[]→LONG_ARRAY`, and `char[]→STRING_ARRAY` change. Score: 6/10

### Risk Analysis
Good coverage. Missing: LONG 2-slot allocation, char→STRING backwards-compatibility, `TypePromotion` transitivity chain INT→LONG→DOUBLE. Score: 7/10

## Recommended Actions

1. Split Phase 1 into type-mapping-expansion (1A) and LONG-type-addition (1B)
2. Fix the D2F/narrowing description — it's narrowing at Java boundary, not widening
3. Add `ArithmeticCompiler`/`ArithmeticEvaluator` as key deliverables for LONG
4. Add `long[]→LONG_ARRAY` and `char[]→STRING_ARRAY` to mappings
5. Reference TypePromotion's pre-written LONG entries (lines 54-58)
6. Add eval-mode LONG support (LiteralEvaluator, ArithmeticEvaluator, Data wrapping)
7. Add LONG 2-slot allocation risk to risk table

## Sign-off

- [x] All critical issues addressed in enhanced plan
- [x] Plan ready for implementation
