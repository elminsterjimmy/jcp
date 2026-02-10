---
title: "feat: Source Location Implementation for AST Nodes"
type: feat
date: 2026-02-10
issue: 14
deepened: 2026-02-10
---

# feat: Source Location Implementation for AST Nodes

## Enhancement Summary

**Deepened on:** 2026-02-10
**Research agents used:** pattern-recognition-specialist, performance-oracle, architecture-strategist, code-simplicity-reviewer, best-practices-researcher, framework-docs-researcher

### Key Improvements from Research

1. **Keep 6 fields** - End position and source content valuable for debugging
2. **Use primitive `int`** (was Integer wrappers) - 50% memory reduction
3. **Use `String.intern()`** for filepath - 99% reduction in filepath memory
4. **Remove Builder pattern** - Use static factories per codebase conventions
5. **Dual formatting** - Simple `toString()` + detailed `formatWithSource()` for errors
6. **Follow javac/Eclipse pattern** - Industry-standard approach

### Critical Insights Discovered

- **GCC/Clang error format** is the industry standard: `file:line:column: message`
- **Source content in errors** improves debugging experience significantly
- **End position** enables precise error highlighting (e.g., `^~~~~`)
- **ASM `visitLineNumber()`** requires label to be visited first
- **ANTLR tokens** use 0-based columns (convert to 1-based for display)
- **80% test coverage** is mandatory (JaCoCo enforced)

---

## Overview

Implement source location tracking for all AST nodes in JCP. This enables meaningful error messages with file paths, line numbers, and column positions, and serves as the foundation for future debugging capabilities.

**Current error (unhelpful):**
```
RuntimeError: Division by zero
```

**Desired error (actionable):**
```
math.jcp:15:8: error: Division by zero
```

## Technical Approach

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Locatable                           │
│  ┌─────────────────────────────────────────────────┐   │
│  │ + getLocation(): SourceLocation                 │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          △
                          │ implements
┌─────────────────────────┴───────────────────────────────┐
│                    AbstractNode                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │ - location: SourceLocation (nullable)           │   │
│  │ + getLocation(): SourceLocation                 │   │
│  │ + setLocation(SourceLocation): void             │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          △
          ┌───────────────┼───────────────┐
     Expression      Statement         Other
       (~45)           (~30)           Nodes
```

### Key Design Decisions

| Decision | Choice | Rationale | Research Source |
|----------|--------|-----------|-----------------|
| **Fields** | 6 fields (filepath, startLine, startColumn, endLine, endColumn, sourceLineContent) | Full range for precise highlighting + source for rich errors | user requirement |
| **Types** | Primitive `int` | 50% memory reduction vs Integer | performance-oracle |
| **Filepath** | `String.intern()` | 99% memory reduction for duplicates | performance-oracle |
| **Construction** | Static factory `of()` + `span()` | Matches JCP codebase patterns | pattern-recognition |
| **Immutability** | Final fields, no setters on SourceLocation | Thread-safe, follows javac | best-practices |
| **Interface** | Read-only `Locatable` (getter only) | ISP compliance | architecture-strategist |
| **Formatting** | `toString()` for simple + `formatWithSource()` for detailed | Best UX for debugging | user requirement |

### Research Insights

#### Industry Patterns (from best-practices-researcher)

| Compiler | Approach | Memory/Node |
|----------|----------|-------------|
| **Clang** | 32-bit offset + SourceManager | 4 bytes |
| **javac** | `int pos` + `int endpos` | 8 bytes |
| **Eclipse JDT** | `int startPosition` + `int length` | 8 bytes |
| **ANTLR** | Token references | 16 bytes |

**Recommendation**: Follow javac/Eclipse pattern (8 bytes) for simplicity.

#### Memory Analysis (from performance-oracle)

| AST Nodes | Original (Integer wrappers) | Optimized (primitive int) |
|-----------|----------------------------|---------------------------|
| 1K        | 96 KB                      | 48 KB                     |
| 10K       | 960 KB                     | 480 KB                    |
| 100K      | 9.6 MB                     | 4.8 MB                    |

**Result**: 50% memory reduction using primitive int instead of Integer wrappers.

---

## API Specification

### SourceLocation Class (Full Features)

```java
/**
 * Immutable source location for AST nodes.
 *
 * Stores complete position range (start to end) plus source content
 * for rich error messages and debugging support.
 *
 * Uses 1-based line and column indexing.
 * Thread-safe due to immutability.
 */
public final class SourceLocation {
    private final String filepath;        // Interned for memory efficiency
    private final int startLine;          // 1-based
    private final int startColumn;        // 1-based
    private final int endLine;            // 1-based
    private final int endColumn;          // 1-based
    private final String sourceLineContent;  // For rich error display

