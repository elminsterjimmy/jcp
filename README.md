# JCP - Java Compiler Platform

![Build Status](https://img.shields.io/github/actions/workflow/status/elminsterjimmy/jcp/maven.yml?branch=master)
![Java Version](https://img.shields.io/badge/Java-11%2B-blue)
![License](https://img.shields.io/badge/License-TBD-lightgrey)

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

JCP sits between your DSL frontend and the JVM backend:

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           YOUR DSL FRONTEND                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                  │
│  │   Source    │───▶│   Lexer     │───▶│   Parser    │                  │
│  │   Code      │    │  (ANTLR)    │    │  (ANTLR)    │                  │
│  └─────────────┘    └─────────────┘    └──────┬──────┘                  │
└──────────────────────────────────────────────┬───────────────────────────┘
                                               │
                              Build JCP AST from parse tree
                                               │
┌──────────────────────────────────────────────▼───────────────────────────┐
│                           JCP MIDDLEWARE                                 │
│                                                                          │
│                         ┌─────────────┐                                  │
│                         │   JCP AST   │                                  │
│                         └──────┬──────┘                                  │
│                                │                                         │
│            ┌───────────────────┴───────────────────┐                    │
│            │                                       │                    │
│     ┌──────▼──────┐                         ┌──────▼──────┐             │
│     │  EVAL MODE  │                         │COMPILE MODE │             │
│     │ Interpreter │                         │  Bytecode   │             │
│     └──────┬──────┘                         └──────┬──────┘             │
│            │                                       │                    │
└────────────┼───────────────────────────────────────┼────────────────────┘
             │                                       │
      ┌──────▼──────┐                         ┌──────▼──────┐
      │   Result    │                         │  JVM Class  │
      └─────────────┘                         └──────┬──────┘
                                                     │
                                              ┌──────▼──────┐
                                              │     JVM     │
                                              │  Execution  │
                                              └─────────────┘
```

## Integrating Your DSL

To use JCP with your custom DSL:

1. **Define your language grammar** using ANTLR, JavaCC, or another parser generator
2. **Build JCP AST nodes** from your parse tree in a visitor/listener
3. **Execute via JCP** using either eval mode (interpreter) or compile mode (bytecode)

```java
// Example: Converting your parse tree to JCP AST
public class MyDslVisitor extends MyDslBaseVisitor<AstNode> {

    @Override
    public AstNode visitAddExpr(MyDslParser.AddExprContext ctx) {
        Expression left = (Expression) visit(ctx.left);
        Expression right = (Expression) visit(ctx.right);
        return new Plus(left, right);  // JCP AST node
    }

    @Override
    public AstNode visitProgram(MyDslParser.ProgramContext ctx) {
        Block block = new BlockImpl();
        for (var stmt : ctx.statement()) {
            block.addStatement((Statement) visit(stmt));
        }
        return block;
    }
}
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
