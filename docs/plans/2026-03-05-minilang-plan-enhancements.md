# MiniLang Implementation Plan - Research Enhancements

**Date:** 2026-03-05
**Status:** Enhancement recommendations from /workflows:deepen-plan
**Base Plan:** `/Users/I772698/workspaces/jcp/docs/plans/2026-03-05-feat-minilang-reference-dsl-plan.md`

## Executive Summary

14 parallel research agents analyzed the MiniLang implementation plan across multiple dimensions: ANTLR integration, architecture, patterns, security, performance, data integrity, and institutional learnings. This document consolidates critical findings and actionable recommendations.

---

## 🔴 CRITICAL ISSUES (Must Address Before Implementation)

### 1. **Dual-Registration Pattern Missing from Phase 2**

**Finding:** The plan mentions dual-registration in risk section but doesn't integrate it into actual implementation tasks.

**From Learning:** `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md`

**Issue:** Declaration compilers for custom types MUST call BOTH:
- `ctx.addDataType(typeMetadata)` - enables compile-time type checking
- `ctx.addGeneratedClass(name, bytecode)` - enables runtime class loading

**Where to Add in Plan:**
- **Phase 2, Task List (Line ~690)** - Add explicit note when implementing StructDeclarationImpl
- **Phase 2, After Line 707** - Add new "Critical Pattern: Custom Type Registration" section with test verification pattern
- **Phase 5, Around Line 850** - Add explicit test requirement for dual registration verification

**Recommended Text:**
```markdown
**Phase 2: AST Builder Implementation**

Tasks:
...
3. Implement visitor methods for all statement types:
   - `visitStructDeclStmt` → `StructDeclarationImpl`

     ⚠️ **CRITICAL:** When building StructDeclarationImpl, ensure JCP's
     StructDeclarationCompiler performs dual registration:
     - `ctx.addDataType(structType)` - compile-time type checking
     - `ctx.addGeneratedClass(name, bytecode)` - runtime loading

     See: docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md
```

---

### 2. **Type Resolution Pattern Incorrectly Implemented**

**Finding:** The TypeResolver class (lines 418-449) does NOT follow the documented pattern.

**From Learning:** `docs/solutions/logic-errors/struct-type-resolution-in-variable-declarations.md`

**Problems:**
1. No CompileContext parameter - cannot look up registered struct types
2. Returns `DataTypeImpl` placeholder instead of actual StructType from context
3. Will cause "Cannot determine struct type for field access" errors

**Current (Wrong):**
```java
// Custom type (struct name)
return new DataTypeImpl(typeName);  // ❌ Placeholder, not real type
```

**Should Be (Two-Phase Resolution):**
```java
// Parse-time: Store type names as placeholders
return new DataTypeImpl(typeName);  // OK at parse-time

// Compile-time: Resolve actual types using context
private DataType resolveActualType(DataType declared, CompileContext ctx) {
    if (declared instanceof SystemDataType) return declared;

    // Standard pattern: check system types, then custom types
    String typeName = declared.getName();
    for (SystemDataType sdt : SystemDataType.values()) {
        if (sdt.getName().equalsIgnoreCase(typeName)) {
            return sdt;
        }
    }
    DataType customType = ctx.getDataType(typeName);  // ✅ Context lookup
    if (customType != null) {
        return customType;
    }
    return SystemDataType.ANY;
}
```

**Where to Update:**
- **Section 3.3 TypeResolver (Lines 415-449)** - Add note explaining two-phase resolution
- **Phase 2, Task 8 (New)** - Add "Implement runtime type resolution in all compilers/evaluators"
- **Success Criteria (Line 724)** - Make type resolution test more specific

---

### 3. **Missing Test Matrix for All Compilation Entry Points**

**Finding:** Plan mentions testing all entry points but lacks explicit requirement to test custom types through ALL paths.

**From Learning:** `docs/solutions/runtime-errors/noclassdeffounderror-struct-classes-not-loaded.md`

**Issue:** When BytecodeGenerator has multiple code generation methods, ALL must store context in rootContext instance field. Missing this causes `getGeneratedClasses()` to return empty.

**Test Matrix Required:**
| Entry Point | Method Generated | Struct Support |
|-------------|------------------|----------------|
| `compile()` | `main()` | ✅ MUST TEST |
| `compileWithReturn()` | `evaluate()` | ✅ MUST TEST |
| `compileToMultipleClasses()` | `main()` | ✅ MUST TEST |

**Where to Add:**
- **Phase 4 (Line ~813)** - Add context management pattern to Runner implementation
- **Phase 5, Task 4 (Line 866)** - Make test matrix explicit requirement
- **Phase 6 (Line ~1015)** - Add testing section to README with this warning

---

## 🟡 HIGH PRIORITY ARCHITECTURAL IMPROVEMENTS

### 4. **Module Boundary Ambiguity**

**Finding:** Plan proposes `module/minilang/` but existing module code lives in `core/src/main/java/com/elminster/jcp/module/base/`

**Recommendation:** Clarify module structure decision before implementation:

