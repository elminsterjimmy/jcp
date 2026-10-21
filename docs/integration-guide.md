[← Back to README](../README.md)

# Integration Guide

This guide covers how to connect your custom DSL to JCP and execute programs using either the interpreter or the bytecode compiler.

For the DSL syntax reference (types, control flow, functions, structs), see the [Language Reference](language-reference.md).

## Overview

To use JCP with your custom DSL:

1. **Define your language grammar** using ANTLR, JavaCC, or another parser generator
2. **Build JCP AST nodes** from your parse tree in a visitor/listener
3. **Execute via JCP** using either eval mode (interpreter) or compile mode (bytecode)

For a detailed look at the internal architecture (function system, method dispatch, bytecode generation), see the [Architecture Documentation](architecture/jcp-function-architecture.md).

## Building AST from Your Parse Tree

Convert your parser's output into JCP AST nodes using a visitor pattern:

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

## Eval Mode (Interpreter)

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

## Compile Mode (Bytecode)

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

## Module Registration

Register external Java classes as callable types within JCP. The base module classes (Math, Strings, Arrays, Convert, IO) are registered automatically.

```java
// Compile mode
BytecodeGenerator generator = new BytecodeGenerator("MyProgram");
generator.registerExternalClass(com.google.common.base.Strings.class, "guava");

// Eval mode
ClassConverter.registerClass(com.google.common.base.Strings.class, context, "guava");
```

Each class is associated with a module name (e.g. `"guava"`, `"base"`). When two modules register classes with the same simple name, users disambiguate with the `module::Type` prefix — see [Language Reference: Modules](language-reference.md#modules-and-type-disambiguation).

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

## See Also

- [Language Reference](language-reference.md) — DSL syntax: types, control flow, functions, structs, modules
- [Architecture](architecture/jcp-function-architecture.md) — Function system internals, method dispatch
- [Debugger API](debugger.md) — Breakpoints, stepping, variable inspection
- [Contributing](contributing.md) — How to add features and submit PRs
