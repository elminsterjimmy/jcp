---
title: "feat: Add compile mode support for MiniLang assertion tests"
type: feat
date: 2026-03-06
issue: 31
---

# feat: Add compile mode support for MiniLang assertion tests

## Overview

Enable MiniLang tests to run in both evaluation and compilation modes, achieving JCP's dual-mode execution guarantee. Currently, 14 MiniLang test scripts (9 assertion tests + 5 simple boolean tests) only pass in eval mode. This plan adds the missing compile-mode infrastructure to support module function calls, the NOT operator, and proper test result access patterns.

**Goal:** Increase test coverage from 31 tests to 45+ tests by running all MiniLang scripts in both modes.

## Technical Approach

### 1. Module Function Resolution in Compile Mode

**Current state:** `FunctionCallExpression` doesn't resolve module functions during compilation. Module functions like `Assertions.assertTrue()` work in eval mode but produce "Undefined function" errors in compile mode.

**Solution:** Extend `FunCallCompiler` to handle module function lookup:
- Check if function name matches a registered module function
- Generate `INVOKESTATIC` bytecode for module calls (similar to existing `StaticMethodCallExpression`)
- Store module function registry in `CompileContext` for compile-time resolution

**Pattern:**
```java
// In FunCallCompiler
if (isModuleFunction(funcName, compileContext)) {
    ModuleFunction func = compileContext.getModuleFunction(funcName);
    // Generate INVOKESTATIC for the module's static method
    generateStaticCall(func.getClassName(), func.getMethodName(), ...);
} else {
    // Existing user-defined function lookup
}
```

### 2. NOT Operator Bytecode Generation

**Current state:** `NotExpression` node exists but lacks a compiler implementation.

**Solution:** Create `NotCompiler` class extending `AbstractAstCompiler`:
- Compile inner expression (should leave boolean on stack)
- Use `ICONST_1 + IXOR` pattern to flip boolean value
  - Push 1 onto stack
  - XOR with expression result (0 XOR 1 = 1, 1 XOR 1 = 0)

**Bytecode pattern:**
```
[compile inner expression]  // Leaves 0 or 1 on stack
ICONST_1                    // Push 1
IXOR                        // XOR: flips the boolean
```

**Naming:** The node's `getName()` returns `"NOT"`, so the compiler must be named `NotCompiler` to match factory lookup.

### 3. Test Variable Access in Compiled Programs

**Current problem:** Tests use reflection to access `result` field, but compiled programs use local variables, not fields.

**Solution options:**

**Option A (Recommended): Return-based testing**
- Wrap test logic in a function that returns the boolean result
- Test harness invokes the function and checks return value
- No need for fields or complex reflection

**Option B: Static field storage**
- Generate static fields for test result variables
- Requires detecting which variables are test outputs
- More complex but supports multiple result values

**Recommendation:** Start with Option A (return-based) for simplicity. If tests need multiple outputs, add Option B later.

## Implementation Phases

### Phase 1: NOT Operator Compilation
**Scope:** Add bytecode generation for logical NOT operator
**Files:** `core/src/main/java/com/elminster/jcp/compile/operator/NotCompiler.java`

- [ ] Create `NotCompiler` extending `AbstractAstCompiler`
- [ ] Implement `compileNode()` with `ICONST_1 + IXOR` pattern
- [ ] Add unit test in `core/src/test/java/com/elminster/jcp/compile/operator/NotCompilerTest.java`
- [ ] Verify factory registration picks up new compiler
- [ ] Test with simple negation: `!true`, `!false`, `!(x > 5)`

**Acceptance:** All simple logical test cases compile without "cannot get compiler for node: NOT" error.

### Phase 2: Module Function Compile Support
**Scope:** Enable compile-mode resolution of module functions
**Files:**
- `core/src/main/java/com/elminster/jcp/compile/function/FunCallCompiler.java`
- `core/src/main/java/com/elminster/jcp/compile/CompileContext.java`

- [ ] Add module function registry to `CompileContext`
- [ ] Populate registry with base module functions (`Assertions.assertTrue`, etc.)
- [ ] Update `FunCallCompiler.compileNode()` to check module registry before user functions
- [ ] Generate `INVOKESTATIC` bytecode for module function calls
- [ ] Add descriptor mapping for module function signatures
- [ ] Test with isolated `Assertions.assertTrue(true)` call

**Acceptance:** `Assertions.assertTrue()` compiles and executes in compile mode without "Undefined function" error.

### Phase 3: Test Harness for Compiled Programs
**Scope:** Create test infrastructure to execute and validate compiled test scripts
**Files:**
- `core/src/test/java/com/elminster/jcp/minilang/MiniLangAssertionTest.java`
- `core/src/test/java/com/elminster/jcp/minilang/MiniLangSimpleTest.java`

