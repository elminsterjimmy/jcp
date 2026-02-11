package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.statement.declaration.MethodDef;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.ast.statement.function.ParameterDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Custom data type for user-defined types (formerly structs).
 * Supports fields, explicit constructors, instance methods, and static methods.
 */
public class StructType implements DataType {

  private static final String METHOD_SIGNATURE_PARAM_SPLITTER = "@";
  private static final String METHOD_SIGNATURE_NAME_SPLITTER = "#";

  private final String name;
  private final List<StructFieldDef> fields;

  // O(1) lookup cache for fields (fixes performance bottleneck)
  private final Map<String, StructFieldDef> fieldCache;

  // Method support - keyed by full signature (name#paramType1@paramType2)
  private final MethodDef constructor;
  private final Map<String, MethodDef> instanceMethods;
  private final Map<String, MethodDef> staticMethods;

  /**
   * Constructor for field-only types (backward compatible).
   */
  public StructType(String name, List<StructFieldDef> fields) {
    this(name, fields, null, Collections.emptyList(), Collections.emptyList());
  }

  /**
   * Constructor with just constructor (methods registered as functions in interpreter).
   * This is the PREFERRED constructor for the interpreter path.
   */
  public StructType(String name, List<StructFieldDef> fields, MethodDef constructor) {
    this(name, fields, constructor, Collections.emptyList(), Collections.emptyList());
  }

  /**
   * Full constructor with methods support (used by compiler path).
   * Note: In interpreter mode, methods should be registered as functions
   * via TypeDeclarationEvaluator, not stored here.
   */
  public StructType(String name, List<StructFieldDef> fields, MethodDef constructor,
                    List<MethodDef> instanceMethods, List<MethodDef> staticMethods) {
    this.name = name;
    this.fields = new ArrayList<>(fields);
    this.constructor = constructor;

    // Build field cache on construction (one-time O(n) cost for O(1) lookups)
    this.fieldCache = new HashMap<>(fields.size());
    for (StructFieldDef field : fields) {
      fieldCache.put(field.getName().getId(), field);
    }

    // Build method caches keyed by full signature for overloading support
    this.instanceMethods = new HashMap<>();
    for (MethodDef method : instanceMethods) {
      String signature = generateMethodSignature(method);
      this.instanceMethods.put(signature, method);
    }

    this.staticMethods = new HashMap<>();
    for (MethodDef method : staticMethods) {
      String signature = generateMethodSignature(method);
      this.staticMethods.put(signature, method);
    }
  }

  /**
   * Generate method signature for overloading: name#paramType1@paramType2
   */
  private static String generateMethodSignature(MethodDef method) {
    StringJoiner paramTypes = new StringJoiner(METHOD_SIGNATURE_PARAM_SPLITTER);
    for (ParameterDef param : method.getParameters()) {
      paramTypes.add(param.getDataType().getName());
    }
    return method.getId().getId() + METHOD_SIGNATURE_NAME_SPLITTER + paramTypes.toString();
  }

  /**
   * Generate method signature from name and argument types.
   */
  public static String generateMethodSignature(String methodName, DataType[] argTypes) {
    StringJoiner paramTypes = new StringJoiner(METHOD_SIGNATURE_PARAM_SPLITTER);
    for (DataType type : argTypes) {
      paramTypes.add(type.getName());
    }
    return methodName + METHOD_SIGNATURE_NAME_SPLITTER + paramTypes.toString();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public DataType getParent() {
    return SystemDataType.ANY;
  }

  public List<StructFieldDef> getFields() {
    return fields;
  }

  /**
   * Get field by name with O(1) lookup.
   */
  public StructFieldDef getField(String fieldName) {
    return fieldCache.get(fieldName);
  }

  /**
   * Get the explicit constructor, or null if using auto-generated constructor.
   */
  public MethodDef getConstructor() {
    return constructor;
  }

  /**
   * Get instance method by name and argument types (supports overloading and type hierarchy).
   * Follows the same pattern as FunCallEvaluator for consistent behavior.
   *
   * @throws IllegalArgumentException if multiple methods match (ambiguity)
   */
  public MethodDef getInstanceMethod(String methodName, DataType[] argTypes) {
    return findMethodWithOverloadResolution(methodName, argTypes, instanceMethods);
  }

  /**
   * Get static method by name and argument types (supports overloading and type hierarchy).
   * Follows the same pattern as FunCallEvaluator for consistent behavior.
   *
   * @throws IllegalArgumentException if multiple methods match (ambiguity)
   */
  public MethodDef getStaticMethod(String methodName, DataType[] argTypes) {
    return findMethodWithOverloadResolution(methodName, argTypes, staticMethods);
  }

  /**
   * Find a method using overload resolution similar to FunCallEvaluator.
   * 1. Collect all candidates with matching name
   * 2. Filter by parameter count
   * 3. Filter by parameter compatibility (isCastableTo)
   * 4. Return single match, or throw ambiguity error, or return null if no match
   */
  private MethodDef findMethodWithOverloadResolution(String methodName, DataType[] argTypes,
                                                      Map<String, MethodDef> methods) {
    List<MethodDef> candidates = new ArrayList<>();

    for (MethodDef method : methods.values()) {
      // Filter by name
      if (!method.getId().getId().equals(methodName)) {
        continue;
      }

      ParameterDef[] params = method.getParameters();

      // Filter by parameter count
      if (params.length != argTypes.length) {
        continue;
      }

      // Filter by parameter compatibility (type hierarchy + widening conversions)
      boolean compatible = true;
      for (int i = 0; i < params.length; i++) {
        DataType argType = argTypes[i];
        DataType paramType = params[i].getDataType();
        // Check type compatibility (hierarchy + widening)
        if (!argType.isCompatibleWith(paramType)) {
          compatible = false;
          break;
        }
      }

      if (compatible) {
        candidates.add(method);
      }
    }

    // Handle results
    if (candidates.isEmpty()) {
      return null;
    } else if (candidates.size() == 1) {
      return candidates.get(0);
    } else {
      // Multiple matches - ambiguity error (same as FunctionAmbiguityException)
      throw new IllegalArgumentException(
          String.format("Ambiguous method call: multiple methods named '%s' match the argument types",
              methodName));
    }
  }

  /**
   * Get all instance methods.
   */
  public Map<String, MethodDef> getInstanceMethods() {
    return Collections.unmodifiableMap(instanceMethods);
  }

  /**
   * Get all static methods.
   */
  public Map<String, MethodDef> getStaticMethods() {
    return Collections.unmodifiableMap(staticMethods);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    StructType that = (StructType) obj;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return "StructType{" + name + "}";
  }
}
