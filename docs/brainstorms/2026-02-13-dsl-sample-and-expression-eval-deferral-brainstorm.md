# Brainstorm: DSL Sample & Expression Evaluation Deferral

**Date:** 2026-02-13
**Status:** Ready for planning

## What We're Building

### 1. Reference DSL Sample (New)

A complete, working DSL implementation that demonstrates how to integrate with JCP. This serves as:
- End-user documentation by example
- Integration test bed for JCP features
- Foundation for future debugger expression evaluation

### 2. Expression Evaluation Deferral (Issue #23)

Defer the debugger expression evaluation feature until a DSL parser exists, with documentation explaining the architectural dependency.

## Why

### The Core Insight

JCP is a **middleware platform** - it provides AST representation, type system, and execution engines (interpreter + compiler). However, **JCP does not include a parser**.

The expression evaluation feature (issue #23) assumes we can convert user-typed strings like `"x + y * 2"` into AST nodes:

```
User types expression → [DSL Parser needed] → AST → JCP Evaluator → Result
                              ↑
                        Missing piece
```

Without a reference DSL parser, there's no practical way to offer interactive expression evaluation in the debugger.

### Why a Full Demo DSL?

A minimal calculator wouldn't demonstrate JCP's full capabilities. A full demo DSL shows:
- Variable declarations and assignments
- Control flow (if/else, while loops)
- Function definitions and calls
- Type system integration
- Both interpreter and compiler modes

## Scope

### Deliverables

**DSL Sample (New Issue):**
- [ ] ANTLR grammar for a simple scripting language
- [ ] Parse tree to JCP AST converter
- [ ] Example programs demonstrating all JCP features
- [ ] Integration documentation
- [ ] Tests showing both eval and compile modes

**Expression Evaluation Deferral (Issue #23):**
- [ ] Update issue with architectural explanation
- [ ] Document how DSL authors can implement expression evaluation
- [ ] Add "deferred" label
- [ ] Link to new DSL sample issue as prerequisite

### Out of Scope

- Advanced DSL features (classes, modules, imports)
- IDE integration (syntax highlighting, LSP)
- Performance optimization of the sample DSL
- Production-ready error messages

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| DSL scope | Full demo (vars, control flow, functions) | Demonstrates all JCP capabilities |
| Parser technology | ANTLR | Industry standard, well-documented |
| Expression eval | Defer until DSL exists | Architectural dependency |
| Security sandboxing | Defer with expression eval | No expression input without parser |

## Open Questions

1. Should the sample DSL live in a separate module or within `core/`?
2. What should we name the sample language? (e.g., "JCPScript", "MiniLang", "Demo")
3. Should we include REPL functionality in the sample?

## Architecture for DSL Authors (Issue #23 Documentation)

When a DSL author wants to add expression evaluation to their debugger integration:

```java
// 1. DSL author provides their own parser
public interface ExpressionParser {
    Expression parse(String input) throws ParseException;
}

// 2. Integrate with JCP debugger
public class MyDslDebugger {
    private final Debugger debugger;
    private final ExpressionParser parser; // DSL-specific

    public Data<?> evaluateExpression(String input) {
        // Parse using DSL-specific parser
        Expression expr = parser.parse(input);

        // Evaluate using JCP (requires paused state)
        EvalContext context = debugger.getCurrentContext();
        return new EvalVisitor(context).visit(expr);
    }
}
```

This approach:
- Keeps JCP parser-agnostic (middleware philosophy)
- Allows each DSL to use their own syntax
- Security sandboxing becomes DSL author's responsibility

## Next Steps

1. Update issue #23 with deferral documentation
2. Create new issue for DSL sample
3. Run `/gw-plan` on the DSL sample issue to create implementation plan
