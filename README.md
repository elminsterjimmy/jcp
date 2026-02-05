# JCP - Java Command Processor

![Build Status](https://img.shields.io/github/actions/workflow/status/elminsterjimmy/jcp/maven.yml?branch=master)
![Java Version](https://img.shields.io/badge/Java-11%2B-blue)
![License](https://img.shields.io/badge/License-TBD-lightgrey)

A custom programming language implementation with **dual-mode execution**: a tree-walking interpreter and a JVM bytecode compiler. Both modes share the same AST representation.

## Features

- **Dual Execution Modes**: Run programs via interpreter (eval) or compile to JVM bytecode
- **Unified AST**: Same abstract syntax tree works with both execution backends
- **Type System**: Hierarchical type system with `ANY`, `NUMERIC`, `INT`, `BOOLEAN`, `STRING`, and more
- **Custom Structs**: Define and use custom data structures
- **Module System**: Extensible module architecture for adding Java-backed functions
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

## Architecture

JCP implements a dual-mode execution architecture:

```
                    ┌─────────────┐
                    │  JCP Script │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   Parser    │
                    │  (Shared)   │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │     AST     │
                    └──────┬──────┘
                           │
           ┌───────────────┴───────────────┐
           │                               │
    ┌──────▼──────┐                 ┌──────▼──────┐
    │  EVAL MODE  │                 │COMPILE MODE │
    │ Interpreter │                 │  Bytecode   │
    └──────┬──────┘                 └──────┬──────┘
           │                               │
    ┌──────▼──────┐                 ┌──────▼──────┐
    │   Result    │                 │  JVM Class  │
    └─────────────┘                 └─────────────┘
```

### Eval Mode (Interpreter)

Tree-walking execution that directly evaluates AST nodes:

```java
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.impl.BlockImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.Data;

// Build your program AST
Block program = new BlockImpl();
// ... add statements to program

// Execute via interpreter
EvalContext context = new RootEvalContext();
new EvalVisitor(context).visit(program);

// Retrieve results
Data result = context.getVariable("varName");
```

### Compile Mode (Bytecode)

Generates JVM bytecode using ASM, producing executable Java classes:

```java
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.impl.BlockImpl;
import com.elminster.jcp.compile.JcpCompiler;

// Build your program AST
Block program = new BlockImpl();
// ... add statements to program

// Compile to bytecode and load
JcpCompiler compiler = new JcpCompiler();
Class<?> clazz = compiler.compileAndLoad(program, "MyProgram");

// Execute compiled code
clazz.getMethod("main", String[].class)
     .invoke(null, (Object) new String[]{});
```

## Language Features

### Variables and Types

```
int x = 10;
string name = "JCP";
boolean flag = true;
```

### Control Flow

```
if (x > 5) {
    // ...
} else {
    // ...
}

while (x > 0) {
    x = x - 1;
}
```

### Functions

```
func add(int a, int b) -> int {
    return a + b;
}

int result = add(3, 5);
```

### Structs

```
struct Point {
    int x;
    int y;
}

Point p = Point(10, 20);
int xVal = p.x;
```

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

## Debugging

### Bytecode Inspection

```java
JcpCompiler compiler = new JcpCompiler();
byte[] bytecode = compiler.compile(program, "ClassName");
compiler.printBytecode(bytecode);  // Print ASM instructions
```

### Class File Analysis

```bash
javap -c -v output/ClassName.class
```

### AspectJ Tracing

The AspectJ module intercepts `Evaluable.eval()` calls for tracing interpreter execution.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feat/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feat/amazing-feature`)
5. Open a Pull Request

### Adding New Language Features

1. **Define AST Node** in `ast/` - implement `getName()` for factory lookup
2. **Create Evaluator** in `eval/[category]/[Feature]Evaluator.java`
3. **Create Compiler** in `compile/[category]/[Feature]Compiler.java`
4. **Write tests** for both modes in `core/src/test/java/`

### Naming Conventions

- Evaluators: `[Feature]Evaluator.java` (e.g., `PlusEvaluator`)
- Compilers: `[Feature]Compiler.java` (e.g., `PlusCompiler`)
- AST node `getName()` returns lowercase (e.g., `"plus"`)

## License

TBD

## References

- [CLAUDE.md](CLAUDE.md) - Developer guide and project conventions
- [Architecture Documentation](docs/architecture/) - Detailed system design
- [Examples](docs/examples/) - Language usage examples