    private SourceLocation(String filepath, int startLine, int startColumn,
                          int endLine, int endColumn, String sourceLineContent) {
        // Validation
        if (startLine < 1 || startColumn < 1) {
            throw new IllegalArgumentException("startLine and startColumn must be >= 1");
        }
        if (endLine < startLine) {
            throw new IllegalArgumentException("endLine must be >= startLine");
        }
        if (endLine == startLine && endColumn < startColumn) {
            throw new IllegalArgumentException("endColumn must be >= startColumn on same line");
        }

        this.filepath = filepath == null ? null : filepath.intern();
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.sourceLineContent = sourceLineContent;
    }

    // Factory for single position (start = end)
    public static SourceLocation of(String filepath, int line, int column) {
        return new SourceLocation(filepath, line, column, line, column, null);
    }

    // Factory for position with source content
    public static SourceLocation of(String filepath, int line, int column, String sourceContent) {
        return new SourceLocation(filepath, line, column, line, column, sourceContent);
    }

    // Factory for full range with source content
    public static SourceLocation span(String filepath, int startLine, int startColumn,
                                      int endLine, int endColumn, String sourceContent) {
        return new SourceLocation(filepath, startLine, startColumn,
                                  endLine, endColumn, sourceContent);
    }

    // Getters
    public String getFilepath() { return filepath; }
    public int getStartLine() { return startLine; }
    public int getStartColumn() { return startColumn; }
    public int getEndLine() { return endLine; }
    public int getEndColumn() { return endColumn; }
    public String getSourceLineContent() { return sourceLineContent; }

    // Convenience: check if this is a range (multi-character/multi-line)
    public boolean hasRange() {
        return endLine > startLine || endColumn > startColumn;
    }

    // Simple GCC-style format: "file:line:column"
    @Override
    public String toString() {
        if (filepath == null) {
            return startLine + ":" + startColumn;
        }
        return filepath + ":" + startLine + ":" + startColumn;
    }

    /**
     * Format with source content for detailed error messages.
     *
     * Example output:
     *   math.jcp:15:8: error: Division by zero
     *     15 |   return a / b;
     *                   ^~~~~
     */
    public String formatWithSource(String level, String message) {
        StringBuilder sb = new StringBuilder();

        // Header: file:line:col: level: message
        sb.append(toString()).append(": ").append(level).append(": ").append(message);

        // Source context if available
        if (sourceLineContent != null) {
            sb.append("\n");
            // Line number gutter
            String lineNum = String.format("%4d | ", startLine);
            sb.append(lineNum).append(sourceLineContent);

            // Caret indicator
            sb.append("\n");
            sb.append(" ".repeat(lineNum.length()));  // Align with code
            sb.append(" ".repeat(startColumn - 1));   // Position to error start
            sb.append("^");

            // Extend caret for range
            if (hasRange() && endLine == startLine) {
                sb.append("~".repeat(Math.max(0, endColumn - startColumn - 1)));
            }
        }

        return sb.toString();
    }

    // equals/hashCode for value semantics
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SourceLocation that)) return false;
        return startLine == that.startLine &&
               startColumn == that.startColumn &&
               endLine == that.endLine &&
               endColumn == that.endColumn &&
               Objects.equals(filepath, that.filepath) &&
               Objects.equals(sourceLineContent, that.sourceLineContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filepath, startLine, startColumn, endLine, endColumn);
    }
}
```

### Error Message Examples

**Simple (toString):**
```
math.jcp:15:8
```

**Detailed (formatWithSource):**
```
math.jcp:15:8: error: Division by zero
  15 |   return a / b;
              ^~~~~
```

### Locatable Interface (Read-Only)

```java
/**
 * Marks AST nodes that can have source location information.
 *
 * Per ISP, this interface is read-only. Location is set via
 * AbstractNode.setLocation() during AST construction.
 */
public interface Locatable {
    /**
     * @return source location or null if not available (synthetic nodes)
     */
    SourceLocation getLocation();
}
```

### AbstractNode Integration

```java
abstract public class AbstractNode implements Node, Locatable {
    private SourceLocation location;

    @Override
    public SourceLocation getLocation() {
        return location;
    }

    /**
     * Sets the source location. Called during AST construction.
     * @param location the source location (may be null for synthetic nodes)
     */
    public void setLocation(SourceLocation location) {
        this.location = location;
    }

