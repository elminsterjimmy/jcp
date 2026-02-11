---
title: "feat: Enhanced Error Handling with Source Context"
type: feat
date: 2026-02-10
issue: 15
deepened: 2026-02-10
---

# feat: Enhanced Error Handling with Source Context

## Enhancement Summary

**Deepened on:** 2026-02-10
**Research agents used:** best-practices-researcher, code-simplicity-reviewer, architecture-strategist, performance-oracle, pattern-recognition-specialist, learnings-researcher

### Key Improvements from Research

1. **Simplify: Remove ErrorFormatter class** - Inline formatting into JcpException (KISS principle)
2. **Simplify: Reduce constructors** - 2-3 constructors instead of 5
3. **Critical: Fix package typo** - `excpetion` → `exception` (document as known issue)
4. **Performance: Use String.format()** - Instead of string concatenation
5. **Architecture: Verify withLocation() preserves stack trace** - Critical for debugging
6. **Testing: Add eval/compile parity tests** - Learned from past bugs

### Research Findings Summary

| Agent | Key Finding | Action |
|-------|-------------|--------|
| Simplicity Reviewer | ErrorFormatter is YAGNI violation | Remove, inline into JcpException |
| Architecture Strategist | Package typo `excpetion` | Document, use correct spelling for new classes |
| Performance Oracle | String concat creates intermediates | Use `String.format()` |
| Pattern Specialist | withLocation() pattern is excellent | Keep, verify stack trace preservation |
| Learnings Researcher | Context passing is mandatory | Always pass location at throw sites |

---

## Overview

Enhance JCP exceptions to include source location information and display rich error messages with source context. This builds upon the foundation established in issue #14 (Source Location Implementation) to transform cryptic error messages into developer-friendly diagnostics.

**Scope:** Location-aware errors only. CallStack/stack traces will be added in issue #16.

**Current state:**
```
RuntimeError: Division by zero
```

**Target state:**
```
RuntimeError: Division by zero at math.jcp:15:12
  15 |   return a / b;
              ^~~~~
```

## Technical Specifications

### Data Model (Resolved from Issue #14)

| Question | Answer | Source |
|----------|--------|--------|
| Line/column indexing | **1-based** | `SourceLocation.java:68-69` validates `>= 1` |
| SourceLocation mutability | **Immutable** (all fields final) | `SourceLocation.java:57` `public final class` |
| sourceIdentifier format | **String filepath** (absolute or relative) | `SourceLocation.java:59` |
| Source content storage | **Stored in SourceLocation** (no file I/O) | `SourceLocation.java:64` `sourceLineContent` |
| Tab handling | **Not expanded** - use as-is | Current implementation |
| Long line handling | **No truncation** | Current implementation |

### API Specifications

#### JcpException Constructors (Simplified from Research)

```java
// core/src/main/java/com/elminster/jcp/exception/JcpException.java
// Research finding: Reduce from 5 to 3 constructors (KISS principle)

public class JcpException extends RuntimeException {
    private final SourceLocation location;

    // Primary constructor - location always encouraged
    public JcpException(String message, SourceLocation location) {
        super(message);
        this.location = location;
    }

    // With cause chain - for wrapping exceptions
    public JcpException(String message, SourceLocation location, Throwable cause) {
        super(message, cause);
        this.location = location;
    }

    // Legacy constructor - backward compatible, location = null
    public JcpException(String message) {
        this(message, (SourceLocation) null);
    }

    // Accessors
    public SourceLocation getLocation() { return location; }

    /**
     * Fluent API for location attachment.
     * CRITICAL: Must preserve stack trace and cause chain.
     *
     * Research finding: This pattern is architecturally sound but
     * MUST copy stack trace via setStackTrace() to preserve debugging info.
     */
    public JcpException withLocation(SourceLocation location) {
        if (this.location != null) {
            return this;  // Don't override existing location
        }
        JcpException newEx = new JcpException(super.getMessage(), location, getCause());
        newEx.setStackTrace(this.getStackTrace());  // CRITICAL: Preserve stack trace
        return newEx;
    }
}
```

