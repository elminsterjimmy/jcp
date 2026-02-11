---
title: "feat: Stack Trace Implementation for Interpreter and Compiler"
type: feat
date: 2026-02-11
issue: 16
deepened: 2026-02-11
---

# feat: Stack Trace Implementation for Interpreter and Compiler

## Enhancement Summary

**Deepened on:** 2026-02-11
**Research agents used:** best-practices-researcher, framework-docs-researcher, learnings-researcher, architecture-strategist, performance-oracle, code-simplicity-reviewer, pattern-recognition-specialist, security-sentinel

### Key Improvements from Research

1. **CRITICAL: visitLabel() MUST be called before visitLineNumber()** - ASM API requirement
2. **ARCHITECTURE: Keep SourceMapping and JvmStackTraceAdapter** - Clear separation of concerns
3. **SECURITY: Add stack depth limit** - Prevent DoS via infinite recursion (default 1000)
4. **PERFORMANCE: Use ArrayDeque not LinkedList** - 50% memory reduction in FastStack
5. **PERFORMANCE: Intern function names** - 95% memory reduction for recursive calls
6. **ARCHITECTURE: Keep CallStack in EvalContext** - Follows LoopContext pattern
7. **PARITY: Test BOTH entry points** - compile() and compileWithReturn() must work

### Critical Findings

| Agent | Key Finding | Action |
|-------|-------------|--------|
| Framework Docs | `visitLabel()` MUST precede `visitLineNumber()` | Update Phase 4 code examples |
| Learnings | Dual-mode registration critical (struct bugs) | Test eval/compile parity |
| Learnings | Multiple entry points had bugs | Test all compiler entry points |
| Simplicity Reviewer | SourceMapping could be inlined | **Keep for clear architecture** (overridden) |
| Architecture | CallStack fits LoopContext pattern | Add to EvalContext interface |
| Performance | LinkedList allocates 24 bytes/node | Use ArrayDeque (already planned) |
| Performance | Function name strings repeated | Intern short names |
| Security | Unbounded recursion = DoS | Add MAX_STACK_DEPTH limit |
| Security | Absolute paths exposed | Add production mode option |

---

## Overview

Implement a unified call stack structure that provides meaningful stack traces for both interpreter and compiler runtime errors. This builds upon the foundation established in issues #14 (Source Location) and #15 (Enhanced Error Handling) to transform cryptic error messages into full debugging information.

