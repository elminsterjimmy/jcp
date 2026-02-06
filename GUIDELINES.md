# JCP Implementation Guidelines

Guidelines for maintaining the JCP (Java Compiler Platform) codebase, optimized for both human contributors and AI coding assistants.

## Quick Reference: Adding a New Language Feature

| Step | Action | File Pattern |
|------|--------|-------------|
| 1 | Define AST node | `ast/[category]/[Feature]Impl.java` |
| 2 | Implement `getName()` | Return `"FEATURE"` (uppercase) |
| 3 | Create evaluator | `eval/[category]/[Feature]Evaluator.java` |
| 4 | Create compiler | `compile/[category]/[Feature]Compiler.java` |
| 5 | Write tests | Both eval AND compile modes |

**No factory registration needed** - reflection handles discovery automatically.

---

## How to Read This Document

- **MUST** / **MUST NOT** - Hard rules. Violations are errors.
- **SHOULD** / **SHOULD NOT** - Guidelines. Exceptions require justification.
- **MAY** - Optional. Use judgment.

---

## KISS Principles

### MUST: Keep implementations minimal

Concrete processor classes SHOULD be 15-50 lines. If a class exceeds 100 lines, extract a base class.

**Good Example** - `core/src/main/java/com/elminster/jcp/compile/operator/arithmetic/PlusCompiler.java` (21 lines):

```java
public class PlusCompiler extends ArithmeticCompiler {
    public PlusCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected void emitOperation(MethodVisitor mv) {
        mv.visitInsn(useDouble ? Opcodes.DADD : Opcodes.IADD);
    }
}
```

### MUST NOT: Create interfaces for single implementations

**Bad:**
```java
interface PlusOperation { int add(int a, int b); }
class PlusOperationImpl implements PlusOperation { ... }
```

**Good:**
```java
class PlusCompiler extends ArithmeticCompiler { ... }
```

Only create an interface when you have 2+ implementations.

### MUST NOT: Optimize prematurely

Prove performance need with benchmarks before adding complexity.

### SHOULD: Prefer explicit over clever

When performance is equivalent, choose readability over cleverness.

---

## Single Responsibility Principle (S)

### MUST: One class = one reason to change

Each processor class handles exactly one AST node type. This is why `PlusEvaluator` and `PlusCompiler` are separate classes.

### MUST: Parallel evaluator/compiler structure

Every AST node that needs processing MUST have both:
- `[Feature]Evaluator.java` in `eval/[category]/`
- `[Feature]Compiler.java` in `compile/[category]/`

**Example:** `PlusEvaluator` handles interpreter mode, `PlusCompiler` handles bytecode generation.

---

## Open/Closed Principle (O)

### MUST: Extend via new classes, not modifications

To add a new language feature, create new classes. Never modify existing factories.

### MUST: Follow naming conventions for auto-discovery

The reflection-based factory discovers processors by naming convention:

1. AST node `getName()` returns: `"PLUS"`, `"WHILE"`, etc.
2. Factory normalizes to PascalCase: `"Plus"`, `"While"`
3. Factory appends suffix: `"PlusEvaluator"`, `"WhileCompiler"`

**File:** `core/src/main/java/com/elminster/jcp/eval/factory/AstEvaluatorFactory.java`

```java
private static String getEvaluatorClassName(String name) {
    return normalize(name) + "Evaluator";
}
```

---

## Liskov Substitution Principle (L)

### MUST: Subtypes must be substitutable

All `Data<T>` implementations (IntegerData, StringData, etc.) MUST work interchangeably where `Data` is expected.

**Example:** `SystemDataType.INT` must be usable anywhere `SystemDataType.NUMERIC` is expected.

### SHOULD: Use @Override annotation

Always annotate overridden methods to catch signature mismatches at compile time.

---

## Interface Segregation Principle (I)

### MUST: Keep interfaces focused (single capability)

**Good Examples:**