- [ ] Refactor test harness to support dual-mode execution
- [ ] For assertion tests: Compile and invoke `main()`, check for exceptions
- [ ] For simple tests: Wrap logic in function, compile, invoke, check return value
- [ ] Add `@ParameterizedTest` with `Mode.EVAL` and `Mode.COMPILE` parameters
- [ ] Update test count expectations (31 → 45+)

**Acceptance:** Test infrastructure can run same test scripts in both eval and compile modes.

### Phase 4: Dual-Mode Test Coverage
**Scope:** Enable all 14 test scripts in both modes
**Files:** All test scripts in `module/src/test/resources/`

- [ ] Run all 9 assertion test scripts in compile mode
- [ ] Run all 5 simple boolean test scripts in compile mode
- [ ] Fix any remaining compilation or runtime issues
- [ ] Verify test count reaches 45+ (14 scripts × 2 modes + existing tests)
- [ ] Update test output to show mode (eval/compile) in test names

**Acceptance:**
- All assertion tests pass in eval mode (existing)
- All assertion tests pass in compile mode (new)
- All simple tests pass in eval mode (existing)
- All simple tests pass in compile mode (new)
- Total test count ≥ 45

## Acceptance Criteria

### Functional Requirements

- [ ] NOT operator compiles to correct bytecode and produces correct results
- [ ] `Assertions.assertTrue()` compiles and executes in compile mode
- [ ] All 9 assertion test scripts pass in compile mode
- [ ] All 5 simple boolean test scripts pass in compile mode
- [ ] Test harness reports pass/fail for both eval and compile modes
- [ ] No regression in existing eval mode tests

### Non-Functional Requirements

- [ ] Test execution time remains under 10 seconds for full suite
- [ ] Code coverage remains above 80% threshold
- [ ] Generated bytecode is valid (passes `javap -v` verification)
- [ ] Error messages clearly indicate eval vs compile mode failures

## Dependencies

### Prerequisites
- MiniLang syntax and parser (#29) - ✅ Already implemented
- Base module system with `Assertions` class - ✅ Already exists
- AST node for `NotExpression` - ✅ Already exists
- Factory-based compiler registration - ✅ Already exists

### No Blocking Dependencies
All required infrastructure is in place. This is purely additive work.

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Module function signature mismatch | Medium | High | Test with explicit type checking; validate descriptor generation matches actual method signatures |
| NOT operator bytecode pattern incorrect | Low | Medium | Add unit tests comparing eval and compile results; verify with `javap -c` output |
| Test harness complexity | Medium | Medium | Start with simple return-based testing (Phase 3, Option A); defer static field approach if not needed |
| Regression in existing eval tests | Low | High | Run full test suite after each phase; use git bisect if failures occur |
| JaCoCo coverage drops below 80% | Low | Medium | Add tests for new compilers; verify `mvn verify` passes before merging |

## Implementation Notes

### Naming Conventions
- Node: `NotExpression` → `getName()` returns `"NOT"`
- Compiler: Must be named `NotCompiler` for factory lookup
- Tests: `NotCompilerTest`, `ModuleFunctionCompileTest`

### Bytecode Verification
Use these commands to verify generated bytecode:
```bash
# Print bytecode during compilation
compiler.printBytecode(bytecode);

# Decompile class file
javap -c -v output/TestClassName.class
```

### Test Execution Pattern
```java
@ParameterizedTest
@EnumSource(Mode.class)
void testAssertions(Mode mode) {
    Block program = parseTestScript("arithmetic-assertions.minilang");
    if (mode == Mode.EVAL) {
        // Existing eval execution
    } else {
        // New compile execution
        Class<?> clazz = compiler.compileAndLoad(program, "Test");
        clazz.getMethod("main", String[].class).invoke(null, (Object) new String[]{});
    }
}
```

## Related Work

- Issue #29: MiniLang syntax implementation (completed)
- `docs/brainstorms/minilang-reference-implementation.md`: Original design brainstorm
- `core/src/main/java/com/elminster/jcp/compile/`: Existing compiler infrastructure
- `module/base/src/main/java/com/elminster/jcp/module/base/Assertions.java`: Module function definitions

## Success Metrics

**Before:** 31 tests (14 MiniLang tests × 1 mode + 17 other tests)
**After:** 45+ tests (14 MiniLang tests × 2 modes + 17 other tests)

**Coverage targets:** Maintain ≥80% instruction and branch coverage (JaCoCo enforced)

## Next Steps

After plan approval, run `/gw-work` to:
1. Create feature branch `feat/31-minilang-compile-mode-tests`
2. Begin Phase 1 implementation (NOT operator)
3. Commit working phases incrementally
4. Open PR when all phases complete