**Option A (Recommended):** True module system
```
module/
  minilang/           # New: MiniLang DSL
  system/             # Move from core/module/base/
```

**Option B:** Temporary placement
```
core/src/main/java/com/elminster/jcp/
  module/
    base/             # System modules (existing)
    minilang/         # MiniLang (temporary)
```

**Where to Add:** After Problem Statement section (~Line 120)

---

### 5. **Split AstBuilder to Prevent God Object**

**Finding:** Single AstBuilder class will handle 15+ distinct concerns and exceed 300-500 lines, violating KISS principles.

**Recommendation:** Split into category-specific builders:
```java
public class MiniLangAstBuilder extends MiniLangBaseVisitor<Node> {
    private final ExpressionBuilder exprBuilder = new ExpressionBuilder();
    private final StatementBuilder stmtBuilder = new StatementBuilder();
    private final DeclarationBuilder declBuilder = new DeclarationBuilder();

    @Override
    public Node visitExpression(ExpressionContext ctx) {
        return exprBuilder.build(ctx);
    }
}
```

**Where to Add:** Section 3 Technical Approach (~Line 300)

---

### 6. **Security Vulnerabilities Identified**

**From Security Audit:** 7 vulnerabilities across 4 severity levels:

1. **Path Traversal (HIGH)** - Arbitrary file read in runner
2. **Resource Exhaustion (HIGH)** - No limits on parsing complexity
3. **String Escape Bug (MEDIUM)** - Incorrect processing order
4. **Unsafe ClassLoader (CRITICAL/LOW)** - Arbitrary bytecode loading
5. **Class Name Injection (MEDIUM)** - Unsanitized filename conversion
6. **Information Disclosure (LOW)** - Verbose errors
7. **Example Security (LOW)** - No review process

**Impact:** +6 days implementation (30% overhead)

**Full Report:** `/Users/I772698/workspaces/jcp/docs/security/2026-03-05-minilang-security-audit-report.md`

**Where to Add:**
- Risk Analysis section (~Line 1250) - Add security risks
- Phase 4 Runner Implementation (~Line 820) - Add input validation
- Phase 5 Testing (~Line 880) - Add security test suite

---

## 🟢 PERFORMANCE OPTIMIZATIONS (Optional)

### 7. **Parser Performance**

**Finding:** ANTLR parse time expected < 10ms per 1000 LOC. No optimization needed initially.

**Recommendations:**
- Enable grammar analysis during build to catch ambiguity
- Configure parallel test execution (3× speedup available)
- Add build/test time tracking to CI

**Full Report:** `/Users/I772698/workspaces/jcp/docs/plans/2026-03-05-performance-analysis-minilang-dsl.md`

---

### 8. **Factory Reflection Overhead**

**Finding:** Factory reflection adds ~575ns per node (vs ~3ns direct). For interpretation: 0.05% to 50% overhead depending on operation.

**Recommendation:** Add evaluator caching for hot paths (10× speedup in loops).

**Where to Add:** Future Considerations section (~Line 1350)

---

## 📋 SIMPLIFICATION OPPORTUNITIES (25-30% LOC Reduction)

### 9. **Excessive Example Files**

**Current:** 8 separate example files
**Recommended:** 4 consolidated files
- `basics.minilang` (hello + variables + arithmetic)
- `functions.minilang`
- `control-flow.minilang`
- `advanced.minilang` (arrays + structs)

**Impact:** 50% file reduction, still demonstrates all features

---

### 10. **Redundant Test Classes**

**Current:** 5 separate test classes (HelloWorldTest, FunctionsTest, etc.)
**Recommended:** Single parameterized test:
```java
@ParameterizedTest
@ValueSource(strings = {"01-hello-world.minilang", "02-variables.minilang", ...})
void testExampleInBothModes(String exampleFile) {
    // Single test method for all examples
}
```

**Impact:** ~300 LOC reduction in tests

---

### 11. **Inline TypeResolver**

**Current:** Separate TypeResolver class (~35 lines)
**Recommended:** Private method in AstBuilder

**Impact:** Remove entire class file, simpler data flow

---

### 12. **Remove YAGNI Violations**

**Items to Remove from Plan:**
- Future extensibility planning (for loops, lambdas, generics) - lines 1356-1387
- IDE support (VSCode, IntelliJ plugins) - lines 1370-1374
- Version compatibility strategy - lines 1383-1398
- Advanced tutorial series - lines 1428-1432
- CHANGELOG.md requirement

**Impact:** Focus plan on reference implementation, not production DSL

---

## 🔍 DATA INTEGRITY CONCERNS

### 13. **Missing Compile-Time Type Validation**

**Finding:** StructInstantiationCompiler and FieldAssignmentCompiler have TODOs indicating missing type checking.

**Impact:** Compiler mode allows invalid types, fails at runtime with VerifyError. Interpreter mode properly validates.

**Recommendation:** Implement `TypeMapper.getExpressionType()` validation before Phase 5 testing.

---

### 14. **Inconsistent Type Promotion**