**Research insight:**
- Reduced from 5 to 3 constructors (simplicity reviewer)
- `withLocation()` MUST call `setStackTrace()` (architecture strategist)
- Pattern is correct for immutability (pattern specialist)

#### Formatting (Inlined in JcpException - No Separate Class)

```java
// Research finding: ErrorFormatter is YAGNI violation - inline into JcpException

public class JcpException extends RuntimeException {
    // ... fields ...

    /**
     * Returns formatted message with location context.
     * Uses String.format() to avoid intermediate string allocations.
     */
    public String getFormattedMessage() {
        if (location == null) {
            return getMessage();  // Graceful fallback
        }
        // Performance: String.format() uses internal StringBuilder
        return String.format("%s\n%s", getMessage(), location.formatWithSource());
    }

    /**
     * Override getMessage() to include simple location prefix.
     * For detailed output with source context, use getFormattedMessage().
     */
    @Override
    public String getMessage() {
        String msg = super.getMessage();
        if (location == null) {
            return msg;
        }
        return String.format("%s at %s", msg, location.toString());
    }
}
```

**Research insight:** ErrorFormatter as separate class violates KISS principle. Formatting belongs in JcpException itself.

#### Static Factory Methods (No Signature Changes Needed)

```java
// Existing pattern in AlreadyDeclaredException.java:11-21
// AST nodes already have location - extract it from the node parameter

public static void throwVariableUndeclaredException(Identifier identifier) {
    // Identifier extends AbstractNode which implements Locatable
    // So we can get location directly from the node - no API change needed!
    throw new VariableUndeclaredException(identifier)
        .withLocation(identifier.getLocation());
}

// Similarly for Function parameter:
public static void throwFunctionAlreadyDeclaredException(Function function) {
    throw new FunctionAlreadyDeclaredException(function)
        .withLocation(function.getLocation());
}
```

**Key insight:** All AST nodes (Identifier, Function, expressions, statements) extend `AbstractNode` which implements `Locatable`. The factory methods already receive AST nodes as parameters, so they can extract location without changing their signatures.

### Error Display Format

```
<ErrorType>: <message> at <filepath>:<line>:<column>
<line_num> | <source_line_content>
           <caret_indicator>
```

**Example outputs:**

```
# Single position error
UndeclaredException: Undefined variable 'x' at main.jcp:10:15
  10 | let y = x + 5;
              ^

# Range error (multi-character token)
CannotCastException: Cannot cast from STRING to INT at main.jcp:15:8
  15 |   return a / b;
             ^~~~~

# No location (legacy/synthetic)
EvaluationException: Division by zero
```

## Architecture

### Exception Hierarchy

```
┌─────────────────────────────────────────────────────────────────┐
│                        JcpException                             │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ - location: SourceLocation (nullable, immutable)          │ │
│  │ + getLocation(): SourceLocation                           │ │
│  │ + getFormattedMessage(): String                           │ │
│  │ + withLocation(SourceLocation): JcpException              │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              △
              ┌───────────────┴───────────────┐
              │                               │
┌─────────────┴─────────────┐   ┌─────────────┴─────────────┐
│    EvaluationException    │   │     CompileException      │
│    (interpreter errors)   │   │    (compiler errors)      │
└───────────────────────────┘   └───────────────────────────┘
              △                               △
    ┌─────────┼─────────┐                     │
    │         │         │              (all throw sites)
CannotCast  Undeclared  ...
```

### Location Attachment Strategy

**Central attachment in base classes** (preferred approach):

```java
// In AbstractAstEvaluator - add helper method
protected SourceLocation getSourceLocation() {
    return astNode instanceof Locatable ? ((Locatable) astNode).getLocation() : null;
}

// In evaluators/compilers - use at throw sites
throw new CannotCastException(actual, expected).withLocation(getSourceLocation());
```

**Alternative: Automatic wrapping in visitor** (if many throw sites):

```java
// In EvalVisitor.visit() - wrap all exceptions
try {
    return evaluable.eval(context);
} catch (JcpException e) {
    if (e.getLocation() == null && node instanceof Locatable) {
        throw e.withLocation(((Locatable) node).getLocation());
    }
    throw e;
}
```

### Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Exception hierarchy | Single base `JcpException` | Unified error handling, shared formatting |
| Location attachment | `withLocation()` fluent method | Returns new instance, immutable pattern |
| Location source | Extract from AST node parameters | Nodes already have location via `Locatable` |
| Location storage | In exception, not read from file | No file I/O during exception handling |
| Formatting | Delegate to `SourceLocation.formatWithSource()` | Reuse existing implementation from #14 |
| Backward compatibility | No API changes needed | Factory methods get location from existing params |
| CallStack | Deferred to issue #16 | Separation of concerns |

## Implementation Phases

### Phase 1: Core Exception Infrastructure (Simplified) ✅ COMPLETED

Create the base exception class with location support. **No separate ErrorFormatter class** (research finding: YAGNI).

**Files created:**
- `core/src/main/java/com/elminster/jcp/exception/JcpException.java`
- `core/src/test/java/com/elminster/jcp/exception/JcpExceptionTest.java`

**Tasks:**
- [x] Create `JcpException` base class (~50 lines)
  - 3 constructors (not 5 - simplified per research)
  - `getLocation()`, `getFormattedMessage()`, `withLocation()`
  - Override `getMessage()` to include location when present
  - **CRITICAL:** `withLocation()` must call `setStackTrace()` to preserve debugging info
- [x] Unit tests for `JcpException` (~20 tests)
  - Construction with/without location
  - `withLocation()` creates new instance (immutable)
  - **`withLocation()` preserves stack trace** (critical test)
  - **`withLocation()` preserves cause chain** (critical test)
  - `getFormattedMessage()` output format
  - `getMessage()` includes simple location prefix
  - Null location → graceful fallback

**Research-driven test additions:**
```java
@Test
void testWithLocationPreservesStackTrace() {
    JcpException original = new JcpException("error", null);
    StackTraceElement[] originalTrace = original.getStackTrace();

    JcpException withLoc = original.withLocation(someLocation);

    assertArrayEquals(originalTrace, withLoc.getStackTrace());  // CRITICAL
}

@Test
void testWithLocationPreservesCause() {
    Throwable cause = new RuntimeException("root cause");
    JcpException original = new JcpException("error", null, cause);

    JcpException withLoc = original.withLocation(someLocation);

    assertSame(cause, withLoc.getCause());  // CRITICAL
}
```

### Phase 2: Migrate EvaluationException Hierarchy ✅ COMPLETED

Update interpreter exceptions to extend `JcpException`.

**Files to modify:**
- `core/src/main/java/com/elminster/jcp/eval/excpetion/EvaluationException.java`
- `core/src/main/java/com/elminster/jcp/eval/excpetion/DeclarationException.java`
- `core/src/main/java/com/elminster/jcp/eval/excpetion/AlreadyDeclaredException.java`
- `core/src/main/java/com/elminster/jcp/eval/excpetion/UndeclaredException.java`
- `core/src/main/java/com/elminster/jcp/eval/excpetion/CannotCastException.java`
- `core/src/main/java/com/elminster/jcp/eval/excpetion/DataCastException.java`
- `core/src/main/java/com/elminster/jcp/eval/excpetion/InitializeException.java`
- `core/src/main/java/com/elminster/jcp/eval/excpetion/FunctionAmbiguityException.java`
- `core/src/main/java/com/elminster/jcp/eval/excpetion/FunctionArgumentsLengthException.java`

**Tasks:**
- [x] Change `EvaluationException extends RuntimeException` → `extends JcpException`
- [x] Update static factory methods to extract location from AST node parameters:
  ```java
  // AlreadyDeclaredException.java - no signature change, just add .withLocation()
  public static void throwFunctionAlreadyDeclaredException(Function function) {
      throw new FunctionAlreadyDeclaredException(function)
          .withLocation(function.getLocation());  // <-- add this
  }

  // Same pattern for all other factory methods
  ```
- [x] Ensure deferred `getMessage()` pattern continues to work
- [x] Verify all existing tests pass (backward compatibility)
- [x] No API changes required - all callers continue to work unchanged

