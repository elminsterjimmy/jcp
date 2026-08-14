package com.elminster.jcp.compile.util;

import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.eval.data.ExternalMethodDef;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Utility class for registering external Java classes in compile mode.
 * Analyzes Java classes via reflection and creates ExternalClassType instances
 * that can be used for method call compilation.
 *
 * <p>When a class is registered, parameter and return types that are themselves
 * non-primitive classes (e.g. {@code CharSequence}) are registered as opaque stubs —
 * an {@link ExternalClassType} with no methods — so that Java-level assignability
 * checks (e.g. STRING satisfies a CharSequence parameter) work correctly without
 * cascading into the full JVM standard library hierarchy.
 */
public final class CompileModeClassConverter {

    private CompileModeClassConverter() {
    }

    /**
     * Register a Java class for use in compiled JCP code.
     * Analyzes the class via reflection and registers all public methods.
     * Unknown parameter/return types are registered as opaque stubs (no methods),
     * preventing cascade into deep JVM stdlib hierarchies.
     *
     * @param clazz  the Java class to register
     * @param ctx    the compile context
     * @param module the module name (for namespacing)
     */
    public static void registerClass(Class<?> clazz, CompileContext ctx, String module) {
        String name = clazz.getSimpleName();

        // Avoid re-registering a type already in the context
        if (ctx.getDataType(name) != null) {
            return;
        }

        // Create ExternalClassType and register it immediately to break potential recursion
        ExternalClassType type = new ExternalClassType(name, clazz);
        ctx.addDataType(type);

        // Analyze and register all public methods
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers())) {
                ExternalMethodDef methodDef = createMethodDef(method, ctx, module);
                if (Modifier.isStatic(method.getModifiers())) {
                    type.addStaticMethod(methodDef);
                } else {
                    type.addInstanceMethod(methodDef);
                }
            }
        }

        // Analyze and register all public constructors
        Constructor<?>[] constructors = clazz.getConstructors();
        for (Constructor<?> constructor : constructors) {
            if (Modifier.isPublic(constructor.getModifiers())) {
                ExternalMethodDef ctorDef = createConstructorDef(constructor, type, ctx, module);
                type.addConstructor(ctorDef);
            }
        }
    }

    /**
     * Create an ExternalMethodDef from a Java Method.
     * Unknown parameter/return types are registered as opaque stubs into the context.
     */
    private static ExternalMethodDef createMethodDef(Method method, CompileContext ctx, String module) {
        String name = method.getName();
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        Class<?>[] javaParams = method.getParameterTypes();
        DataType[] paramTypes = new DataType[javaParams.length];
        for (int i = 0; i < javaParams.length; i++) {
            paramTypes[i] = mapJavaTypeToDataType(javaParams[i], ctx, module);
        }

        DataType returnType = mapJavaTypeToDataType(method.getReturnType(), ctx, module);
        String descriptor = Type.getMethodDescriptor(method);

        return new ExternalMethodDef(method, name, paramTypes, returnType, descriptor, isStatic);
    }

    /**
     * Create an ExternalMethodDef from a Java Constructor.
     * Unknown parameter types are registered as opaque stubs into the context.
     */
    private static ExternalMethodDef createConstructorDef(Constructor<?> constructor,
                                                           ExternalClassType type,
                                                           CompileContext ctx, String module) {
        Class<?>[] javaParams = constructor.getParameterTypes();
        DataType[] paramTypes = new DataType[javaParams.length];
        for (int i = 0; i < javaParams.length; i++) {
            paramTypes[i] = mapJavaTypeToDataType(javaParams[i], ctx, module);
        }

        String descriptor = Type.getConstructorDescriptor(constructor);
        return new ExternalMethodDef(null, "<init>", paramTypes, type, descriptor, false);
    }

    /**
     * Map a Java type to a JCP DataType, registering unknown class types as opaque stubs.
     *
     * <p>Unknown non-primitive, non-array types (e.g. {@code CharSequence},
     * {@code IntStream}) are registered as an opaque {@link ExternalClassType} stub
     * with no methods. This is enough for Java-level assignability checks (e.g.
     * STRING satisfies a CharSequence parameter) without triggering a full cascade
     * into the JVM standard library hierarchy.
     *
     * <p>To expose a type's own methods to JCP, call {@link #registerClass} explicitly.
     */
    public static DataType mapJavaTypeToDataType(Class<?> javaType, CompileContext ctx, String module) {
        DataType known = mapJavaTypeToDataType(javaType);
        if (known != SystemDataType.ANY) {
            return known;
        }
        // Object.class maps to ANY — the universal base, no stub needed
        if (javaType == Object.class) {
            return SystemDataType.ANY;
        }
        if (javaType.isPrimitive() || javaType.isArray()) {
            return SystemDataType.ANY;
        }
        // Register an opaque stub — no methods — so assignability checks resolve correctly
        // without cascading into the full Java stdlib hierarchy.
        // Key by simple name (JCP-visible) but detect same-simple-name/different-package
        // collisions by comparing the stored Java class; fall back to ANY on collision.
        String simpleName = javaType.getSimpleName();
        if (simpleName.isEmpty()) {
            return SystemDataType.ANY;
        }
        DataType existing = ctx.getDataType(simpleName);
        if (existing instanceof ExternalClassType) {
            // If the stored class differs (same simple name, different package) fall back to ANY
            // rather than silently returning the wrong stub.
            return ((ExternalClassType) existing).getJavaClass() == javaType
                    ? existing : SystemDataType.ANY;
        }
        if (existing != null) {
            return existing;
        }
        ExternalClassType stub = new ExternalClassType(simpleName, javaType);
        ctx.addDataType(stub);
        return stub;
    }

    /**
     * Map a Java type to a JCP DataType (context-free).
     * Unknown/unsupported types return {@code ANY}.
     *
     * <p>Mapped types:
     * <ul>
     *   <li>{@code int}, {@code Integer}, {@code char}, {@code Character} → {@code INT}
     *       (char and int share the same JVM slot)</li>
     *   <li>{@code double}, {@code Double} → {@code DOUBLE}</li>
     *   <li>{@code boolean}, {@code Boolean} → {@code BOOLEAN}</li>
     *   <li>{@code void}, {@code Void} → {@code VOID}</li>
     *   <li>{@code String} → {@code STRING}</li>
     *   <li>Array variants → corresponding array types</li>
     *   <li>All other types → {@code ANY} (use the context-aware overload to register them)</li>
     * </ul>
     */
    public static DataType mapJavaTypeToDataType(Class<?> javaType) {
        // Primitive types
        if (javaType == int.class || javaType == Integer.class) {
            return SystemDataType.INT;
        }
        // char and int share the same JVM slot (I); map char to INT
        if (javaType == char.class || javaType == Character.class) {
            return SystemDataType.INT;
        }
        if (javaType == double.class || javaType == Double.class) {
            return SystemDataType.DOUBLE;
        }
        if (javaType == boolean.class || javaType == Boolean.class) {
            return SystemDataType.BOOLEAN;
        }
        if (javaType == void.class || javaType == Void.class) {
            return SystemDataType.VOID;
        }
        if (javaType == String.class) {
            return SystemDataType.STRING;
        }
        // Object maps to ANY — it is the universal base in both Java and JCP
        if (javaType == Object.class) {
            return SystemDataType.ANY;
        }

        // Array types — primitive arrays
        if (javaType == int[].class || javaType == char[].class
                || javaType == byte[].class || javaType == short[].class) {
            return SystemDataType.INT_ARRAY;
        }
        if (javaType == long[].class) {
            return SystemDataType.ANY_ARRAY;
        }
        if (javaType == double[].class || javaType == float[].class) {
            return SystemDataType.DOUBLE_ARRAY;
        }
        if (javaType == boolean[].class) {
            return SystemDataType.BOOLEAN_ARRAY;
        }
        if (javaType == String[].class) {
            return SystemDataType.STRING_ARRAY;
        }
        if (javaType == Integer[].class) {
            return SystemDataType.INT_ARRAY;
        }
        if (javaType == Double[].class) {
            return SystemDataType.DOUBLE_ARRAY;
        }
        if (javaType == Boolean[].class) {
            return SystemDataType.BOOLEAN_ARRAY;
        }
        if (javaType == Object[].class) {
            return SystemDataType.ANY_ARRAY;
        }

        // Unknown class type — caller should use the context-aware overload to register it
        return SystemDataType.ANY;
    }
}