**Finding:** AssignmentEvaluator only uses `isCastableTo()`, doesn't allow int→double promotion.

**Recommendation:** Update to use `isCompatibleWith()` for type promotion support.

---

## 📚 DOCUMENTATION ENHANCEMENTS

### 15. **Consolidate Documentation Files**

**Current Plan:** README + GRAMMAR.md + CUSTOMIZATION.md (3 files)
**Recommended:** Single comprehensive README with clear sections

**Impact:** 300% easier to maintain, users find everything in one place

---

### 16. **Add ANTLR Best Practices**

**From Research:** `/Users/I772698/workspaces/jcp/docs/research/2026-03-05-antlr4-framework-documentation.md`

**Key Patterns to Document:**
- Visitor vs listener (recommend visitor for returning values)
- Left-recursive expression rules for precedence
- Keywords MUST come before ID rule in lexer
- `visitLabel()` before `visitLineNumber()` (ASM requirement)

**Where to Add:** Phase 1 Grammar Implementation (~Line 650)

---

### 17. **Add ASM Bytecode Patterns**

**Key Patterns:**
- ClassWriter with COMPUTE_FRAMES | COMPUTE_MAXS flags
- NEW + DUP pattern for object creation
- Type descriptor generation (int→I, String→Ljava/lang/String;)
- Local variable slot sizing (double/long = 2 slots)

**Where to Add:** Phase 2 Implementation Notes (~Line 700)

---

## 🎯 ACCEPTANCE CRITERIA UPDATES

### 18. **Make Test Requirements Explicit**

**Current:** "Type resolution works for system and custom types"

**Should Be:**
```markdown
- [ ] Type resolution works:
  - [ ] Parse-time: TypeResolver returns system types or placeholders
  - [ ] Compile-time: VariableDeclarationCompiler resolves struct types from context
  - [ ] Eval-time: VariableDeclarationEvaluator resolves struct types from context
  - [ ] Field access compiles on struct-typed variables
  - [ ] Function parameters with struct types resolve correctly
```

**Where to Update:** Acceptance Criteria section (~Line 1115)

---

## 📊 RISK UPDATES

### 19. **Add Blocking Requirement Reference**

**Current:** Risk mentions dual-registration
**Should Add:** Link to solution documents as BLOCKING requirements

**Text:**
```markdown
**BLOCKING REQUIREMENTS:**
- docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md
  MUST implement dual-registration pattern
- docs/solutions/logic-errors/struct-type-resolution-in-variable-declarations.md
  MUST pass CompileContext to all type resolution
- docs/solutions/runtime-errors/noclassdeffounderror-struct-classes-not-loaded.md
  MUST test all compilation entry points with custom types
```

**Where to Add:** Risk Analysis section (~Line 1268)

---

## 🗂️ REFERENCE FILES CREATED

### Research Documents Generated:
1. `/Users/I772698/workspaces/jcp/docs/research/2026-03-05-antlr4-framework-documentation.md`
2. `/Users/I772698/workspaces/jcp/docs/security/2026-03-05-minilang-security-audit-report.md`
3. `/Users/I772698/workspaces/jcp/docs/security/SECURITY-SUMMARY.md`
4. `/Users/I772698/workspaces/jcp/docs/plans/2026-03-05-performance-analysis-minilang-dsl.md`

### Institutional Learnings Applied:
- `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md`
- `docs/solutions/logic-errors/struct-type-resolution-in-variable-declarations.md`
- `docs/solutions/runtime-errors/noclassdeffounderror-struct-classes-not-loaded.md`

---

## 📈 IMPACT SUMMARY

**LOC Reduction:** 25-30% potential savings
- Implementation: ~1500 → ~1100 lines (27% reduction)
- Tests: ~1000 → ~700 lines (30% reduction)
- Documentation: 3 files → 1 file (67% reduction)

**Quality Improvements:**
- Security vulnerabilities identified and documented (+6 days implementation)
- Critical bug patterns from learnings integrated
- Architecture ambiguities clarified
- Performance baseline established

**Time Impact:**
- Original estimate: 20 days (4 weeks)
- With security fixes: 26 days (~5 weeks)
- With simplifications: Net ~23 days (security overhead partially offset)

---

## ✅ NEXT STEPS

1. **Review this enhancement document** with team/stakeholders
2. **Update base plan** with critical issues (#1-3) before implementation starts
3. **Incorporate high-priority items** (#4-6) during Phase 1-2
4. **Apply simplifications** (#9-12) to reduce scope
5. **Reference security audit** when implementing Phase 4 Runner
6. **Use research documents** as implementation guides

---

## 📞 Questions for User

1. **Module structure decision:** True module system (Option A) or temporary placement (Option B)?
2. **Security overhead:** Accept +6 days for security fixes or defer to later?
3. **Simplification scope:** Remove all YAGNI items or keep some future planning?
4. **Documentation format:** Single README or keep separate GRAMMAR.md/CUSTOMIZATION.md?

---

**Enhancement Complete:** All 14 research agents' findings consolidated.
**Status:** Ready for plan updates based on priorities.