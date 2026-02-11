package com.elminster.jcp.compile.debug;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps JVM class/method names to JCP source file/function names.
 * Populated during compilation, used at runtime for stack trace conversion.
 *
 * <p>This class maintains bidirectional mappings:
 * <ul>
 *   <li>JVM class name → JCP source file name</li>
 *   <li>JVM class.method → JCP function name</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * SourceMapping mapping = new SourceMapping();
 * mapping.registerClass("com/example/Main", "main.jcp");
 * mapping.registerFunction("com/example/Main", "calculate", "calculate");
 *
 * // Later, when converting stack traces:
 * String jcpFile = mapping.getFilename("com/example/Main");
 * String jcpFunc = mapping.getFunctionName("com/example/Main", "calculate");
 * }</pre>
 */
public class SourceMapping {

  private final Map<String, String> classToFile = new HashMap<>();
  private final Map<String, String> methodToFunction = new HashMap<>();

  /**
   * Register a mapping from JVM class name to JCP source file.
   *
   * @param jvmClassName  JVM internal class name (e.g., "com/example/Main")
   * @param jcpFilename   JCP source file name (e.g., "main.jcp")
   */
  public void registerClass(String jvmClassName, String jcpFilename) {
    classToFile.put(jvmClassName, jcpFilename);
  }

  /**
   * Register a mapping from JVM method to JCP function name.
   *
   * @param jvmClassName   JVM internal class name
   * @param jvmMethodName  JVM method name
   * @param jcpFunctionName JCP function name
   */
  public void registerFunction(String jvmClassName, String jvmMethodName, String jcpFunctionName) {
    String key = jvmClassName + "." + jvmMethodName;
    methodToFunction.put(key, jcpFunctionName);
  }

  /**
   * Get the JCP source file name for a JVM class.
   *
   * @param jvmClassName JVM internal class name
   * @return JCP source file name, or null if not mapped
   */
  public String getFilename(String jvmClassName) {
    return classToFile.get(jvmClassName);
  }

  /**
   * Get the JCP function name for a JVM method.
   *
   * @param jvmClassName  JVM internal class name
   * @param jvmMethodName JVM method name
   * @return JCP function name, or null if not mapped
   */
  public String getFunctionName(String jvmClassName, String jvmMethodName) {
    String key = jvmClassName + "." + jvmMethodName;
    return methodToFunction.get(key);
  }

  /**
   * Check if a class is registered in this mapping.
   *
   * @param jvmClassName JVM internal class name
   * @return true if the class has a mapping
   */
  public boolean hasClass(String jvmClassName) {
    return classToFile.containsKey(jvmClassName);
  }

  /**
   * Clear all mappings.
   */
  public void clear() {
    classToFile.clear();
    methodToFunction.clear();
  }
}
