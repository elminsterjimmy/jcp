package com.elminster.jcp.compile.debug;

import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.exception.CallStack;
import com.elminster.jcp.exception.StackFrame;

import java.util.Set;

/**
 * Converts JVM stack traces to JCP CallStack.
 * Filters internal JVM and JCP framework frames to provide clean stack traces.
 *
 * <p>This adapter transforms {@link StackTraceElement} arrays (from JVM exceptions)
 * into JCP's {@link CallStack} format, mapping JVM class/method names back to
 * JCP source file/function names.
 *
 * <p>Filtered packages include:
 * <ul>
 *   <li>JVM internal packages: java., jdk., sun.</li>
 *   <li>JCP framework packages: com.elminster.jcp.compile, com.elminster.jcp.eval</li>
 *   <li>ASM library: org.objectweb.asm</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * SourceMapping mapping = new SourceMapping();
 * mapping.registerClass("Main", "main.jcp");
 * mapping.registerFunction("Main", "calculate", "calculate");
 *
 * JvmStackTraceAdapter adapter = new JvmStackTraceAdapter(mapping);
 * try {
 *     // ... code that might throw
 * } catch (Exception e) {
 *     CallStack jcpStack = adapter.convert(e.getStackTrace());
 *     throw new JcpException(e.getMessage(), null, jcpStack);
 * }
 * }</pre>
 */
public class JvmStackTraceAdapter {

  private static final Set<String> FILTERED_PACKAGE_PREFIXES = Set.of(
      "java.",
      "jdk.",
      "sun.",
      "com.elminster.jcp.compile",
      "com.elminster.jcp.eval",
      "org.objectweb.asm"
  );

  private final SourceMapping mapping;

  /**
   * Create a new adapter with the given source mapping.
   *
   * @param mapping the source mapping from compilation
   */
  public JvmStackTraceAdapter(SourceMapping mapping) {
    this.mapping = mapping;
  }

  /**
   * Convert a JVM stack trace to a JCP CallStack.
   *
   * <p>The conversion process:
   * <ol>
   *   <li>Filter out JVM/JCP internal frames</li>
   *   <li>Map JVM class names to JCP source files</li>
   *   <li>Map JVM method names to JCP function names</li>
   *   <li>Use JVM line numbers (columns default to 1)</li>
   * </ol>
   *
   * @param jvmStack the JVM stack trace elements
   * @return JCP CallStack with mapped frames
   */
  public CallStack convert(StackTraceElement[] jvmStack) {
    CallStack stack = new CallStack();

    // Process in reverse order so most recent frame ends up on top
    for (int i = jvmStack.length - 1; i >= 0; i--) {
      StackTraceElement elem = jvmStack[i];

      // Filter JVM/JCP internal frames
      if (shouldFilter(elem.getClassName())) {
        continue;
      }

      // Convert to JCP format
      String jvmClassName = toInternalName(elem.getClassName());
      String filename = mapping.getFilename(jvmClassName);
      String funcName = mapping.getFunctionName(jvmClassName, elem.getMethodName());

      // Build source location (JVM doesn't track columns, so default to 1)
      SourceLocation loc = SourceLocation.of(
          filename != null ? filename : elem.getFileName(),
          elem.getLineNumber() > 0 ? elem.getLineNumber() : 1,
          1
      );

      // Use JVM names as fallback if not mapped
      stack.push(StackFrame.of(
          funcName != null ? funcName : elem.getMethodName(),
          loc
      ));
    }

    return stack;
  }

  /**
   * Check if a class should be filtered from the stack trace.
   *
   * @param className fully qualified class name (with dots)
   * @return true if the class should be excluded
   */
  private boolean shouldFilter(String className) {
    for (String prefix : FILTERED_PACKAGE_PREFIXES) {
      if (className.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Convert a fully qualified class name to JVM internal name.
   * Example: "com.example.Main" → "com/example/Main"
   *
   * @param className fully qualified class name
   * @return JVM internal name
   */
  private String toInternalName(String className) {
    return className.replace('.', '/');
  }
}
