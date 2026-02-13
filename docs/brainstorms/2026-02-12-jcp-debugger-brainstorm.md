# Brainstorm: JCP Debugger

**Date:** 2026-02-12
**Status:** Ready for planning

## What We're Building

A programmatic debugger API for JCP that enables developers to debug DSL programs with standard debugging controls and inspection capabilities. The debugger will support breakpoints, stepping controls (step over, step into, step out, continue), variable inspection across scope chains, expression evaluation (watch), and stack trace visualization.

## Why

When end users encounter unexpected results or need to understand complex control flow in their DSL programs, they currently have no way to inspect intermediate state. A debugger allows:
- Inspecting variable values at any point during execution
- Understanding execution flow through step-by-step debugging
- Evaluating expressions in the current context
- Viewing the call stack to understand how execution reached a certain point

## Scope

### Deliverables (MVP)

1. **Debugger API Interface**
   - `Debugger` interface with core debugging operations
   - Integration point with `EvalVisitor` for interpreter mode

2. **Breakpoint Management**
   - Set/remove breakpoints on AST nodes (statements)
   - Breakpoint hit callbacks

3. **Stepping Controls**
   - Step over (execute current statement, stop at next)
   - Step into (descend into function calls)
   - Step out (complete current function, stop at caller)
   - Continue (run until next breakpoint)

4. **Variable Inspection**
   - Inspect variables at current scope
   - Full scope chain access (local → parent → global)
   - Type information alongside values

5. **Expression Evaluation (Watch)**
   - Evaluate arbitrary expressions in current debug context
   - Support for complex expressions, not just variable names

6. **Stack Trace**
   - View call stack at breakpoint
   - Navigate stack frames

### Future Enhancements (Out of Scope for MVP)

- Conditional breakpoints (break when `x > 5`)
- Exception/error breakpoints
- Compiler mode debugging (JDWP/JDI integration)
- CLI interface
- DAP (Debug Adapter Protocol) for IDE integration

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Target mode | Interpreter first | We control the execution loop; easier to intercept |
| Interface type | Programmatic API | Sufficient for initial use; can build CLI/DAP on top |
| Scope inspection | Full chain | Users need to see inherited variables from parent scopes |
| Expression eval | Yes (watch) | Critical for debugging complex expressions |
| Breakpoint identification | Hybrid: source location + AST node | Source location (line/column) as primary (AST nodes already have this); AST node reference as fallback for programmatic use |
| Execution model | Synchronous (blocking) | Simpler implementation; sufficient for programmatic API |
| Loop handling | Standard stepping | No special loop-aware behavior; matches industry standard debuggers |
| Run to cursor | Deferred | Not needed in MVP; breakpoints are sufficient |

## Technical Considerations

### Integration with EvalVisitor

The debugger needs to hook into `EvalVisitor.visit()` to:
- Check for breakpoints before/after each statement
- Pause execution and wait for user commands
- Provide access to `EvalContext` for variable inspection

### Context Stack Navigation

`EvalContext` maintains a context stack for scopes. The debugger needs to:
- Expose the full stack for stack trace display
- Allow inspection of any frame's variables
- Support expression evaluation in specific frame contexts

### Potential Debugger Interface

```java
// Breakpoint location - hybrid identification
public interface BreakpointLocation {
    static BreakpointLocation at(int line, int column);  // Primary: source location
    static BreakpointLocation at(Ast node);              // Fallback: AST node reference
}

public interface Debugger {
    // Breakpoint management (hybrid identification)
    void setBreakpoint(BreakpointLocation location);
    void setBreakpoint(Ast node);  // Convenience method
    void removeBreakpoint(BreakpointLocation location);
    Set<BreakpointLocation> getBreakpoints();

    // Execution control (synchronous/blocking)
    void stepOver();
    void stepInto();
    void stepOut();
    void continueExecution();

    // Inspection
    Map<String, Data<?>> getVariables();
    Map<String, Data<?>> getVariables(int frameIndex);
    Data<?> evaluate(Expression expr);
    List<StackFrame> getStackTrace();

    // State
    boolean isPaused();
    Ast getCurrentNode();
    BreakpointLocation getCurrentLocation();
}
```

## Open Questions

All questions resolved during brainstorming.

## Next Steps

Run `/gw-plan` to create implementation plan.
