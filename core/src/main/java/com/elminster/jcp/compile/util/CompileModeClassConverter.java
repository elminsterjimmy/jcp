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
 */
public final class CompileModeClassConverter {

    private CompileModeClassConverter() {
    }

    /**
     * Register a Java class for use in compiled JCP code.
     * Analyzes the class via reflection and registers all public methods.
     *
     * @param clazz  the Java class to register
     * @param ctx    the compile context
     * @param module the module name (for namespacing)
     */
    public static void registerClass(Class<?> clazz, CompileContext ctx, String module) {
        String name = clazz.getSimpleName();

        // Create ExternalClassType
        ExternalClassType type = new ExternalClassType(name, clazz);

        // Analyze and register all public methods
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers())) {
                ExternalMethodDef methodDef = createMethodDef(method);
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
                ExternalMethodDef ctorDef = createConstructorDef(constructor, type);
                type.addConstructor(ctorDef);
            }
        }

        // Register the type in compile context
        ctx.addDataType(type);
    }

    /**
     * Create an ExternalMethodDef from a Java Method.
     */
    private static ExternalMethodDef createMethodDef(Method method) {
        String name = method.getName();
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        // Convert Java parameter types to JCP DataTypes
        Class<?>[] javaParams = method.getParameterTypes();
        DataType[] paramTypes = new DataType[javaParams.length];
        for (int i = 0; i < javaParams.length; i++) {
            paramTypes[i] = mapJavaTypeToDataType(javaParams[i]);
        }

        // Convert Java return type to JCP DataType
        DataType returnType = mapJavaTypeToDataType(method.getReturnType());

        // Build JVM method descriptor
        String descriptor = Type.getMethodDescriptor(method);

        return new ExternalMethodDef(method, name, paramTypes, returnType, descriptor, isStatic);
    }

    /**
     * Create an ExternalMethodDef from a Java Constructor.
     */
    private static ExternalMethodDef createConstructorDef(Constructor<?> constructor, ExternalClassType type) {
        // Convert Java parameter types to JCP DataTypes
        Class<?>[] javaParams = constructor.getParameterTypes();
        DataType[] paramTypes = new DataType[javaParams.length];
        for (int i = 0; i < javaParams.length; i++) {
            paramTypes[i] = mapJavaTypeToDataType(javaParams[i]);
        }

        // Constructor returns the class type itself
        DataType returnType = type;

        // Build JVM constructor descriptor
        String descriptor = Type.getConstructorDescriptor(constructor);

        // Note: Constructor doesn't have a Java Method, so we pass null
        // The descriptor is enough for bytecode generation
        return new ExternalMethodDef(null, "<init>", paramTypes, returnType, descriptor, false);
    }

    /**
     * Map a Java type to a JCP DataType.
     */
    public static DataType mapJavaTypeToDataType(Class<?> javaType) {
        // Primitive types
        if (javaType == int.class || javaType == Integer.class) {
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

        // Array types
        if (javaType == int[].class) {
            return SystemDataType.INT_ARRAY;
        }
        if (javaType == double[].class) {
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

        // Object and other types map to ANY
        // This allows parameters like Object to accept any JCP value
        return SystemDataType.ANY;
    }
}
