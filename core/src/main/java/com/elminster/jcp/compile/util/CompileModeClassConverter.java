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
import java.util.HashSet;
import java.util.Set;

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
 *
 * <p>Circular dependency between classes (A's method returns B; B's method takes A)
 * is handled via an in-progress set passed through recursive calls, following the
 * same sentinel pattern used by Spring's bean factory: a class is added to the set
 * before its methods are populated, so any re-entrant call for the same FQN finds
 * the class already in-progress and leaves the stub as-is.
 */
public final class CompileModeClassConverter {

    private CompileModeClassConverter() {
    }

    /**
     * Register a Java class for use in compiled JCP code.
     * Analyzes the class via reflection and registers all public methods.
     * Unknown parameter/return types that are not yet registered are registered
     * first (dependency-first ordering), with circular references left as stubs.
     *
     * @param clazz  the Java class to register
     * @param ctx    the compile context
     * @param module the module name (for namespacing)
     */
    public static void registerClass(Class<?> clazz, CompileContext ctx, String module) {
        registerClass(clazz, ctx, module, new HashSet<>());
    }

    /**
     * Internal registration with an in-progress set to detect circular references.
     * Any FQN in {@code inProgress} is currently being registered higher up the
     * call stack; re-entering it would loop, so we leave the stub in place.
     */
    private static void registerClass(Class<?> clazz, CompileContext ctx, String module,
                                       Set<String> inProgress) {
        String fqn = clazz.getName();
        String name = clazz.getSimpleName();

        // Circular reference guard — this class is already being registered above us.
        if (inProgress.contains(fqn)) {
            return;
        }

        // FQN lookup — canonical check for this exact class.
        DataType byFqn = ctx.getDataTypeByFqn(fqn);
        if (byFqn instanceof ExternalClassType) {
            ExternalClassType existingExt = (ExternalClassType) byFqn;
            if (!existingExt.getInstanceMethods().isEmpty() || !existingExt.getStaticMethods().isEmpty()) {
                // Already fully registered — nothing to do.
                return;
            }
            // A stub was created by mapJavaTypeToDataType when this class appeared as a
            // parameter/return type of another class. Mark in-progress and fully register now.
            inProgress.add(fqn);
            addMethodsAndConstructors(existingExt, clazz, ctx, module, inProgress);
            inProgress.remove(fqn);
            return;
        }

        // No FQN entry yet. Check simple name for a system/struct type that would block.
        // If the simple name is already ambiguous (two external classes share it), fall through
        // to register this one under its own FQN — it's reachable by FQN even if not by simple name.
        try {
            DataType bySimpleName = ctx.getDataType(name);
            if (bySimpleName != null && !(bySimpleName instanceof ExternalClassType)) {
                // A user-declared JCP type occupies this simple name — skip silently.
                return;
            }
        } catch (com.elminster.jcp.compile.exception.CompileException ignored) {
            // Simple name already ambiguous — proceed to register under FQN.
        }

        // Not yet registered. Create, register, then add methods.
        ExternalClassType type = new ExternalClassType(name, clazz);
        ctx.addDataType(type);
        inProgress.add(fqn);
        addMethodsAndConstructors(type, clazz, ctx, module, inProgress);
        inProgress.remove(fqn);
    }