| Interface | Methods | File |
|-----------|---------|------|
| `Node` | 1 (`getName()`) | `core/src/main/java/com/elminster/jcp/ast/Node.java` |
| `Evaluable` | 1 (`eval()`) | `core/src/main/java/com/elminster/jcp/eval/Evaluable.java` |
| `Compilable` | 1 (`compile()`) | `core/src/main/java/com/elminster/jcp/compile/Compilable.java` |

### MUST NOT: Fat interfaces

Never create interfaces requiring clients to implement unused methods.

---

## Dependency Inversion Principle (D)

### SHOULD: Depend on abstractions

Processors depend on `Node` interface, not concrete node classes.

```java
public class PlusCompiler extends ArithmeticCompiler {
    public PlusCompiler(Node astNode) {  // Interface, not concrete
        super(astNode);
    }
}
```

### SHOULD: Accept context via parameters

Pass `EvalContext` or `CompileContext` as method parameters, not via constructors.

---

## Compiler-Specific Patterns

### MUST: Dual Registration for Custom Types

When implementing declaration compilers for custom types (structs, enums):

```java
// BOTH registrations are required
ctx.addDataType(typeMetadata);         // For compile-time type checking
ctx.addGeneratedClass(name, bytecode); // For runtime class loading
```

**Why:** Registering only bytecode causes "Unknown type" errors downstream.

**Source:** `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md`

### MUST: Follow 3-Step Type Resolution

When resolving type names during compilation:

1. Check system types first (fast path for primitives)
2. Check custom types from context: `ctx.getDataType(typeName)`
3. Fall back to `SystemDataType.ANY` only if neither match

**Why:** Skipping step 2 breaks struct field access and method calls.

### MUST: Maintain Eval/Compile Mode Parity

When implementing compile mode for an eval feature:

1. Check what state changes the evaluator makes to `EvalContext`
2. Ensure compiler makes equivalent changes to `CompileContext`
3. Test BOTH modes in integration tests

### MUST: Visitors delegate to factories

Use factory lookup for node processing. Never use `instanceof` checks in visitors.

```java
// Good - factory handles dispatch
Compilable compiler = AstCompilerFactory.getCompiler(node);
compiler.compile(mv, ctx);

// Bad - manual dispatch
if (node instanceof PlusExpression) { ... }
```

### MUST: Create child contexts for nested scopes

```java
CompileContext childCtx = ctx.createChildContext();
// ... compile nested block
```

Variable lookup follows: current context → parent chain (lexical scoping).

---

## Anti-Patterns to Avoid

### God Context Objects

**Warning signs:**
- Context class > 500 lines
- Context handles > 5 distinct concerns

**Current state:** `CompileContext` is ~476 lines - watch this boundary.

### Mutable AST Nodes

**Bad:** AST nodes with mutable fields
**Good:** `IntLiteral.of()` - functional interface, no mutation
**Risk:** Concurrent evaluation can cause race conditions

### Incomplete TODO Comments

**Bad:**
```java
// TODO
// TODO: fix this
```

**Good:**
```java
// TODO(owner, P1): Add type checking - prevents ClassCastException at runtime
```

### Deep Context Chains

**SHOULD:** Limit parent context chain to 3 levels.
**Risk:** O(n) lookup time for deeply nested scopes.
**Mitigation:** Cache frequently accessed symbols at root.

### Silent Factory Failures

Factory MUST throw clear exceptions with node name when processor not found.

---

## References

### Internal Documentation
- Architecture: [CLAUDE.md](CLAUDE.md)
- Documented learnings: `docs/solutions/`

### Key Files
- Factory pattern: `core/src/main/java/com/elminster/jcp/eval/factory/AstEvaluatorFactory.java`
- KISS example: `core/src/main/java/com/elminster/jcp/compile/operator/arithmetic/PlusCompiler.java`
- ISP example: `core/src/main/java/com/elminster/jcp/ast/Node.java`