### Phase 3: Migrate CompileException ✅ COMPLETED

Update compiler exception to extend `JcpException`.

**Files to modify:**
- `core/src/main/java/com/elminster/jcp/compile/exception/CompileException.java`

**Tasks:**
- [x] Change `CompileException extends RuntimeException` → `extends JcpException`
- [x] Add location-aware constructors:
  ```java
  public CompileException(String message, SourceLocation location)
  public CompileException(String message, SourceLocation location, Throwable cause)
  ```
- [x] Unit tests for compiler exception with location

### Phase 4: Evaluator Location Attachment ✅ COMPLETED

Update evaluators to attach source location when throwing exceptions.

**Files to modify:**
- `core/src/main/java/com/elminster/jcp/eval/base/AbstractAstEvaluator.java`
- Throw sites in: `VariableDeclarationEvaluator`, `FunctionEvaluator`, `StructInstantiationEvaluator`, `FieldAccessEvaluator`, `FieldAssignmentEvaluator`, `PlusPlusEvaluator`, `MinusMinusEvaluator`

**Tasks:**
- [x] Add helper method to `AbstractAstEvaluator`:
  ```java
  protected SourceLocation getSourceLocation() {
      return astNode instanceof Locatable ? ((Locatable) astNode).getLocation() : null;
  }
  ```
- [x] Update throw sites to include location:
  ```java
  // Before
  throw new CannotCastException(actual, expected);

  // After
  throw new CannotCastException(actual, expected).withLocation(getSourceLocation());
  ```
- [x] Integration tests verifying location in interpreter errors

### Phase 5: Compiler Location Attachment ✅ COMPLETED

Update compilers to attach source location when throwing exceptions.

**Files to modify:**
- `core/src/main/java/com/elminster/jcp/compile/base/AbstractAstCompiler.java`
- Throw sites in: `ReturnCompiler`, `IdentifierCompiler`, `VariableCompiler`, `BreakCompiler`, `ContinueCompiler`, `AssignmentCompiler`, `FunCallCompiler`, `StaticMethodCallCompiler`, `PlusPlusCompiler`, `MinusMinusCompiler`

**Tasks:**
- [x] Add helper method to `AbstractAstCompiler`:
  ```java
  protected SourceLocation getSourceLocation() {
      return astNode instanceof Locatable ? ((Locatable) astNode).getLocation() : null;
  }
  ```
- [x] Update throw sites (example):
  ```java
  // Before (ReturnCompiler.java:37)
  throw new CompileException("Return statement outside function context");

  // After
  throw new CompileException("Return statement outside function context", getSourceLocation());
  ```
- [x] Integration tests verifying location in compiler errors

### Phase 6: Documentation and Coverage ✅ COMPLETED

- [x] Update Javadoc for all public exception classes
- [x] Add usage examples in `JcpException` class documentation
- [x] Run `mvn verify -pl core` to ensure 80% coverage
- [ ] Update `CLAUDE.md` with error handling patterns (if significant) - Not needed, existing patterns are clear

## Test Strategy

### Test Matrix (Eval/Compile Parity)

| Error Scenario | Eval Mode | Compile Mode | Location Attached |
|----------------|-----------|--------------|-------------------|
| Undefined variable | `VariableDeclarationEvaluator` | `VariableCompiler` | ✅ |
| Type mismatch | `CannotCastException` | `CompileException` | ✅ |
| Already declared | `AlreadyDeclaredException` | N/A | ✅ |
| Return outside function | N/A | `ReturnCompiler` | ✅ |
| Break outside loop | `BreakEvaluator` | `BreakCompiler` | ✅ |
| Undefined function | `FunctionEvaluator` | `FunCallCompiler` | ✅ |

### Unit Tests (Consolidated - No Separate ErrorFormatterTest)