    @Override
    public String toString() {
        if (location != null) {
            return getName() + " at " + location;
        }
        return getName();
    }
}
```

### Usage in Error Handling

```java
// In evaluator or compiler when catching exceptions
try {
    evaluator.eval(context);
} catch (RuntimeException e) {
    SourceLocation loc = astNode.getLocation();
    if (loc != null) {
        // Rich error message with source context
        System.err.println(loc.formatWithSource("error", e.getMessage()));
    } else {
        // Fallback for synthetic nodes
        System.err.println("error: " + e.getMessage());
    }
}
```

---

## Implementation Phases

### Phase 1: Core Location Class

**Files to create:**
- `core/src/main/java/com/elminster/jcp/ast/SourceLocation.java`
- `core/src/main/java/com/elminster/jcp/ast/Locatable.java`
- `core/src/test/java/com/elminster/jcp/ast/SourceLocationTest.java`

**Tasks:**
- [ ] Create `SourceLocation` with 6 fields (filepath, startLine, startColumn, endLine, endColumn, sourceLineContent)
- [ ] Use primitive `int` for line/column fields
- [ ] Intern filepath strings for memory efficiency
- [ ] Factory methods: `of(filepath, line, column)`, `of(..., sourceContent)`, `span(...)`
- [ ] Validate: line/column >= 1, endLine >= startLine, endColumn >= startColumn (same line)
- [ ] Implement `toString()` with simple GCC format
- [ ] Implement `formatWithSource(level, message)` for detailed errors
- [ ] Implement `hasRange()` helper
- [ ] Implement `equals()` and `hashCode()`
- [ ] Create `Locatable` interface (getter only)
- [ ] Unit tests for all paths

**Test cases:**
```java
SourceLocationTest.java:
// Construction
- testValidConstruction()
- testValidConstructionWithSourceContent()
- testSpanConstruction()
- testNullFilepath()

// Validation failures
- testInvalidStartLineThrows()
- testInvalidStartColumnThrows()
- testEndLineBeforeStartLineThrows()
- testEndColumnBeforeStartColumnThrows()

// Formatting
- testToStringSimpleFormat()
- testFormatWithSourceSinglePosition()
- testFormatWithSourceRange()
- testFormatWithSourceNoContent()

// Helpers
- testHasRangeTrue()
- testHasRangeFalse()

