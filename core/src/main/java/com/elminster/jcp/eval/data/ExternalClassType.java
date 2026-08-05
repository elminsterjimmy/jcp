package com.elminster.jcp.eval.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataType implementation for external Java classes.
 * Used in compile mode to represent Java classes whose methods can be called
 * from compiled JCP code via INVOKESTATIC or INVOKEVIRTUAL.
 */
public class ExternalClassType implements DataType {

    private final String name;
    private final Class<?> javaClass;
    private final Map<String, List<ExternalMethodDef>> staticMethods;
    private final Map<String, List<ExternalMethodDef>> instanceMethods;
    private final List<ExternalMethodDef> constructors;

    public ExternalClassType(String name, Class<?> javaClass) {
        this.name = name;
        this.javaClass = javaClass;
        this.staticMethods = new HashMap<>();
        this.instanceMethods = new HashMap<>();
        this.constructors = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DataType getParent() {
        return SystemDataType.ANY;
    }

    public Class<?> getJavaClass() {
        return javaClass;
    }

    /**
     * Get the JVM internal name for this class (e.g., "com/elminster/jcp/module/base/logger/Logger").
     */
    public String getInternalName() {
        return javaClass.getName().replace('.', '/');
    }

    /**
     * Add a static method to this type.
     */
    public void addStaticMethod(ExternalMethodDef method) {
        staticMethods.computeIfAbsent(method.getName(), k -> new ArrayList<>()).add(method);
    }

    /**
     * Add an instance method to this type.
     */
    public void addInstanceMethod(ExternalMethodDef method) {
        instanceMethods.computeIfAbsent(method.getName(), k -> new ArrayList<>()).add(method);
    }

    /**
     * Add a constructor to this type.
     */
    public void addConstructor(ExternalMethodDef constructor) {
        constructors.add(constructor);
    }

    /**
     * Get constructor by argument types with overload resolution.
     * Returns null if no matching constructor found.
     */
    public ExternalMethodDef getConstructor(DataType[] argTypes) {
        List<ExternalMethodDef> matches = new ArrayList<>();
        for (ExternalMethodDef ctor : constructors) {
            DataType[] params = ctor.getParameterTypes();

            if (params.length != argTypes.length) {
                continue;
            }

            boolean compatible = true;
            for (int i = 0; i < params.length; i++) {
                DataType argType = argTypes[i];
                DataType paramType = params[i];
                // Check type compatibility (hierarchy + widening)
                if (!argType.isCompatibleWith(paramType)) {
                    compatible = false;
                    break;
                }
            }

            if (compatible) {
                matches.add(ctor);
            }
        }

        if (matches.isEmpty()) {
            return null;
        } else if (matches.size() == 1) {
            return matches.get(0);
        } else {
            throw new IllegalArgumentException(
                String.format("Ambiguous constructor call: multiple constructors match the argument types"));
        }
    }

    /**
     * Get static method by name and argument types with overload resolution.
     * Returns null if no matching method found.
     *
     * @throws IllegalArgumentException if multiple methods match (ambiguity)
     */
    public ExternalMethodDef getStaticMethod(String methodName, DataType[] argTypes) {
        return findMethodWithOverloadResolution(methodName, argTypes, staticMethods);
    }

    /**
     * Get instance method by name and argument types with overload resolution.
     * Returns null if no matching method found.
     *
     * @throws IllegalArgumentException if multiple methods match (ambiguity)
     */
    public ExternalMethodDef getInstanceMethod(String methodName, DataType[] argTypes) {
        return findMethodWithOverloadResolution(methodName, argTypes, instanceMethods);
    }

    /**
     * Find a method using overload resolution similar to StructType.
     * 1. Collect all candidates with matching name
     * 2. Filter by parameter count
     * 3. Filter by parameter compatibility (isCastableTo)
     * 4. Return single match, or throw ambiguity error, or return null if no match
     */
    private ExternalMethodDef findMethodWithOverloadResolution(
            String methodName, DataType[] argTypes, Map<String, List<ExternalMethodDef>> methods) {

        List<ExternalMethodDef> candidates = methods.getOrDefault(methodName, Collections.emptyList());
        if (candidates.isEmpty()) {
            return null;
        }

        List<ExternalMethodDef> matches = new ArrayList<>();
        for (ExternalMethodDef method : candidates) {
            DataType[] params = method.getParameterTypes();

            // Filter by parameter count
            if (params.length != argTypes.length) {
                continue;
            }

            // Filter by parameter compatibility (type hierarchy + widening conversions)
            boolean compatible = true;
            for (int i = 0; i < params.length; i++) {
                DataType argType = argTypes[i];
                DataType paramType = params[i];
                // Check type compatibility (hierarchy + widening)
                if (!argType.isCompatibleWith(paramType)) {
                    compatible = false;
                    break;
                }
            }

            if (compatible) {
                matches.add(method);
            }
        }

        if (matches.isEmpty()) {
            return null;
        } else if (matches.size() == 1) {
            return matches.get(0);
        }

        // More than one compatible candidate: prefer an exact-type match over a
        // widening/hierarchy match (e.g. abs(int) vs abs(double) for an INT arg).
        List<ExternalMethodDef> exactMatches = new ArrayList<>();
        for (ExternalMethodDef method : matches) {
            if (DataType.allExactMatch(argTypes, method.getParameterTypes())) {
                exactMatches.add(method);
            }
        }
        if (exactMatches.size() == 1) {
            return exactMatches.get(0);
        }

        // Zero or multiple exact matches: genuinely ambiguous.
        throw new IllegalArgumentException(
            String.format("Ambiguous method call: multiple methods named '%s' match the argument types",
                methodName));
    }

    /**
     * Get all static methods (for debugging/inspection).
     */
    public Map<String, List<ExternalMethodDef>> getStaticMethods() {
        return Collections.unmodifiableMap(staticMethods);
    }

    /**
     * Get all instance methods (for debugging/inspection).
     */
    public Map<String, List<ExternalMethodDef>> getInstanceMethods() {
        return Collections.unmodifiableMap(instanceMethods);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ExternalClassType that = (ExternalClassType) obj;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "ExternalClassType{" + name + "}";
    }
}
