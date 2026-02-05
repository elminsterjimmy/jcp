package com.elminster.jcp.eval.data;

import java.lang.reflect.Method;

/**
 * Stores method signature information for external Java methods.
 * Used by ExternalClassType to represent methods on external Java classes
 * that can be called from compiled JCP code.
 */
public class ExternalMethodDef {

    private final Method javaMethod;
    private final String name;
    private final DataType[] parameterTypes;
    private final DataType returnType;
    private final String descriptor;
    private final boolean isStatic;

    public ExternalMethodDef(Method javaMethod, String name, DataType[] parameterTypes,
                             DataType returnType, String descriptor, boolean isStatic) {
        this.javaMethod = javaMethod;
        this.name = name;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.descriptor = descriptor;
        this.isStatic = isStatic;
    }

    public Method getJavaMethod() {
        return javaMethod;
    }

    public String getName() {
        return name;
    }

    public DataType[] getParameterTypes() {
        return parameterTypes;
    }

    public DataType getReturnType() {
        return returnType;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public String toString() {
        return "ExternalMethodDef{" +
            "name='" + name + '\'' +
            ", descriptor='" + descriptor + '\'' +
            ", isStatic=" + isStatic +
            '}';
    }
}