    private static void addMethodsAndConstructors(ExternalClassType type, Class<?> clazz,
                                                   CompileContext ctx, String module,
                                                   Set<String> inProgress) {
        for (Method method : clazz.getMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                ExternalMethodDef methodDef = createMethodDef(method, ctx, module, inProgress);
                if (Modifier.isStatic(method.getModifiers())) {
                    type.addStaticMethod(methodDef);
                } else {
                    type.addInstanceMethod(methodDef);
                }
            }
        }
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (Modifier.isPublic(constructor.getModifiers())) {
                type.addConstructor(createConstructorDef(constructor, type, ctx, module, inProgress));
            }
        }
    }

    /**
     * Create an ExternalMethodDef from a Java Method.
     * Unknown parameter/return types are registered as opaque stubs into the context.
     */
    private static ExternalMethodDef createMethodDef(Method method, CompileContext ctx, String module,
                                                      Set<String> inProgress) {
        String name = method.getName();
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        Class<?>[] javaParams = method.getParameterTypes();
        DataType[] paramTypes = new DataType[javaParams.length];
        for (int i = 0; i < javaParams.length; i++) {
            paramTypes[i] = mapJavaTypeToDataType(javaParams[i], ctx, module, inProgress);
        }

        DataType returnType = mapJavaTypeToDataType(method.getReturnType(), ctx, module, inProgress);
        String descriptor = Type.getMethodDescriptor(method);

        return new ExternalMethodDef(method, name, paramTypes, returnType, descriptor, isStatic);
    }

    /**
     * Create an ExternalMethodDef from a Java Constructor.
     * Unknown parameter types are registered as opaque stubs into the context.
     */
    private static ExternalMethodDef createConstructorDef(Constructor<?> constructor,
                                                           ExternalClassType type,
                                                           CompileContext ctx, String module,
                                                           Set<String> inProgress) {
        Class<?>[] javaParams = constructor.getParameterTypes();
        DataType[] paramTypes = new DataType[javaParams.length];
        for (int i = 0; i < javaParams.length; i++) {
            paramTypes[i] = mapJavaTypeToDataType(javaParams[i], ctx, module, inProgress);
        }

        String descriptor = Type.getConstructorDescriptor(constructor);
        return new ExternalMethodDef(null, "<init>", paramTypes, type, descriptor, false);
    }

    /**
     * Map a Java type to a JCP DataType, registering unknown class types if not already present.
     * Entry point for external callers (e.g. TypeMapper). Uses a fresh in-progress set.
     */
    public static DataType mapJavaTypeToDataType(Class<?> javaType, CompileContext ctx, String module) {
        return mapJavaTypeToDataType(javaType, ctx, module, new HashSet<>());
    }

    /**
     * Map a Java type to a JCP DataType.
     *
     * <p>Known primitive/standard types are returned directly. Unknown class types are
     * registered via {@link #registerClass(Class, CompileContext, String, Set)} using
     * dependency-first ordering — the type's own methods are populated before returning,
     * unless a circular dependency is detected (type already in {@code inProgress}), in
     * which case a stub (no methods) is registered and returned to break the cycle.
     */
    private static DataType mapJavaTypeToDataType(Class<?> javaType, CompileContext ctx, String module,
                                                   Set<String> inProgress) {
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
        // Anonymous/synthetic classes have no usable simple name — fall back to ANY
        String simpleName = javaType.getSimpleName();
        if (simpleName.isEmpty()) {
            return SystemDataType.ANY;
        }
        String fqn = javaType.getName();
        // FQN lookup — return immediately if already registered (fully or as in-progress stub).
        DataType byFqn = ctx.getDataTypeByFqn(fqn);
        if (byFqn != null) {
            return byFqn;
        }
        // Check if a non-ExternalClassType (struct/system) is registered under this simple name.
        // If so, return it — JCP-source types take precedence and no stub is needed.
        try {
            DataType existing = ctx.getDataType(simpleName);
            if (existing != null && !(existing instanceof ExternalClassType)) {
                return existing;
            }
        } catch (com.elminster.jcp.compile.exception.CompileException ignored) {
            // Simple name already ambiguous between external classes — fall through.
        }
        // Register the type (dependency-first, circular guard via inProgress).
        // registerClass will create the ExternalClassType, add it to the context, and populate
        // its methods — unless inProgress detects a cycle, in which case it creates a stub and
        // returns without methods (same as the old opaque-stub behaviour for that cycle edge).
        registerClass(javaType, ctx, module, inProgress);
        // After registerClass the type is in the context (fully or as a cycle-stub); return it.
        DataType registered = ctx.getDataTypeByFqn(fqn);
        if (registered != null) {
            return registered;
        }
        // Fallback: should not reach here, but guard defensively.
        return SystemDataType.ANY;
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
        // float maps to DOUBLE (widening; JCP has no separate float type)
        if (javaType == float.class || javaType == Float.class) {
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
        // byte and short share the JVM int slot; map to INT
        if (javaType == byte.class || javaType == Byte.class
                || javaType == short.class || javaType == Short.class) {
            return SystemDataType.INT;
        }
        // long — no JCP LONG type yet; callers should not emit LRETURN for these
        if (javaType == long.class || javaType == Long.class) {
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
