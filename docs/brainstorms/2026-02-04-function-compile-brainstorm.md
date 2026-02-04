# Brainstorm: Function Compilation

**Date:** 2026-02-04
**Status:** Ready for planning
**Feature:** 3 of 3 (independent)

## What We're Building

Compile existing function support (already working in eval mode) to JVM bytecode. No new language features - just bytecode generation for existing AST.

## Scope

What already works in eval mode:
- Function declaration with parameters and return type
- Function calls with arguments
- Recursion (Fibonacci test passes)
- Function overloading (by parameter types)
- Return statements

Goal: Make all of this work in compile mode.

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Method type | Static methods | Matches eval (no `this` context) |
| Overloading | Support via name mangling | Match eval behavior |
| Scope | Function-local variables | Each function has own local var table |

## Implementation Notes

### Existing AST Nodes (no changes needed)

- `FunctionDeclaration` / `FunctionDeclarationImpl`
- `FunctionCallExpression`
- `ReturnStatement`
- `ParameterDef`

### Compile Mode Implementation

**Function Declaration → JVM Method:**
```java
// JCP: func add(int a, int b) -> int { return a + b; }
// Generates:
public static int add(int a, int b) {
    return a + b;
}
```

Bytecode generation:
1. `ClassWriter.visitMethod()` with proper descriptor
2. Create new `CompileContext` for function scope
3. Allocate parameters as local variables (index 0, 1, 2...)
4. Compile function body statements
5. `visitMaxs()` and `visitEnd()`

**Function Call → INVOKESTATIC:**
```java
// JCP: add(1, 2)
// Bytecode:
ICONST_1
ICONST_2
INVOKESTATIC ClassName.add(II)I
```

**Return Statement:**
- Already partially implemented (`ReturnCompiler`)
- Need to handle different return types: `IRETURN`, `DRETURN`, `ARETURN`, `RETURN`

### Two-Pass Compilation

**Challenge:** Function may be called before it's declared.

**Solution:** Two-pass approach:
1. **Pass 1:** Collect all function signatures (name, params, return type)
2. **Pass 2:** Generate bytecode for function bodies and calls

### Context Management

Each function needs its own `CompileContext`:
- Fresh local variable table (parameters start at index 0)
- No parent context (unlike blocks within a function)
- Track return type for proper return instruction

## Files to Create/Modify

**New compile classes:**
- `compile/declaration/FunctionDeclarationCompiler.java`
- `compile/function/FunctionCallCompiler.java`

**Modified files:**
- `compile/BytecodeGenerator.java` - two-pass compilation, multiple methods
- `compile/context/CompileContext.java` - track current function info
- `compile/control/ReturnCompiler.java` - handle all return types
- `compile/factory/AstCompilerFactory.java` - register new compilers
- `compile/util/TypeMapper.java` - generate method descriptors

### Method Descriptor Generation

Need to generate JVM method descriptors:
- `(II)I` → two int params, returns int
- `(ID)D` → int and double params, returns double
- `()V` → no params, returns void

## Open Questions

1. Where to store function registry during compilation (in CompileContext)?
2. How to handle function overloading - mangle names or use descriptors?
3. Should functions be compiled into separate methods or inlined?

## Test Cases to Add

```java
// Test 1: Simple function
func add(int a, int b) -> int { return a + b; }
add(1, 2)  // → 3

// Test 2: Recursion (existing eval test)
func fibonacci(int n) -> int {
    if (n <= 1) { return n; }
    return fibonacci(n - 1) + fibonacci(n - 2);
}
fibonacci(10)  // → 55

// Test 3: Void function
func printSum(int a, int b) -> void {
    // just compute, no return value
}
```

## Next Steps

Run `/workflows:plan docs/brainstorms/2026-02-04-function-compile-brainstorm.md`
