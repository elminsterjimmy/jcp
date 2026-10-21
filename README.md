# JCP - Java Compiler Platform

![Build Status](https://img.shields.io/github/actions/workflow/status/elminsterjimmy/jcp/maven.yml?branch=master)
![Coverage](https://raw.githubusercontent.com/elminsterjimmy/jcp/master/.github/badges/jacoco.svg)
![Branches](https://raw.githubusercontent.com/elminsterjimmy/jcp/master/.github/badges/branches.svg)
![Java Version](https://img.shields.io/badge/Java-11%2B-blue)
![License](https://img.shields.io/badge/License-MIT-green)

A **middleware platform** that bridges Domain-Specific Languages (DSLs) and the JVM. JCP handles the semantic analysis, type checking, and code generation phases of compilation, allowing you to focus on defining your language's syntax using tools like ANTLR while leveraging the JVM for execution and optimization.

## What is JCP?

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Your DSL      │     │      JCP        │     │      JVM        │
│                 │     │   (Middleware)  │     │                 │
│  - Syntax       │────▶│  - AST          │────▶│  - IR/Bytecode  │
│  - Lexer        │     │  - Type System  │     │  - Optimization │
│  - Parser       │     │  - Eval/Compile │     │  - Execution    │
│  (ANTLR, etc.)  │     │  - Modules      │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

**You define**: Lexical analysis and parsing (using ANTLR, JavaCC, or hand-written parsers)
**JCP provides**: AST representation, type system, interpreter, and bytecode compiler
**JVM handles**: IR optimization, JIT compilation, and runtime execution

## Features

- **DSL Middleware**: Connect your custom parser to JVM execution with minimal effort
- **Dual Execution Modes**: Run via tree-walking interpreter (eval) or compile to JVM bytecode
- **Unified AST**: Same abstract syntax tree works with both execution backends
- **Type System**: Hierarchical type system with `ANY`, `NUMERIC`, `INT`, `BOOLEAN`, `STRING`, and more
- **Custom Structs**: Define and use custom data structures
- **Module System**: Extensible module architecture for adding Java-backed functions, with `module::Type` disambiguation when names collide
- **Function Overloading**: Type-safe function resolution with parameter type matching

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6+

### Build

```bash
git clone https://github.com/elminsterjimmy/jcp.git
cd jcp
mvn clean install
```

### Run Tests

```bash
mvn test
```

## Documentation

| Topic | Description |
|-------|-------------|
| [Language Reference](docs/language-reference.md) | Types, control flow, functions, structs, modules |
| [Integration Guide](docs/integration-guide.md) | Connect your DSL to JCP via ANTLR, eval/compile modes, error handling |
| [Architecture](docs/architecture/jcp-function-architecture.md) | Function system internals, method dispatch, bytecode generation |
| [Debugger API](docs/debugger.md) | Breakpoints, stepping, variable inspection |
| [Contributing](docs/contributing.md) | How to add features and submit PRs |

## Project Structure

```
jcp/
├── core/           # Main language implementation
│   ├── ast/        # Abstract syntax tree nodes
│   ├── eval/       # Tree-walking interpreter
│   ├── compile/    # JVM bytecode compiler (ASM)
│   └── module/     # Module system
├── aspectj/        # AspectJ-based tracing for debugging
├── module/         # Extensible module system
└── docs/           # Documentation
    ├── architecture/   # System design docs
    ├── brainstorms/    # Feature planning
    └── examples/       # Usage examples
```

## Building from Source

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

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## References

- [CLAUDE.md](CLAUDE.md) - Developer guide and project conventions
- [Architecture Documentation](docs/architecture/) - Detailed system design
- [Examples](docs/examples/) - Language usage examples
