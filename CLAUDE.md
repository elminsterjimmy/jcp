# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JCP (Java Command Processor) is a custom programming language implementation with **dual-mode execution**: a tree-walking interpreter (`eval/`) and a JVM bytecode compiler (`compile/`). Both modes share the same AST representation.

## Build Commands

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run all tests
mvn test

# Run tests in core module only
mvn test -pl core

# Run specific test class
mvn test -Dtest=BytecodeGeneratorTest

# Run specific test method
mvn test -Dtest=BytecodeGeneratorTest#testWhileLoop
```

## Module Structure

- **core/** (`jcp-core`) - Main language implementation with AST, interpreter, and compiler
- **aspectj/** (`jcp-aspectj`) - AspectJ-based tracing for debugging evaluation
- **module/** - Extensible module system (skeleton)

## Architecture

### Dual-Mode Execution

Both modes use the same AST nodes but different execution backends:

1. **Interpreter Mode** (`core/src/main/java/com/elminster/jcp/eval/`)
   - Tree-walking evaluation via `EvalVisitor`
   - Runtime state in `EvalContext`
   - Returns `Data<T>` wrapper objects

2. **Compiler Mode** (`core/src/main/java/com/elminster/jcp/compile/`)
   - Generates JVM bytecode using ASM 9.6
   - Compilation state in `CompileContext`
   - Entry point: `JcpCompiler.compile()` or `compileAndLoad()`

### Factory Pattern for Node Processing

Both modes use reflection-based factories that map AST node names to processor classes:

- `AstEvaluatorFactory` → scans for `*Evaluator` classes
- `AstCompilerFactory` → scans for `*Compiler` classes

**Naming convention**: Node `getName()` returns `"plus"` → Factory looks for `PlusEvaluator` / `PlusCompiler`

### Type System

`SystemDataType` enum with inheritance hierarchy:
- `ANY` (root) → `STRING`, `BOOLEAN`, `VOID`
- `ANY` → `NUMERIC` → `INT`
- Array variants: `INT_ARRAY`, `BOOLEAN_ARRAY`, `STRING_ARRAY`

Type compatibility checked via `isCastableTo()` method.

### Context Management

- **EvalContext**: Variable/function maps, loop context for break/continue, context stack for scopes
- **CompileContext**: Local variable tracking with JVM indices, loop labels, parent chain for scopes

## Adding a New Language Feature

1. **Define AST Node** in `ast/` - implement `getName()` for factory lookup
2. **Create Evaluator** in `eval/[category]/[Feature]Evaluator.java` - extends `AbstractAstEvaluator`
3. **Create Compiler** in `compile/[category]/[Feature]Compiler.java` - extends `AbstractAstCompiler`
4. **Write tests** for both modes in `core/src/test/java/com/elminster/jcp/`
5. **Update TypeMapper** if adding a new type (descriptors, load/store/return opcodes)

## Key Entry Points

**Interpreter:**
```java
Block program = new BlockImpl();
// ... add statements
EvalContext context = new RootEvalContext();
new EvalVisitor(context).visit(program);
Data result = context.getVariable("varName");
```

**Compiler:**
```java
Block program = new BlockImpl();
// ... add statements
JcpCompiler compiler = new JcpCompiler();
Class<?> clazz = compiler.compileAndLoad(program, "ClassName");
clazz.getMethod("main", String[].class).invoke(null, (Object) new String[]{});
```

## Debugging

- Use `compiler.printBytecode(bytecode)` to view generated ASM instructions
- Use `javap -c -v output/ClassName.class` to verify compiled output
- AspectJ module intercepts `Evaluable.eval()` calls for tracing

## Current Development

Branch `compiler-with-asm` - active work on bytecode generation. See `docs/brainstorms/` for planned features:
- Function compilation to bytecode
- Double/floating-point type support
- Struct/record types