// Value semantics
- testEqualsAndHashCode()
- testFilepathInterning()
```

### Phase 2: AST Integration

**Files to modify:**
- `core/src/main/java/com/elminster/jcp/ast/AbstractNode.java`

**Files to create:**
- `core/src/test/java/com/elminster/jcp/ast/LocatableTest.java`

**Tasks:**
- [ ] Add `private SourceLocation location` field to AbstractNode
- [ ] Implement `Locatable.getLocation()`
- [ ] Add `setLocation()` method (not in interface per ISP)
- [ ] Update `toString()` to include location
- [ ] Unit tests on various node types
- [ ] Verify existing tests still pass

**Test cases:**
```java
LocatableTest.java:
- testSetGetLocationOnExpression()
- testSetGetLocationOnStatement()
- testNullLocationByDefault()
- testToStringWithLocation()
- testToStringWithoutLocation()
```

### Phase 3: Documentation

**Tasks:**
- [ ] Javadoc on `SourceLocation` and `Locatable`
- [ ] ANTLR integration example in Javadoc
- [ ] Update CLAUDE.md with location tracking section

**ANTLR Integration Example (Full Range with Source):**
```java
@Override
public Expression visitBinaryExpr(BinaryExprContext ctx) {
    Expression left = visit(ctx.left);
    Expression right = visit(ctx.right);
    Expression expr = new Plus(left, right);

    // Extract location from ANTLR context
    Token start = ctx.getStart();
    Token stop = ctx.getStop() != null ? ctx.getStop() : start;

    // Get source line content for error display
    String sourceLine = sourceLines.get(start.getLine() - 1);  // 0-indexed list

    // ANTLR columns are 0-based, convert to 1-based
    expr.setLocation(SourceLocation.span(
        currentFile,
        start.getLine(),                           // Already 1-based
        start.getCharPositionInLine() + 1,         // Convert to 1-based
        stop.getLine(),
        stop.getCharPositionInLine() + stop.getText().length(),
        sourceLine
    ));

    return expr;
}
```

### Phase 4 (Future): Additional Features

**Can be added later when needed:**
- [ ] Range merging (`SourceLocation.merge(loc1, loc2)`) - for spanning nodes
- [ ] Multi-line source content storage
- [ ] Integration with ASM `visitLineNumber()` for compiler stack traces

---

## Acceptance Criteria

### Functional Requirements

- [ ] `SourceLocation` stores 6 fields: filepath, startLine, startColumn, endLine, endColumn, sourceLineContent
- [ ] `SourceLocation` is immutable (final fields)
- [ ] Factory methods validate: line/column >= 1, endLine >= startLine
- [ ] All AST nodes inherit location via `AbstractNode`
- [ ] Null location handled gracefully (synthetic nodes)
- [ ] `toString()` returns simple GCC format: `file:line:col`
- [ ] `formatWithSource()` returns detailed error with source snippet and caret indicator
- [ ] `hasRange()` correctly identifies multi-character/multi-line locations

### Non-Functional Requirements

- [ ] Memory overhead: ~48 bytes per node (6 fields + reference + String refs)
- [ ] Filepath strings interned for memory efficiency
- [ ] No performance regression in existing tests
- [ ] 80% code coverage (JaCoCo enforced)
- [ ] Zero breaking changes to existing tests

---

## Test Coverage Strategy

JaCoCo enforces 80% instruction and branch coverage.

### Test Matrix

| Class | Tests | Coverage Target |
|-------|-------|-----------------|
| `SourceLocation` | 15-18 | 100% (new code) |
| `Locatable` | Interface only | N/A |
| `AbstractNode` | 5-7 | 80%+ (modified) |

### Critical Test Scenarios

**Construction:**
1. `SourceLocation.of("file.jcp", 1, 1)` - valid single position
2. `SourceLocation.of("file.jcp", 1, 1, "int x = 5;")` - with source content
3. `SourceLocation.span("f", 1, 1, 1, 10, "...")` - valid range
4. `SourceLocation.of(null, 1, 1)` - null filepath allowed

**Validation failures:**
5. `SourceLocation.of("f", 0, 1)` - invalid startLine throws
6. `SourceLocation.of("f", 1, 0)` - invalid startColumn throws
7. `SourceLocation.span("f", 5, 1, 3, 1, null)` - endLine < startLine throws
8. `SourceLocation.span("f", 5, 10, 5, 5, null)` - endColumn < startColumn (same line) throws

**Formatting:**
9. `toString()` returns `"file.jcp:1:5"`
10. `formatWithSource("error", "msg")` returns detailed output with caret

**Helpers:**
11. `hasRange()` returns true for ranges, false for points

**Integration:**
12. `node.setLocation(loc); assertEquals(loc, node.getLocation())`
13. `assertNull(new Plus(...).getLocation())` - null by default
14. `node.toString()` includes location when set

**Estimated new test count: ~20 tests**

---

## Risk Analysis

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Nodes not extending AbstractNode | Low | Medium | Audit verified 75+ nodes extend it |
| Memory overhead | Low | Low | Using primitive int saves 50% vs Integer wrappers |
| Breaking existing tests | Low | High | Run full test suite per phase |
| Thread safety | Low | Medium | SourceLocation is immutable |
| Large source content strings | Low | Medium | Parser controls content; document best practices |

---

## Files Summary

### New Files
| File | Lines | Description |
|------|-------|-------------|
| `SourceLocation.java` | ~120 | Immutable location with 6 fields + formatting |
| `Locatable.java` | ~15 | Read-only interface |
| `SourceLocationTest.java` | ~200 | Unit tests |
| `LocatableTest.java` | ~60 | Integration tests |

### Modified Files
| File | Changes | Description |
|------|---------|-------------|
| `AbstractNode.java` | ~15 lines | Implement Locatable |

### Total Scope
- **New code**: ~135 lines
- **Test code**: ~260 lines
- **Total**: ~395 lines

---

## Comparison: Original vs Optimized

| Aspect | Original Plan | Optimized Plan |
|--------|---------------|----------------|
| Fields | 6 with Integer wrappers | 6 with primitive int |
| Field types | `Integer` (nullable) | `int` (primitive, memory efficient) |
| Filepath | Plain String | `String.intern()` (memory efficient) |
| Construction | Builder + factories | Static factories only (matches codebase) |
| Formatting | Single format | `toString()` simple + `formatWithSource()` detailed |
| Memory/node | ~80 bytes | ~48 bytes (40% reduction) |
| Builder | Yes | No (follows JCP patterns) |

---

## References

- **Brainstorm**: [docs/brainstorms/2026-02-10-source-location-debugging-foundation-brainstorm.md](../brainstorms/2026-02-10-source-location-debugging-foundation-brainstorm.md)
- **Related Issues**: #15 (Error Handling), #16 (Stack Traces)
- **Industry Examples**: javac JCTree, Eclipse JDT ASTNode, Clang SourceLocation
- **GUIDELINES.md**: KISS principles, 80% coverage requirement
