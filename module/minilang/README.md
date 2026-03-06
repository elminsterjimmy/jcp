# MiniLang - JCP Reference DSL

MiniLang is a minimalist reference DSL demonstrating how to integrate custom Domain-Specific Languages with the Java Compiler Platform (JCP). It serves as both documentation and a working example for DSL authors.

## Overview

MiniLang demonstrates the complete integration pattern:

1. **Define syntax** using ANTLR4 grammar
2. **Convert parse tree** to JCP AST nodes
3. **Execute** in dual modes: interpreter (eval) and bytecode compiler

This implementation validates JCP's core guarantee: **eval and compile modes produce identical results**.

## Language Features

### Variables and Types

```minilang
let x: int = 10
let y: double = 3.14
let name: string = "MiniLang"
let flag: boolean = true
```

Supported types: `int`, `double`, `boolean`, `string`, `void`

### Expressions

**Arithmetic:**
```minilang
let sum: int = x + y
let product: int = x * y
let difference: int = x - y
let quotient: int = x / y
let remainder: int = x % y
```

**Comparison:**
```minilang
x < 5
y > 10
x <= 5
y >= 10
x == y
x != y
```

**Logical:**
```minilang
flag && true
flag || false
!flag
```

### Functions

```minilang
func add(a: int, b: int) -> int {
    return a + b
}

let result: int = add(5, 3)
```

**Recursion:**
```minilang
func factorial(n: int) -> int {
    if n <= 1 {
        return 1
    }
    return n * factorial(n - 1)
}
```

### Control Flow

**If/Else:**
```minilang
if x > 5 {
    result = 1
} else {
    result = 0
}
```

**While Loops:**
```minilang
let counter: int = 0
while counter < 5 {
    counter = counter + 1
}
```

**Break and Continue:**
```minilang
while n < 10 {
    if n == 5 {
        break
    }
    n = n + 1
}

while i < 5 {
    i = i + 1
    if i == 3 {
        continue
    }
    sum = sum + i
}
```

## Project Structure

```
module/minilang/
├── pom.xml                                 # Maven configuration
├── src/
│   ├── main/
│   │   ├── antlr4/
│   │   │   └── com/elminster/minilang/
│   │   │       └── MiniLang.g4             # ANTLR4 grammar
│   │   ├── java/
│   │   │   └── com/elminster/minilang/
│   │   │       └── ParseTreeConverter.java # AST converter
│   │   └── resources/
│   │       └── examples/
│   │           ├── 01-basics.minilang      # Variables and expressions
│   │           ├── 02-functions.minilang   # Functions and recursion
│   │           └── 03-control-flow.minilang # Control structures
│   └── test/
│       └── java/
│           └── com/elminster/minilang/
│               └── MiniLangIntegrationTest.java # Dual-mode tests
└── README.md
```

## Using MiniLang

### Build

```bash
# Build MiniLang module
mvn clean install -pl module/minilang

# Run tests
mvn test -pl module/minilang
```

### Execute Examples

```java
// Load and parse MiniLang source
String source = Files.readString(Path.of("examples/01-basics.minilang"));
ParseTreeConverter converter = new ParseTreeConverter("01-basics.minilang", source);
Block program = converter.parse(source);

// Execute in interpreter mode
EvalContext ctx = new RootEvalContext();
new EvalVisitor(ctx).visit(program);

// Or compile to bytecode
JcpCompiler compiler = new JcpCompiler();
Class<?> clazz = compiler.compileAndLoad(program, "Example");
clazz.getMethod("main", String[].class).invoke(null, (Object) new String[]{});
```

## Creating Your Own DSL

MiniLang demonstrates the essential steps for integrating any DSL with JCP:

### Step 1: Define Grammar

Create an ANTLR4 grammar (`.g4` file) defining your DSL syntax:

```antlr4
grammar MyDSL;

program: statement* EOF ;

statement
    : varDecl
    | expression
    ;

varDecl: 'var' ID '=' expression ;
```

### Step 2: Implement Converter

Extend `BaseVisitor` to convert ANTLR parse tree to JCP AST:

```java
public class MyDslConverter extends MyDSLBaseVisitor<Node> {

    @Override
    public Statement visitVarDecl(MyDSLParser.VarDeclContext ctx) {
        String name = ctx.ID().getText();
        Expression value = (Expression) visit(ctx.expression());
        return new VariableDeclarationImpl(name, SystemDataType.ANY, value);
    }
}
```

### Step 3: Attach Source Locations

Always attach source locations for debugging:

