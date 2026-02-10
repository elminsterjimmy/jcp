# Brainstorm: Source Location & Debugging Foundation

**Date:** 2026-02-10
**Status:** Ready for planning

## What We're Building

A comprehensive source location tracking and error reporting system for JCP that serves as the foundation for future debugging capabilities. This includes:

1. **Source Location Interface** - Track file, line, column, and source content for all AST nodes
2. **Enhanced Error Handling** - Rich error messages with source context and classic format
3. **Call Stack Structure** - Unified stack trace support for both interpreter and compiler modes

## Why

Currently, JCP errors don't indicate where they occurred in the source code. This makes debugging difficult for DSL users. Adding source location tracking enables:

- Meaningful error messages that point to exact source locations
- Stack traces showing the call chain leading to an error
- Foundation for breakpoint-based debugging (step over, step through, etc.)
- Multi-file program support with proper file attribution

## Scope

### Deliverables

**Issue 1: Source Location Implementation**
- `SourceLocation` interface with filepath, start/end line:column, source line content
- All AST nodes implement the location interface
- Location populated during AST construction (by DSL parser)
- Multi-file support via filepath field

**Issue 2: Error Handling Enhancement**
- Enhanced exception types with location awareness
- Error message format: `Error: <message> at <file>:<line>:<column>`
- Source line display in error output: `-->  int x = 5 / 0;`
- Caret pointing to exact error position

**Issue 3: Stack Trace Implementation**
- Separate `CallStack` structure shared by compiler and interpreter
- Interpreter: Track function/block entry/exit in EvalContext
- Compiler: Generate JVM line number tables for bytecode
- Compiler: Adapt JVM stack traces to JCP CallStack format
- Stack trace display in error output

### Out of Scope
- Actual debugger implementation (breakpoints, stepping) - future work
- `include` keyword for multi-file imports - separate feature
- IDE integration - future work
- Source maps for external tools - future work

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Location attachment | Interface on AST nodes | Clean design, all nodes get location support |
| Source content storage | Store line content in SourceLocation | Enables rich error display without re-reading files |
| Call stack ownership | Separate structure | Reusable across interpreter and compiler modes |
| Compiler stack traces | JVM tables + CallStack adapter | Best of both worlds - JVM tooling + JCP format |
| Scope | Full implementation | Foundation needed for debugging feature |

## Technical Design

### SourceLocation Interface

```java
public interface Locatable {
    SourceLocation getLocation();
    void setLocation(SourceLocation location);
}

public class SourceLocation {
    private String filepath;
    private int startLine;
    private int startColumn;
    private int endLine;
    private int endColumn;
    private String sourceLineContent;
}
```

### CallStack Structure

```java
public class CallStack {
    private Deque<StackFrame> frames;

    public void push(StackFrame frame);
    public StackFrame pop();
    public List<StackFrame> getFrames();
    public String formatStackTrace();
}

public class StackFrame {
    private String functionName;
    private SourceLocation location;
}
```

### Error Format Example

```
RuntimeError: Division by zero
  at divide(math.jcp:15:12)
  at calculate(main.jcp:8:5)
  at main(main.jcp:3:1)

  15 |   return a / b;
               ^^^^^
```

## Open Questions

1. Should `SourceLocation` be immutable after creation?
2. How should anonymous blocks/lambdas be named in stack traces?
3. Should we cache source file content for multiple error displays?
4. How to handle generated/synthetic AST nodes with no source location?

## Dependencies

- Issue 1 (Location) must be completed first
- Issue 2 (Error Handling) depends on Issue 1
- Issue 3 (Stack Traces) depends on Issues 1 and 2

## Next Steps

Run `/gw-plan` to create implementation plan for each issue.
