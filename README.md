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

## Error Handling with Source Location

JCP provides enhanced error messages with GCC-style source location tracking. When errors occur, you'll see exactly where in your source code the problem is:

```
RuntimeError: Division by zero at math.jcp:15:12
math.jcp:15:12
  15 |   return a / b;
              ^~~~~
```

### Integrating Source Location with ANTLR

When building JCP AST nodes from your ANTLR parse tree, attach source location information from the parser context:

```java
import com.elminster.jcp.ast.SourceLocation;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class MyDslVisitor extends MyDslBaseVisitor<AstNode> {

    private final String sourceFile;
    private final String[] sourceLines;  // Original source split by lines

    public MyDslVisitor(String sourceFile, String source) {
        this.sourceFile = sourceFile;
        this.sourceLines = source.split("\n");
    }

    /**
     * Creates a SourceLocation from an ANTLR parser context.
     */
    private SourceLocation locationFrom(ParserRuleContext ctx) {
        Token start = ctx.getStart();
        Token stop = ctx.getStop();
        int line = start.getLine();
        String sourceLine = (line > 0 && line <= sourceLines.length)
            ? sourceLines[line - 1] : null;

        if (start.getLine() == stop.getLine()) {
            // Single-line span
            return SourceLocation.span(
                sourceFile,
                line, start.getCharPositionInLine() + 1,
                line, stop.getCharPositionInLine() + stop.getText().length(),
                sourceLine
            );
        } else {
            // Multi-line - just use start position
            return SourceLocation.of(
                sourceFile,
                line, start.getCharPositionInLine() + 1,
                sourceLine
            );
        }
    }

    @Override
    public AstNode visitAddExpr(MyDslParser.AddExprContext ctx) {
        Expression left = (Expression) visit(ctx.left);
        Expression right = (Expression) visit(ctx.right);
        Plus node = new Plus(left, right);
        node.setLocation(locationFrom(ctx));  // Attach source location
        return node;
    }

    @Override
    public AstNode visitVariableDecl(MyDslParser.VariableDeclContext ctx) {
        String name = ctx.ID().getText();
        Expression init = (Expression) visit(ctx.expression());
        VariableDeclaration decl = new VariableDeclaration(
            Identifier.fromName(name),
            init
        );
        decl.setLocation(locationFrom(ctx));
        return decl;
    }
}
```

### Handling JCP Exceptions

All JCP exceptions extend `JcpException` and include source location information:

```java
try {
    new EvalVisitor(context).visit(program);
} catch (JcpException e) {
    // Simple message with location suffix
    System.err.println(e.getMessage());
    // Output: Division by zero at math.jcp:15:12

    // Full message with source context
    System.err.println(e.getFormattedMessage());
    // Output:
    // Division by zero at math.jcp:15:12
    // math.jcp:15:12
    //   15 |   return a / b;
    //               ^~~~~

    // Access location programmatically
    SourceLocation loc = e.getLocation();
    if (loc != null) {
        System.err.printf("Error at line %d, column %d%n",
            loc.getLine(), loc.getColumn());
    }
}
```

### Exception Hierarchy

```
JcpException (base)
├── EvaluationException (interpreter errors)
│   ├── DeclarationException
│   │   ├── AlreadyDeclaredException
│   │   └── UndeclaredException
│   ├── CannotCastException
│   └── FunctionAmbiguityException
└── CompileException (bytecode compiler errors)
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

### Programmatic Debugger (Interpreter Mode)

JCP provides a programmatic debugger API for stepping through code, setting breakpoints, and inspecting variables during interpretation.

#### Basic Usage

```java
import com.elminster.jcp.debug.*;
import com.elminster.jcp.eval.context.RootEvalContext;

// Create debugger and visitor
DefaultDebugger debugger = new DefaultDebugger();
EvalContext context = new RootEvalContext();
DebuggingEvalVisitor visitor = new DebuggingEvalVisitor(context, debugger);

// Set breakpoints (filepath is required)
Breakpoint bp1 = debugger.setBreakpoint("main.jcp", 10);        // Line 10
Breakpoint bp2 = debugger.setBreakpoint("main.jcp", 15, 5);     // Line 15, column 5
Breakpoint bp3 = debugger.setBreakpoint(node);                  // At specific AST node