```java
private void attachLocation(Node node, ParserRuleContext ctx) {
    Token start = ctx.getStart();
    Token stop = ctx.getStop() != null ? ctx.getStop() : start;

    SourceLocation location = SourceLocation.span(
        sourceFile,
        start.getLine(),
        start.getCharPositionInLine() + 1,
        stop.getLine(),
        stop.getCharPositionInLine() + stop.getText().length(),
        sourceLines[start.getLine() - 1]
    );

    ((AbstractNode) node).setLocation(location);
}
```

### Step 4: Map Types

Map your DSL types to JCP's type system:

```java
private DataType resolveType(String typeName) {
    switch (typeName.toLowerCase()) {
        case "int": return SystemDataType.INT;
        case "string": return SystemDataType.STRING;
        case "bool": return SystemDataType.BOOLEAN;
        default: return SystemDataType.ANY;
    }
}
```

### Step 5: Test Both Modes

Write integration tests validating dual-mode execution:

```java
@Test
void testDualModeExecution() throws Exception {
    Block program = converter.parse(source);

    // Test eval mode
    assertDoesNotThrow(() -> {
        EvalContext ctx = new RootEvalContext();
        new EvalVisitor(ctx).visit(program);
    });

    // Test compile mode
    assertDoesNotThrow(() -> {
        JcpCompiler compiler = new JcpCompiler();
        Class<?> clazz = compiler.compileAndLoad(program, "Test");
        clazz.getMethod("main", String[].class).invoke(null, (Object) new String[]{});
    });
}
```

## Design Principles

### KISS (Keep It Simple, Stupid)

- **Inline type resolution**: No separate TypeResolver class
- **Single converter class**: All logic in ParseTreeConverter
- **Simple visitor pattern**: Direct mapping from parse tree to AST

### Minimal Abstractions

- No complex type system - uses JCP's built-in types
- No custom intermediate representations
- Direct ANTLR → JCP AST conversion

### Clear Separation

```
Source Code → ANTLR Lexer → Tokens → ANTLR Parser → Parse Tree
                                                          ↓
                                                 ParseTreeConverter
                                                          ↓
                                                      JCP AST
                                                          ↓
                                               Eval Mode | Compile Mode
                                                          ↓
                                                      Execution
```

## Implementation Notes

### Function Calls

All function calls use `FunctionCallExpression`, whether they're user-defined or module functions:

```java
FunctionCallExpression call = new FunctionCallExpression(
    Identifier.fromName(funcName),
    args
);
```

JCP's function lookup system resolves both simple names (`add`) and dotted names (`Assertions.assertTrue`).

### Literal Wrapping

JCP requires literals to be wrapped in `LiteralExpression`:

```java
// Correct
LiteralExpression expr = LiteralExpression.of(IntLiteral.of(42));

// Incorrect - will fail at runtime
Expression expr = IntLiteral.of(42);
```

### Variable References

Use `VariableExpression` for variable references:

```java
// Correct
VariableExpression expr = VariableExpression.of("x");

// Incorrect - would be treated as string literal
IdentifierExpression expr = new IdentifierExpression("x");
```

## Known Limitations

1. **Nested loop scoping**: Variables declared inside nested while loops may cause "already declared" errors due to JCP core scoping limitations. Workaround: declare loop variables outside the loop.

2. **Module functions in compile mode**: Module function assertions (`Assertions.assertTrue`) work in eval mode but may not be fully supported in compile mode yet.

## Testing

### Test Coverage

JaCoCo enforces **80% minimum coverage** on both instructions and branches:

```bash
mvn verify -pl module/minilang
open module/minilang/target/site/jacoco/index.html
```

### Dual-Mode Validation

All examples are tested in both modes:

```bash
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Examples

See `src/main/resources/examples/` for complete working examples:

- **01-basics.minilang**: Variables, arithmetic, strings, booleans
- **02-functions.minilang**: Function declarations, recursion (factorial, fibonacci)
- **03-control-flow.minilang**: If/else, while loops, break, continue

## Contributing

When extending MiniLang:

1. Update the grammar (`MiniLang.g4`)
2. Add visitor methods in `ParseTreeConverter`
3. Add example programs demonstrating new features
4. Update integration tests
5. Ensure 80%+ test coverage

## License

Part of the JCP (Java Compiler Platform) project.

## References

- [ANTLR4 Documentation](https://github.com/antlr/antlr4/blob/master/doc/index.md)
- [JCP Core README](../../core/README.md)
- [Integration Guide](../../CLAUDE.md)