**JcpExceptionTest.java (~120 lines):**
```java
// ===== CONSTRUCTION =====
@Test void testConstructionWithoutLocation()
@Test void testConstructionWithLocation()
@Test void testConstructionWithCause()

// ===== IMMUTABLE withLocation() PATTERN =====
@Test void testWithLocationCreatesNewInstance()
@Test void testWithLocationPreservesMessage()
@Test void testWithLocationPreservesCause()
@Test void testWithLocationPreservesStackTrace()  // CRITICAL (from architecture review)
@Test void testWithLocationDoesNotOverrideExisting()  // Immutability check

// ===== MESSAGE FORMATTING (was ErrorFormatterTest) =====
@Test void testGetMessageWithoutLocation()
@Test void testGetMessageWithLocation()  // "error at file:line:col"
@Test void testGetFormattedMessageWithoutLocation()
@Test void testGetFormattedMessageWithLocation()  // Full source context
@Test void testGetFormattedMessageWithRangeLocation()  // Caret with tildes

// ===== EDGE CASES =====
@Test void testNullLocationHandling()
@Test void testVeryLongSourceLine()  // From pattern review
@Test void testMultiByteCharacters()  // UTF-8 column accuracy
```

**Research insight:** ErrorFormatterTest merged into JcpExceptionTest (no separate class).

### Integration Tests (Enhanced from Research)

**LocationAwareExceptionIntegrationTest.java (~200 lines):**

```java
// ===== EVAL/COMPILE PARITY TESTS (from institutional learnings) =====
// These MUST test same scenarios in both modes

@ParameterizedTest
@ValueSource(strings = {"EVAL", "COMPILE"})
void testUndefinedVariableShowsLocation(String mode) {
    // Setup AST with undefined variable at line 10, col 5
    // Execute in specified mode
    // Assert: exception has correct location
    // Assert: message format is consistent between modes
}

@ParameterizedTest
@ValueSource(strings = {"EVAL", "COMPILE"})
void testTypeMismatchShowsLocation(String mode) { ... }

// ===== ENTRY POINT COVERAGE (from NoClassDefFoundError learning) =====
// All entry points must behave consistently

@Test void testCompileMethodShowsLocation()
@Test void testCompileWithReturnMethodShowsLocation()  // This entry point had bugs before
@Test void testCompileToMultipleClassesMethodShowsLocation()

// ===== STANDARD TESTS =====
@Test void testUndefinedVariableShowsLocation()
@Test void testTypeMismatchShowsLocation()
@Test void testFunctionErrorShowsLocation()
@Test void testCompileReturnOutsideFunctionShowsLocation()
@Test void testCompileBreakOutsideLoopShowsLocation()
@Test void testCompileUndefinedFunctionShowsLocation()
```

**Research insight:** Past bugs showed eval/compile divergence. Parameterized tests ensure parity.

## Acceptance Criteria

### Functional Requirements

- [ ] All JCP exceptions include optional source location
- [ ] `JcpException.getFormattedMessage()` returns human-readable error with location
- [ ] Error format: `<message> at <file>:<line>:<col>`
- [ ] Source line displayed with line number gutter: `  15 | <source>`
- [ ] Caret indicator: `^` for single position, `^~~~` for range
- [ ] Null location → graceful fallback to message only
- [ ] Backward compatible: existing constructors unchanged

### Non-Functional Requirements

- [ ] JaCoCo 80% instruction and 80% branch coverage maintained
- [ ] No breaking changes to existing public API
- [ ] All existing tests pass without modification
- [ ] Exception creation lightweight (no file I/O)

### Quality Gates

- [ ] `mvn test -pl core` passes
- [ ] `mvn verify -pl core` passes (coverage check)
- [ ] No new compiler warnings

## Dependencies

- **Requires:** Issue #14 (Source Location Implementation) - ✅ COMPLETED
  - `SourceLocation` class with `formatWithSource()` method
  - `Locatable` interface on all AST nodes
  - `AbstractNode` implements location tracking

## Future Work (Issue #16)

- `CallStack` structure with `StackFrame` entries
- Interpreter: Track function entry/exit in EvalContext
- Compiler: JVM LineNumberTable + adapter to JCP format
- Stack trace display in error output