// Add event listener for debugging events
debugger.addListener(new DebugEventListener() {
    public void onBreakpointHit(Node node, Breakpoint breakpoint) {
        System.out.println("Paused at " + breakpoint);
    }
    public void onStepComplete(Node node) {
        System.out.println("Step completed at line " + debugger.getCurrentLine());
    }
    public void onStateChanged(DebugState oldState, DebugState newState) {
        System.out.println("State: " + oldState + " -> " + newState);
    }
    public void onError(Exception error) {
        System.err.println("Debug error: " + error.getMessage());
    }
});

// Start debugging in separate thread
new Thread(() -> visitor.debug(program)).start();

// Wait for breakpoint (with timeout handling - see below)
waitForPause(debugger, 30_000);

// Inspect variables at breakpoint
Map<String, Data<?>> vars = debugger.getVariables();
vars.forEach((name, value) -> System.out.println(name + " = " + value));

// Step through code
debugger.stepOver();    // Execute current line, pause at next
debugger.stepInto();    // Enter function calls
debugger.stepOut();     // Run until function returns

// Continue to next breakpoint
debugger.continueExecution();

// Remove breakpoint
debugger.removeBreakpoint(bp1);

// Stop debugging
debugger.stop();
```

#### Timeout Handling

The debugger waits indefinitely when paused at a breakpoint. You must implement timeout handling in your code:

```java
/**
 * Waits for debugger to pause with timeout.
 *
 * @param debugger the debugger instance
 * @param timeoutMs maximum wait time in milliseconds
 * @throws RuntimeException if timeout occurs
 */
public static void waitForPause(DefaultDebugger debugger, long timeoutMs)
        throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;

    while (!debugger.isPaused()) {
        if (System.currentTimeMillis() > deadline) {
            debugger.stop();  // Force stop on timeout
            throw new RuntimeException("Debugger timeout - breakpoint not hit within "
                + timeoutMs + "ms");
        }
        Thread.sleep(10);
    }
}
```

Alternative using `CompletableFuture`:

```java
CompletableFuture<Void> debugFuture = CompletableFuture.runAsync(
    () -> visitor.debug(program)
);

try {
    debugFuture.get(30, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    debugger.stop();
    throw new RuntimeException("Debug session timed out");
}
```

Alternative using event listener with `CountDownLatch`:

```java
CountDownLatch breakpointLatch = new CountDownLatch(1);

debugger.addListener(new DebugEventListener() {
    public void onBreakpointHit(Node node, Breakpoint bp) {
        breakpointLatch.countDown();
    }
    // ... other methods
});

new Thread(() -> visitor.debug(program)).start();

if (!breakpointLatch.await(30, TimeUnit.SECONDS)) {
    debugger.stop();
    throw new RuntimeException("Timeout waiting for breakpoint");
}
```

#### Debugger API Summary

| Method | Description |
|--------|-------------|
| `setBreakpoint(String file, int line)` | Set breakpoint at file and line |
| `setBreakpoint(String file, int line, int column)` | Set breakpoint at file, line, and column |
| `setBreakpoint(Node node)` | Set breakpoint at AST node |
| `removeBreakpoint(Breakpoint bp)` | Remove a breakpoint |
| `getBreakpoints()` | Get all breakpoints |
| `getBreakpointsAt(int line)` | Get breakpoints at a line |
| `stepOver()` | Execute current line, pause at next |
| `stepInto()` | Enter function calls |
| `stepOut()` | Run until function returns |
| `continueExecution()` | Continue to next breakpoint |
| `stop()` | Stop debugging entirely |
| `detach()` | Detach but keep breakpoints |
| `getVariables()` | Get variables in current scope |
| `getStackTrace()` | Get call stack |
| `isPaused()` | Check if paused |
| `isAttached()` | Check if attached |
| `getCurrentLine()` | Get current line number |
| `getCurrentNode()` | Get current AST node |

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

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## References

- [CLAUDE.md](CLAUDE.md) - Developer guide and project conventions
- [Architecture Documentation](docs/architecture/) - Detailed system design
- [Examples](docs/examples/) - Language usage examples