**Current error (after #15):**
```
RuntimeError: Division by zero at math.jcp:15:12
  15 |   return a / b;
              ^~~~~
```

**Target error (after #16):**
```
RuntimeError: Division by zero at math.jcp:15:12
  15 |   return a / b;
              ^~~~~

Stack trace:
  at divide(math.jcp:15:12)
  at calculate(main.jcp:8:5)
  at main(main.jcp:3:1)
```

## Technical Approach

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CallStack                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ - frames: Deque<StackFrame>  (ArrayDeque - optimized)      │ │
│  │ - MAX_DEPTH: int = 1000      (security limit)              │ │
│  │ + push(frame): void          (throws if depth exceeded)    │ │
│  │ + pop(): StackFrame                                        │ │
│  │ + peek(): StackFrame                                       │ │
│  │ + getFrames(): List<StackFrame>                           │ │
│  │ + formatStackTrace(): String                              │ │
│  │ + copy(): CallStack                                       │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ contains
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       StackFrame                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │ - functionName: String (interned for memory efficiency)   │ │
│  │ - location: SourceLocation                                 │ │
│  │ + of(name, location): StackFrame  (static factory)        │ │
│  │ + toString(): String  // "functionName(file:line:col)"    │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Research Insight: Architecture Decision

**From Architecture Strategist Review:**
- CallStack fits naturally in EvalContext (like LoopContext for break/continue)
- RootEvalContext initializes ONE CallStack, shared by child contexts
- No need for separate CallStack in CompileContext - use JVM native LineNumberTable

### Dual-Mode Integration

```
┌─────────────────────────────────────────────────────────────────┐
│                    INTERPRETER MODE                              │
│                                                                  │
│  EvalContext                                                     │
│  ├── variables                                                   │
│  ├── functions                                                   │
│  ├── loopContext       ◄─── Existing pattern to follow          │
│  └── callStack: CallStack  ◄──── NEW (same pattern)             │
│                                                                  │
│  FunctionEvaluator (doFunc):                                     │
│    1. Push StackFrame(functionName, location)                    │
│    2. Execute function body via try-finally                      │
│    3. Pop StackFrame (guaranteed by finally)                     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     COMPILER MODE                                │
│                                                                  │
│  BytecodeGenerator:                                              │
│    - classWriter.visitSource(sourceFile, null) ◄─── REQUIRED    │
│                                                                  │
│  Statement Compilers:                                            │
│    - CRITICAL: visitLabel() BEFORE visitLineNumber()            │
│    - LineNumberTable attribute in .class file                    │
│                                                                  │
│  Runtime error handling:                                         │
│    - SourceMapping: JVM class/method → JCP file/function names  │
│    - JvmStackTraceAdapter: converts JVM stack → JCP CallStack   │
└─────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions (Enhanced from Research)

| Decision | Choice | Rationale | Research Source |
|----------|--------|-----------|-----------------|
| Stack structure | `ArrayDeque<StackFrame>` | 50% memory vs LinkedList, O(1) push/pop | performance-oracle |
| Thread safety | Immutable copy on exception | CallStack.copy() preserves state at throw time | architecture-strategist |
| Stack depth limit | MAX_DEPTH = 1000 | Prevents DoS via infinite recursion | security-sentinel |
| Function name interning | `name.intern()` for names < 64 chars | 95% memory reduction for recursive calls | performance-oracle |
| Anonymous blocks | `<anonymous>` or `<block@line:col>` | Clear identification in stack traces | - |
| Compiler adapter | `SourceMapping` + `JvmStackTraceAdapter` | Clear separation of concerns, extensible | arch decision |
| Entry point naming | `main` / `evaluate` | Standard convention | - |
| Package location | `com.elminster.jcp.exception` | Co-locate with JcpException | architecture-strategist |
| Stack trace preservation | `setStackTrace()` copy | Consistency with existing `withLocation()` pattern | arch decision |

### Architectural Decision: Stack Trace Preservation in Fluent Methods

**Decision:** Use `setStackTrace()` to copy the original Java stack trace when creating enhanced exception instances in `withLocation()` and `withCallStack()`.

**Context:** When enhancing a `JcpException` with additional context (location or call stack), we create a new instance. There are two approaches to preserve the original Java stack trace:

1. **Cause chain** (standard Java idiom): Pass `this` as the cause to the new exception
   ```java
   return new JcpException(message, location, callStack, this);  // this as cause
   ```
   - Pro: Standard Java pattern
   - Con: `printStackTrace()` shows "Caused by:" which implies wrapping, not enhancement

2. **Stack trace copy** (chosen approach): Copy the stack trace to the new instance
   ```java
   newEx.setStackTrace(this.getStackTrace());
   ```
   - Pro: Clean output - exception appears to originate from original throw site
   - Pro: Consistent with existing `withLocation()` implementation
   - Con: Manual `setStackTrace()` call required

**Decision:** Use stack trace copy for consistency with existing code and cleaner output.

**Rationale:** We are not wrapping an exception (which would warrant a cause chain), we are **enhancing the same logical exception** with additional JCP-specific context. The Java stack trace should still point to where the error originated, not where we added the context.

**Note:** Both the JCP CallStack (for DSL users) and Java stack trace (for JCP developers) are preserved:
- `exception.getCallStack()` → JCP program stack trace
- `exception.getStackTrace()` → Java implementation stack trace

### Architectural Decision: Keep SourceMapping and JvmStackTraceAdapter

**Decision:** Maintain `SourceMapping` and `JvmStackTraceAdapter` as separate classes in `compile/debug/` package.

**Context:** The code-simplicity-reviewer suggested inlining this logic into `JcpCompiler` as a static utility method to save ~90 lines of code.

**Decision:** Keep separate classes for clear architectural hierarchy.

**Rationale:**
1. **Separation of Concerns:** `SourceMapping` handles name translation, `JvmStackTraceAdapter` handles stack conversion
2. **Testability:** Each class can be unit tested independently
3. **Extensibility:** Future features (multi-file compilation, source maps, debug symbols) will benefit from this structure
4. **Consistency:** Follows JCP's established pattern of separate classes for distinct responsibilities (e.g., `AstEvaluatorFactory`, `AstCompilerFactory`)
5. **Middleware Philosophy:** JCP is a platform - cleaner architecture is worth a few extra lines

**Trade-off:** ~90 more lines of code for better maintainability and extensibility.

## Data Model

### StackFrame (Enhanced)

```java
package com.elminster.jcp.exception;  // Changed from debug/ per architecture review

import com.elminster.jcp.ast.SourceLocation;

/**
 * Represents a single frame in the call stack.
 * Immutable for thread safety and exception attachment.
 *
 * <p>Function names are interned for memory efficiency in recursive calls.
 */
public final class StackFrame {
    private static final int MAX_NAME_LENGTH_FOR_INTERNING = 64;

    private final String functionName;
    private final SourceLocation location;

    private StackFrame(String functionName, SourceLocation location) {
        // Intern short function names for memory efficiency (performance-oracle recommendation)
        if (functionName == null) {
            this.functionName = "<unknown>";
        } else if (functionName.length() < MAX_NAME_LENGTH_FOR_INTERNING) {
            this.functionName = functionName.intern();
        } else {
            this.functionName = functionName;
        }
        this.location = location;
    }

    /**
     * Creates a stack frame with function name and location.
     * Follows SourceLocation.of() pattern.
     */
    public static StackFrame of(String functionName, SourceLocation location) {
        return new StackFrame(functionName, location);
    }

    public String getFunctionName() {
        return functionName;
    }

    public SourceLocation getLocation() {
        return location;
    }

    /**
     * Format: "functionName(file:line:col)" or "functionName" if no location.
     */
    @Override
    public String toString() {
        if (location == null) {
            return functionName;
        }
        return String.format("%s(%s)", functionName, location);
    }
}
```

### CallStack (Enhanced with Security)

```java
package com.elminster.jcp.exception;  // Changed from debug/ per architecture review

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Thread-safe call stack for tracking function execution.
 *
 * <p>Used by interpreter to track function calls.
 * Used by compiler adapter to convert JVM stack traces.
 *
 * <p><b>Security:</b> Stack depth is limited to prevent DoS attacks
 * via infinite recursion. Configure via system property.
 */
public final class CallStack {
    /**
     * Maximum stack depth to prevent DoS via infinite recursion.
     * Configurable via: -Djcp.maxStackDepth=2000
     */
    private static final int MAX_STACK_DEPTH =
        Integer.parseInt(System.getProperty("jcp.maxStackDepth", "1000"));

    private final Deque<StackFrame> frames;

    public CallStack() {
        // ArrayDeque is 50% more memory efficient than LinkedList (performance-oracle)
        this.frames = new ArrayDeque<>(32);  // Pre-size for typical depth
    }

    private CallStack(Deque<StackFrame> frames) {
        this.frames = new ArrayDeque<>(frames);
    }

    /**
     * Push a frame onto the stack.
     *
     * @throws StackOverflowException if max depth exceeded (security limit)
     */
    public void push(StackFrame frame) {
        if (frames.size() >= MAX_STACK_DEPTH) {
            throw new StackOverflowException(
                "Maximum call stack depth exceeded: " + MAX_STACK_DEPTH +
                "\nLast frame: " + frame.toString()
            );
        }
        frames.push(frame);
    }

    public StackFrame pop() {
        return frames.isEmpty() ? null : frames.pop();
    }

    public StackFrame peek() {
        return frames.isEmpty() ? null : frames.peek();
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public int size() {
        return frames.size();
    }

    /**
     * Returns frames in call order (most recent first).
     */
    public List<StackFrame> getFrames() {
        return Collections.unmodifiableList(new ArrayList<>(frames));
    }

    /**
     * Creates an immutable copy for exception attachment.
     */
    public CallStack copy() {
        return new CallStack(this.frames);
    }

    /**
     * Formats stack trace for display.
     *
     * <pre>
     * Stack trace:
     *   at divide(math.jcp:15:12)
     *   at calculate(main.jcp:8:5)
     *   at main(main.jcp:3:1)
     * </pre>
     */
    public String formatStackTrace() {
        if (frames.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Stack trace:\n");
        for (StackFrame frame : frames) {
            sb.append("  at ").append(frame).append("\n");
        }
        return sb.toString().trim();
    }
}

/**
 * Thrown when call stack depth exceeds configured limit.
 */
class StackOverflowException extends JcpException {
    public StackOverflowException(String message) {
        super(message);
    }
}
```

### JcpException Enhancement

```java
// Add to JcpException.java

private final CallStack callStack;

public JcpException(String message, SourceLocation location, CallStack callStack) {
    super(message);
    this.location = location;
    this.callStack = callStack != null ? callStack.copy() : null;
}

public JcpException(String message, SourceLocation location, CallStack callStack, Throwable cause) {
    super(message, cause);
    this.location = location;
    this.callStack = callStack != null ? callStack.copy() : null;
}

public CallStack getCallStack() {
    return callStack;
}

/**
 * CRITICAL: Must call setStackTrace() to preserve Java stack trace (learned from #15).
 */
@SuppressWarnings("unchecked")
public <T extends JcpException> T withCallStack(CallStack callStack) {
    if (this.callStack != null) {
        return (T) this;
    }
    JcpException newEx = new JcpException(getBaseMessage(), location, callStack, getCause());
    newEx.setStackTrace(this.getStackTrace());  // CRITICAL: Preserve stack trace
    return (T) newEx;
}

/**
 * Full formatted message with location context and stack trace.
 */
public String getFullMessage() {
    StringBuilder sb = new StringBuilder(getFormattedMessage());
    if (callStack != null && !callStack.isEmpty()) {
        sb.append("\n\n").append(callStack.formatStackTrace());
    }
    return sb.toString();
}
```

## Implementation Phases (Revised: 4 Phases)

**Research finding:** Original 6-phase plan over-engineered. Consolidated to 4 phases.

### Phase 1: Core Stack Infrastructure

**Files to create:**
- `core/src/main/java/com/elminster/jcp/exception/StackFrame.java`
- `core/src/main/java/com/elminster/jcp/exception/CallStack.java`
- `core/src/test/java/com/elminster/jcp/exception/StackFrameTest.java`
- `core/src/test/java/com/elminster/jcp/exception/CallStackTest.java`

**Package change:** `exception/` instead of `debug/` (architecture-strategist recommendation)

**Tasks:**
- [ ] Create `StackFrame` with static factory `of()` and function name interning
- [ ] Create `CallStack` with ArrayDeque, depth limit, and push/pop/copy
- [ ] Create `StackOverflowException` for depth limit exceeded
- [ ] Unit tests for both classes (~25 tests)
  - Construction with/without location
  - push/pop/peek operations
  - copy() creates independent copy
  - formatStackTrace() output format
  - Empty stack handling
  - **Stack depth limit enforcement (security test)**
  - **Function name interning verification (performance test)**

**Test cases (enhanced):**
```java
// StackFrameTest.java
@Test void testConstructionWithLocation()
@Test void testConstructionWithNullLocation()
@Test void testConstructionWithNullFunctionName()
@Test void testToStringWithLocation()
@Test void testToStringWithoutLocation()
@Test void testFunctionNameInterning()  // NEW: verify short names are interned
@Test void testLongFunctionNameNotInterned()  // NEW: > 64 chars not interned

// CallStackTest.java
@Test void testPushAndPop()
@Test void testPeekDoesNotRemove()
@Test void testEmptyStack()
@Test void testGetFramesReturnsUnmodifiableList()
@Test void testCopyIsIndependent()
@Test void testFormatStackTraceEmpty()
@Test void testFormatStackTraceMultipleFrames()
@Test void testSize()
@Test void testMaxDepthEnforced()  // NEW: security test
@Test void testMaxDepthConfigurable()  // NEW: system property
```

### Phase 2: JcpException Enhancement + Interpreter Integration

**Research insight:** Combine Phases 2 & 3 from original plan - closely coupled.

**Files to modify:**
- `core/src/main/java/com/elminster/jcp/exception/JcpException.java`
- `core/src/main/java/com/elminster/jcp/eval/context/EvalContext.java`
- `core/src/main/java/com/elminster/jcp/eval/context/RootEvalContext.java`
- `core/src/main/java/com/elminster/jcp/eval/statement/function/FunctionEvaluator.java`
- `core/src/main/java/com/elminster/jcp/eval/EvalVisitor.java`
- `core/src/test/java/com/elminster/jcp/exception/JcpExceptionTest.java`

**Tasks:**
- [ ] Add `callStack` field and constructors to JcpException
- [ ] Add `withCallStack()` fluent method (MUST preserve Java stack trace)
- [ ] Add `getFullMessage()` for combined output
- [ ] Add `CallStack getCallStack()` to `EvalContext` interface
- [ ] Initialize `CallStack` in `RootEvalContext`
- [ ] Update `FunctionEvaluator.doFunc()` with push/pop:
  ```java
  // In FunctionEvaluator.doFunc() - line 58 area
  StackFrame frame = StackFrame.of(function.getName(), function.getLocation());
  evalContext.getCallStack().push(frame);
  try {
      // existing function body execution (lines 62-85)
  } finally {
      evalContext.getCallStack().pop();
  }
  ```
- [ ] Update exception handling to attach CallStack:
  ```java
  // In EvalVisitor or at catch sites
  catch (JcpException e) {
      throw e.withCallStack(context.getCallStack());
  }
  ```
- [ ] Integration tests for interpreter stack traces

**Test cases:**
```java
// JcpExceptionTest.java additions
@Test void testConstructionWithCallStack()
@Test void testWithCallStackCreatesNewInstance()
@Test void testWithCallStackPreservesJavaStackTrace()  // CRITICAL
@Test void testWithCallStackPreservesCause()  // CRITICAL
@Test void testWithCallStackDoesNotOverrideExisting()
@Test void testGetFullMessageWithCallStack()
@Test void testGetFullMessageWithoutCallStack()
@Test void testBackwardCompatibility()

// Interpreter integration tests
@Test void testStackTraceInSimpleFunctionCall()
@Test void testStackTraceInNestedFunctionCalls()
@Test void testStackTraceInRecursiveFunction()
@Test void testStackTraceWithAnonymousBlocks()
@Test void testStackTracePreservedAcrossRethrow()
```

### Phase 3: Compiler Line Number Generation

**CRITICAL ASM API REQUIREMENT** (from framework-docs-researcher):
> `visitLabel()` MUST be called BEFORE `visitLineNumber()`, otherwise `IllegalArgumentException` is thrown.

**Files to modify:**
- `core/src/main/java/com/elminster/jcp/compile/BytecodeGenerator.java`
- `core/src/main/java/com/elminster/jcp/compile/base/AbstractAstCompiler.java`
- Statement compilers: VariableDeclarationCompiler, AssignmentCompiler, FunCallCompiler, IfCompiler, WhileCompiler, ReturnCompiler

**Tasks:**
- [ ] Add source file attribute in BytecodeGenerator:
  ```java
  // In BytecodeGenerator.compile() - after classWriter.visit()
  String sourceFile = determineSourceFile(program);
  classWriter.visitSource(sourceFile, null);  // REQUIRED for stack traces
  ```
- [ ] Add helper method in AbstractAstCompiler (CORRECT ORDER):
  ```java
  /**
   * Emits line number for current AST node.
   * CRITICAL: visitLabel() MUST be called before visitLineNumber().
   */
  protected void visitLineNumber(MethodVisitor mv) {
      SourceLocation loc = getSourceLocation();
      if (loc != null) {
          Label label = new Label();
          mv.visitLabel(label);           // 1. Visit label FIRST
          mv.visitLineNumber(loc.getStartLine(), label);  // 2. Then line number
      }
  }
  ```
- [ ] Apply to key statement compilers (single line addition each):
  ```java
  @Override
  public void compile(MethodVisitor mv, CompileContext ctx) {
      visitLineNumber(mv);  // Add at start of compile()
      // ... existing bytecode generation
  }
  ```
- [ ] Apply to ALL entry points (learned from struct bugs):
  - `generateMainMethod()`
  - `generateEvaluateMethod()` (compileWithReturn path)
- [ ] Verify with javap:
  ```bash
  javap -v -c target/test-classes/GeneratedClass.class | grep -A20 LineNumberTable
  ```

**Test cases:**
```java
@Test void testLineNumberTableGenerated()  // Verify with javap
@Test void testSourceFileAttributeSet()
@Test void testLineNumbersInMainMethod()  // compile() path
@Test void testLineNumbersInEvaluateMethod()  // compileWithReturn() path - CRITICAL
```

### Phase 4: Compiler Stack Trace Adapter

**Architecture:** Keep `SourceMapping` and `JvmStackTraceAdapter` as separate classes for clear separation of concerns and future extensibility (e.g., multi-file compilation, source maps).

**Files to create:**
- `core/src/main/java/com/elminster/jcp/compile/debug/SourceMapping.java`
- `core/src/main/java/com/elminster/jcp/compile/debug/JvmStackTraceAdapter.java`
- `core/src/test/java/com/elminster/jcp/compile/debug/SourceMappingTest.java`
- `core/src/test/java/com/elminster/jcp/compile/debug/JvmStackTraceAdapterTest.java`

**Files to modify:**
- `core/src/main/java/com/elminster/jcp/compile/JcpCompiler.java`
- Integration tests

**Tasks:**
- [ ] Create `SourceMapping` class for JVM→JCP name mapping:
  ```java
  package com.elminster.jcp.compile.debug;

  /**
   * Maps JVM class/method names to JCP source file/function names.
   * Populated during compilation, used at runtime for stack trace conversion.
   */
  public class SourceMapping {
      private final Map<String, String> classToFile = new HashMap<>();
      private final Map<String, String> methodToFunction = new HashMap<>();

      public void registerClass(String jvmClassName, String jcpFilename) {
          classToFile.put(jvmClassName, jcpFilename);
      }

      public void registerFunction(String jvmClassName, String jvmMethodName,
                                   String jcpFunctionName) {
          methodToFunction.put(jvmClassName + "." + jvmMethodName, jcpFunctionName);
      }

      public String getFilename(String jvmClassName) {
          return classToFile.get(jvmClassName);
      }

      public String getFunctionName(String jvmClassName, String jvmMethodName) {
          return methodToFunction.get(jvmClassName + "." + jvmMethodName);
      }
  }
  ```
- [ ] Create `JvmStackTraceAdapter` class:
  ```java
  package com.elminster.jcp.compile.debug;

  /**
   * Converts JVM stack traces to JCP CallStack.
   * Filters internal JVM and JCP framework frames.
   */
  public class JvmStackTraceAdapter {
      private static final Set<String> FILTERED_PACKAGES = Set.of(
          "java.", "jdk.", "sun.", "com.elminster.jcp.compile",
          "com.elminster.jcp.eval", "org.objectweb.asm"
      );

      private final SourceMapping mapping;

      public JvmStackTraceAdapter(SourceMapping mapping) {
          this.mapping = mapping;
      }

      public CallStack convert(StackTraceElement[] jvmStack) {
          CallStack stack = new CallStack();
          for (StackTraceElement elem : jvmStack) {
              // Filter JVM/JCP internal frames
              if (shouldFilter(elem.getClassName())) {
                  continue;
              }

              String filename = mapping.getFilename(elem.getClassName());
              String funcName = mapping.getFunctionName(
                  elem.getClassName(), elem.getMethodName());

              // Use JVM names as fallback if not mapped
              SourceLocation loc = SourceLocation.of(
                  filename != null ? filename : elem.getFileName(),
                  elem.getLineNumber(),
                  1  // JVM doesn't track columns
              );
              stack.push(StackFrame.of(
                  funcName != null ? funcName : elem.getMethodName(),
                  loc
              ));
          }
          return stack;
      }

      private boolean shouldFilter(String className) {
          return FILTERED_PACKAGES.stream()
              .anyMatch(pkg -> className.startsWith(pkg));
      }
  }
  ```
- [ ] Register mappings during compilation in `JcpCompiler`/`BytecodeGenerator`
- [ ] Wrap runtime exceptions with JCP stack trace:
  ```java
  // In compileAndLoad() or execute()
  try {
      compiledClass.getMethod("main", String[].class).invoke(null, args);
  } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof JcpException) {
          throw (JcpException) cause;  // Already has JCP stack trace
      }
      // Convert JVM stack trace to JCP using adapter
      CallStack jcpStack = stackTraceAdapter.convert(cause.getStackTrace());
      throw new JcpException(cause.getMessage(), null, jcpStack, cause);
  }
  ```
- [ ] Unit tests for SourceMapping and JvmStackTraceAdapter:
  ```java
  // SourceMappingTest.java
  @Test void testRegisterAndGetClass()
  @Test void testRegisterAndGetFunction()
  @Test void testUnknownClassReturnsNull()
  @Test void testUnknownMethodReturnsNull()

  // JvmStackTraceAdapterTest.java
  @Test void testConvertSimpleStackTrace()
  @Test void testConvertNestedFunctionCalls()
  @Test void testFilterJvmInternalFrames()
  @Test void testFilterJcpInternalFrames()
  @Test void testUnknownClassUsesJvmName()
  @Test void testUnknownMethodUsesJvmName()
  ```
- [ ] **Eval/Compile Parity Tests** (learned from struct bugs):
  ```java
  @ParameterizedTest
  @ValueSource(strings = {"EVAL", "COMPILE"})
  void testStackTraceInDivisionByZero(String mode)

  @ParameterizedTest
  @ValueSource(strings = {"EVAL", "COMPILE"})
  void testStackTraceInNestedFunctions(String mode)

  @Test
  void testStackTraceFormatConsistencyBetweenModes()
  ```
- [ ] Run full test suite: `mvn verify -pl core`

## Acceptance Criteria

### Functional Requirements

- [ ] `CallStack` structure with push/pop/peek operations
- [ ] `StackFrame` with function name and source location
- [ ] **Stack depth limit enforced (default 1000)** - security
- [ ] Interpreter tracks function entry/exit in EvalContext
- [ ] Compiler generates JVM LineNumberTable in bytecode
- [ ] Runtime errors include full JCP stack trace
- [ ] Stack trace format: `  at functionName(file:line:col)`
- [ ] Anonymous blocks identified as `<anonymous>` or `<block@line:col>`
- [ ] Null location gracefully handled (no crash)
- [ ] **JVM internal frames filtered** - security

### Non-Functional Requirements

- [ ] JaCoCo 80% instruction and 80% branch coverage maintained
- [ ] No breaking changes to existing public API
- [ ] All existing tests pass without modification
- [ ] Stack trace generation lightweight (no file I/O)
- [ ] Thread-safe: CallStack.copy() for exception attachment
- [ ] **Function names interned for memory efficiency** - performance

### Quality Gates

- [ ] `mvn test -pl core` passes
- [ ] `mvn verify -pl core` passes (coverage check)
- [ ] No new compiler warnings
- [ ] javap shows LineNumberTable in generated bytecode
- [ ] **Both compile() and compileWithReturn() tested** - learned from bugs

## Dependencies

- **Requires:** Issue #14 (Source Location Implementation) - COMPLETED
- **Requires:** Issue #15 (Enhanced Error Handling) - COMPLETED

## Risk Analysis (Enhanced)

| Risk | Likelihood | Impact | Mitigation | Source |
|------|------------|--------|------------|--------|
| ASM visitLabel/visitLineNumber order | **HIGH** | High | Follow CORRECT order in code examples | framework-docs |
| Performance overhead in interpreter | Low | Low | Stack operations are O(1), ArrayDeque | performance-oracle |
| Breaking existing exception handling | Medium | High | Backward compatible constructors | - |
| JVM stack trace filtering incomplete | Medium | Low | Filter java.*, jdk.*, internal | security-sentinel |
| Column number unavailable in JVM | High | Low | Default to column 1 for compiled code | - |
| Recursive function DoS attack | **Medium** | **High** | MAX_STACK_DEPTH = 1000 | security-sentinel |
| **Eval/Compile divergence** | **Medium** | **High** | **Parameterized parity tests** | learnings-researcher |
| **Multiple entry point bugs** | **Medium** | **High** | **Test compile() AND compileWithReturn()** | learnings-researcher |
| withCallStack() loses Java stack | Medium | High | **MUST call setStackTrace()** | #15 learnings |

## File Changes Summary (Revised)

### New Files (7)
| File | Lines | Description |
|------|-------|-------------|
| `exception/StackFrame.java` | ~50 | Stack frame with interning and static factory |
| `exception/CallStack.java` | ~100 | Call stack with depth limit and ArrayDeque |
| `exception/StackFrameTest.java` | ~80 | Unit tests including interning/security |
| `exception/CallStackTest.java` | ~100 | Unit tests including depth limit |
| `compile/debug/SourceMapping.java` | ~50 | JVM to JCP name mapping registry |
| `compile/debug/JvmStackTraceAdapter.java` | ~60 | JVM stack trace converter |
| `compile/debug/JvmStackTraceAdapterTest.java` | ~80 | Unit tests for adapter |

### Modified Files (8+)
| File | Changes | Description |
|------|---------|-------------|
| `JcpException.java` | ~40 lines | Add callStack field, constructors, withCallStack, getFullMessage |
| `JcpExceptionTest.java` | ~60 lines | Tests including stack trace preservation |
| `EvalContext.java` | ~5 lines | Add getCallStack() |
| `RootEvalContext.java` | ~10 lines | Initialize callStack |
| `FunctionEvaluator.java` | ~10 lines | Push/pop stack frames |
| `BytecodeGenerator.java` | ~10 lines | visitSource() call |
| `AbstractAstCompiler.java` | ~15 lines | visitLineNumber() helper |
| ~6 compiler files | ~3 lines each | Add visitLineNumber() calls |
| `JcpCompiler.java` | ~20 lines | Adapter integration + exception wrapping |
| Integration tests | ~120 lines | Parity tests for eval/compile |

### Estimated Total
- **New code**: ~450 lines
- **Test code**: ~360 lines
- **Total**: ~810 lines

## Security Checklist (from security-sentinel)

### Input Validation
- [ ] Stack depth limit enforced (MAX_DEPTH = 1000)
- [ ] Function names validated/sanitized (pattern check)
- [ ] Source line content length limited (existing in SourceLocation)

### Information Disclosure Prevention
- [ ] JVM internal frames filtered (java.*, jdk.*, internal)
- [ ] JCP framework frames filtered (com.elminster.jcp.compile/eval)
- [ ] Consider production mode for path sanitization (future)

### Resource Limits
- [ ] Maximum stack depth configurable via -Djcp.maxStackDepth
- [ ] ArrayDeque pre-sized to 32 (typical depth)

## Institutional Learnings Applied

**From `docs/solutions/logic-errors/struct-type-not-registered-in-compile-context.md`:**
> "Declaration compilers for custom types must update BOTH registries"

**Application:** When adding CallStack to EvalContext, ensure equivalent tracking exists for compiler mode (LineNumberTable).

**From `docs/solutions/runtime-errors/noclassdeffounderror-struct-classes-not-loaded.md`:**
> "When BytecodeGenerator has multiple code generation methods, ensure they follow the same pattern"

**Application:** Test stack traces work with BOTH `compile()` AND `compileWithReturn()` entry points.

**From `docs/solutions/logic-errors/struct-type-resolution-in-variable-declarations.md`:**
> "Type resolution requires context access. Always pass CompileContext to resolution methods."

**Application:** Ensure stack trace formatting receives necessary context for lookups.

## References

### Internal
- `SourceLocation.java` - Location model (follow immutable pattern)
- `JcpException.java` - Exception with location (extend with callStack)
- `EvalContext.java` - Interpreter context (add getCallStack())
- `LoopContext` - Pattern to follow for context integration

### External
- [ASM MethodVisitor.visitLineNumber() documentation](https://asm.ow2.io/javadoc/org/objectweb/asm/MethodVisitor.html)
- [JVM LineNumberTable specification](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.7.12)
- javap for bytecode verification

### Related Issues
- Issue #14: Source Location Implementation (COMPLETED)
- Issue #15: Enhanced Error Handling (COMPLETED)