## Risk Analysis (Enhanced from Research)

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking existing exception handling | Medium | High | Overloaded constructors, keep legacy signatures |
| Missing location in some code paths | High | Low | Graceful null fallback, iterate to add coverage |
| Inconsistent eval/compile behavior | Medium | Medium | **Test matrix with parity tests** (from learnings) |
| Test assertion failures from format change | Medium | Low | Tests check message content, not format |
| Tab character misalignment | Medium | Low | Document as known limitation, fix in future |
| **withLocation() loses stack trace** | Medium | **High** | **MUST call setStackTrace()** (architecture review) |
| **Package typo confusion** | Low | Medium | Document in CLAUDE.md, use correct spelling for new classes |

### Research-Identified Risks

**From Institutional Learnings (3 past bugs):**

1. **Context-passing omission** - Past bugs occurred when methods didn't have access to context/location
   - **Mitigation:** Always pass AstNode to throw sites for location extraction

2. **Eval/Compile divergence** - Struct bugs showed eval worked but compile didn't
   - **Mitigation:** Run identical test scenarios in both modes

3. **Multiple entry points** - `compile()` vs `compileWithReturn()` had different behavior
   - **Mitigation:** Test all entry points with exception scenarios

## File Changes Summary (Simplified from Research)

**New files (2):** _(Reduced from 4 - ErrorFormatter removed)_
| File | Lines | Description |
|------|-------|-------------|
| `exception/JcpException.java` | ~50 | Base exception with location + formatting |
| `exception/JcpExceptionTest.java` | ~120 | Unit tests (expanded for stack trace tests) |

**Modified files (12+):**
| File | Changes | Description |
|------|---------|-------------|
| `EvaluationException.java` | ~10 | Extend JcpException |
| `CompileException.java` | ~20 | Extend JcpException, add constructors |
| 8 exception subclasses | ~5 each | Add `.withLocation()` to factories |
| `AbstractAstEvaluator.java` | ~10 | Add `getSourceLocation()` helper |
| `AbstractAstCompiler.java` | ~10 | Add `getSourceLocation()` helper |
| ~15 evaluator/compiler files | ~3 each | Update throw sites |

**Estimated total: ~400-500 lines** _(Reduced from 500-700)_

### Files NOT Created (Research Decision)
- ~~`ErrorFormatter.java`~~ - Inlined into JcpException (KISS principle)
- ~~`ErrorFormatterTest.java`~~ - Tests moved to JcpExceptionTest

## References

### Internal
- `SourceLocation.java:193-223` - `formatWithSource()` implementation
- `AlreadyDeclaredException.java:11-21` - Static factory pattern
- `ExceptionTest.java` - Existing test patterns
- `GUIDELINES.md` - KISS and SOLID principles

### Institutional Learnings (from /deepen-plan research)

**From `docs/solutions/logic-errors/struct-type-resolution-in-variable-declarations.md`:**
- Context-passing is critical for informative error messages
- Vague errors like "Cannot determine struct type" are unhelpful without location
- **Action:** Always include file:line:col in exception messages

**From `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md`:**
- Eval mode worked, compile mode didn't (parity issue)
- **Action:** Run SAME test scenarios in BOTH modes
- **Action:** Verify exception messages match between modes

**From `docs/solutions/runtime-errors/noclassdeffounderror-struct-classes-not-loaded.md`:**
- Multiple entry points (`compile()` vs `compileWithReturn()`) had inconsistent behavior
- **Action:** Test all entry points with exception scenarios
- **Action:** Add javadoc warnings about context requirements

### External Research

**Best Practices (2024-2026):**
- Rust RFC 1644: Error message format with carets and labels
- Google Error Prone: Factory methods over constructors
- Aleksey Shipilëv: Exception performance (stack trace is 80% of cost)

**Performance:**
- `withLocation()` double allocation is acceptable (cold path)
- Use `String.format()` instead of concatenation
- Lazy `getMessage()` pattern is optimal

### Known Issues

**Package Typo:** `com.elminster.jcp.eval.excpetion` (should be `exception`)
- 9 files affected
- **Decision:** Document as known issue, use correct spelling for new `JcpException` class
- **Future:** Fix in breaking-change release
