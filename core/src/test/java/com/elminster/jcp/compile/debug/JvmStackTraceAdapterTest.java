package com.elminster.jcp.compile.debug;

import com.elminster.jcp.exception.CallStack;
import com.elminster.jcp.exception.StackFrame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JvmStackTraceAdapter class.
 */
class JvmStackTraceAdapterTest {

  private SourceMapping mapping;
  private JvmStackTraceAdapter adapter;

  @BeforeEach
  void setUp() {
    mapping = new SourceMapping();
    adapter = new JvmStackTraceAdapter(mapping);
  }

  @Test
  void convertEmptyStackTrace() {
    CallStack result = adapter.convert(new StackTraceElement[0]);

    assertTrue(result.isEmpty());
  }

  @Test
  void convertWithMappedClass() {
    mapping.registerClass("com/example/Main", "main.jcp");
    mapping.registerFunction("com/example/Main", "calculate", "calculate");

    StackTraceElement[] jvmStack = {
        new StackTraceElement("com.example.Main", "calculate", "Main.java", 42)
    };

    CallStack result = adapter.convert(jvmStack);

    assertFalse(result.isEmpty());
    List<StackFrame> frames = result.getFrames();
    assertEquals(1, frames.size());
    assertEquals("calculate", frames.get(0).getFunctionName());
    assertEquals("main.jcp", frames.get(0).getLocation().getFilepath());
    assertEquals(42, frames.get(0).getLocation().getStartLine());
  }

  @Test
  void convertWithUnmappedClass() {
    // No mapping registered - should use JVM names as fallback
    StackTraceElement[] jvmStack = {
        new StackTraceElement("com.example.Main", "doSomething", "Main.java", 10)
    };

    CallStack result = adapter.convert(jvmStack);

    assertFalse(result.isEmpty());
    List<StackFrame> frames = result.getFrames();
    assertEquals(1, frames.size());
    assertEquals("doSomething", frames.get(0).getFunctionName());
    assertEquals("Main.java", frames.get(0).getLocation().getFilepath());
  }

  @Test
  void filtersJavaPackages() {
    StackTraceElement[] jvmStack = {
        new StackTraceElement("java.lang.Integer", "parseInt", "Integer.java", 100),
        new StackTraceElement("com.example.Main", "main", "Main.java", 5)
    };

    CallStack result = adapter.convert(jvmStack);

    List<StackFrame> frames = result.getFrames();
    assertEquals(1, frames.size());
    assertEquals("main", frames.get(0).getFunctionName());
  }

  @Test
  void filtersJdkPackages() {
    StackTraceElement[] jvmStack = {
        new StackTraceElement("jdk.internal.misc.Unsafe", "park", "Unsafe.java", 50),
        new StackTraceElement("com.example.Main", "main", "Main.java", 5)
    };

    CallStack result = adapter.convert(jvmStack);

    List<StackFrame> frames = result.getFrames();
    assertEquals(1, frames.size());
    assertEquals("main", frames.get(0).getFunctionName());
  }

  @Test
  void filtersSunPackages() {
    StackTraceElement[] jvmStack = {
        new StackTraceElement("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 30),
        new StackTraceElement("com.example.Main", "main", "Main.java", 5)
    };

    CallStack result = adapter.convert(jvmStack);

    List<StackFrame> frames = result.getFrames();
    assertEquals(1, frames.size());
    assertEquals("main", frames.get(0).getFunctionName());
  }

  @Test
  void filtersJcpCompilePackages() {
    StackTraceElement[] jvmStack = {
        new StackTraceElement("com.elminster.jcp.compile.BytecodeGenerator", "compile", "BytecodeGenerator.java", 100),
        new StackTraceElement("com.example.Main", "main", "Main.java", 5)
    };

    CallStack result = adapter.convert(jvmStack);

    List<StackFrame> frames = result.getFrames();
    assertEquals(1, frames.size());
    assertEquals("main", frames.get(0).getFunctionName());
  }

  @Test
  void filtersJcpEvalPackages() {
    StackTraceElement[] jvmStack = {
        new StackTraceElement("com.elminster.jcp.eval.EvalVisitor", "visit", "EvalVisitor.java", 50),
        new StackTraceElement("com.example.Main", "main", "Main.java", 5)
    };

    CallStack result = adapter.convert(jvmStack);

    List<StackFrame> frames = result.getFrames();
    assertEquals(1, frames.size());
    assertEquals("main", frames.get(0).getFunctionName());
  }

  @Test
  void filtersAsmPackages() {
    StackTraceElement[] jvmStack = {
        new StackTraceElement("org.objectweb.asm.ClassWriter", "visit", "ClassWriter.java", 200),
        new StackTraceElement("com.example.Main", "main", "Main.java", 5)
    };

    CallStack result = adapter.convert(jvmStack);

    List<StackFrame> frames = result.getFrames();
    assertEquals(1, frames.size());
    assertEquals("main", frames.get(0).getFunctionName());
  }

  @Test
  void preservesFrameOrder() {
    mapping.registerClass("com/example/Main", "main.jcp");
    mapping.registerClass("com/example/Utils", "utils.jcp");

    // JVM stack trace has most recent frame at index 0
    StackTraceElement[] jvmStack = {
        new StackTraceElement("com.example.Utils", "helper", "Utils.java", 20), // newest (index 0)
        new StackTraceElement("com.example.Main", "main", "Main.java", 10)      // oldest (index 1)
    };

    CallStack result = adapter.convert(jvmStack);

    List<StackFrame> frames = result.getFrames();
    assertEquals(2, frames.size());
    // After conversion, most recent should be first (top of stack)
    assertEquals("helper", frames.get(0).getFunctionName());
    assertEquals("main", frames.get(1).getFunctionName());
  }

  @Test
  void handlesNegativeLineNumber() {
    StackTraceElement[] jvmStack = {
        new StackTraceElement("com.example.Main", "main", "Main.java", -1) // native method
    };

    CallStack result = adapter.convert(jvmStack);

    List<StackFrame> frames = result.getFrames();
    assertEquals(1, frames.size());
    assertEquals(1, frames.get(0).getLocation().getStartLine()); // defaults to 1
  }

  @Test
  void handlesZeroLineNumber() {
    StackTraceElement[] jvmStack = {
        new StackTraceElement("com.example.Main", "main", "Main.java", 0)
    };

    CallStack result = adapter.convert(jvmStack);

    List<StackFrame> frames = result.getFrames();
    assertEquals(1, frames.size());
    assertEquals(1, frames.get(0).getLocation().getStartLine()); // defaults to 1
  }

  @Test
  void columnDefaultsToOne() {
    mapping.registerClass("com/example/Main", "main.jcp");

    StackTraceElement[] jvmStack = {
        new StackTraceElement("com.example.Main", "main", "Main.java", 42)
    };

    CallStack result = adapter.convert(jvmStack);

    List<StackFrame> frames = result.getFrames();
    assertEquals(1, frames.get(0).getLocation().getStartColumn());
  }
}
