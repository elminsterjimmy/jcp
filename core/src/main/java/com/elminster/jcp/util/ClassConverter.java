package com.elminster.jcp.util;

import com.elminster.common.util.ReflectUtil;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.eval.excpetion.InitializeException;
import com.elminster.jcp.module.AbstractModuleFunction;
import com.google.common.reflect.ClassPath;

import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Set;

/**
 * The helper class to convert a class to a registered datatype and functions.
 *
 * @author jgu
 * @version 1.0
 */
public class ClassConverter {

    public static void registerClass(Class<?> clazz, EvalContext context, String module) {
        registerClass(clazz, context, module, new HashSet<>());
    }

    /**
     * Internal registration with an in-progress set to detect circular references.
     * Any FQN in {@code inProgress} is currently being registered higher up the call
     * stack; re-entering it would loop, so we leave the stub in place.
     */
    private static void registerClass(Class<?> clazz, EvalContext context, String module,
                                       Set<String> inProgress) {
        String fqn = clazz.getName();
        String name = clazz.getSimpleName();

        // Circular reference guard.
        if (inProgress.contains(fqn)) {
            return;
        }

        // FQN lookup — if fully registered, nothing to do.
        DataType byFqn = context.getDataTypeByFqn(fqn);
        if (byFqn instanceof ExternalClassType) {
            ExternalClassType existingExt = (ExternalClassType) byFqn;
            if (!existingExt.getInstanceMethods().isEmpty() || !existingExt.getStaticMethods().isEmpty()) {
                return;
            }
            // Stub exists — mark in-progress and fully register it now.
            inProgress.add(fqn);
            registerMethods(clazz, context, module, existingExt, inProgress);
            inProgress.remove(fqn);
            return;
        }

        // Simple-name lookup to check for system/struct types.
        // If the name is already ambiguous (two external classes share it), fall through to
        // register this one under its own FQN — reachable by FQN even if not by simple name.
        DataType existing;
        try {
            existing = byFqn != null ? byFqn : DataTypeUtils.getDataType(name, context);
        } catch (com.elminster.jcp.eval.excpetion.EvaluationException ignored) {
            existing = null; // ambiguous simple name — proceed to FQN-keyed registration
        }
        if (existing instanceof ExternalClassType) {
            ExternalClassType existingExt = (ExternalClassType) existing;
            if (existingExt.getJavaClass() == clazz
                    && (!existingExt.getInstanceMethods().isEmpty()
                        || !existingExt.getStaticMethods().isEmpty())) {
                return;
            }
        } else if (existing != null && !(existing instanceof DataTypeImpl)) {
            return;
        }

        DataType dt;
        if (existing instanceof ExternalClassType && ((ExternalClassType) existing).getJavaClass() == clazz) {
            dt = existing;
        } else {
            dt = new ExternalClassType(name, clazz);
            context.addDataType(dt);
        }
        inProgress.add(fqn);
        registerMethods(clazz, context, module, dt, inProgress);
        inProgress.remove(fqn);
    }

    private static void registerMethods(Class<?> clazz, EvalContext context, String module,
                                         DataType dt, Set<String> inProgress) {
        for (Method method : clazz.getMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                if (Modifier.isStatic(method.getModifiers())) {
                    registerStaticMethod(method, context, module, dt, inProgress);
                } else {
                    registerNonStaticMethod(method, context, module, dt, inProgress);
                }
            }
        }
        registerConstructors(clazz, context, module, dt, inProgress);
    }

    private static void registerStaticMethod(Method method, EvalContext context, String module,
                                              DataType dt, Set<String> inProgress) {
        String methodName = method.getName();
        Parameter[] parameters = method.getParameters();
        ParameterDef[] parameterDefs = getParameterDefs(parameters, context, module, inProgress);

        Class<?> returnType = method.getReturnType();
        DataType returnDataType = getDataType(returnType, context, module, inProgress);

        Function function = new AbstractModuleFunction() {
            @Override
            public String getModule() {
                return module;
            }

            @Override
            public String getName() {
                return FunctionUtils.getModuleFunctionName(module, dt.getName(), methodName);
            }

            @Override
            public Identifier getId() {
                return new IdentifierExpression(getName());
            }

            @Override
            public ParameterDef[] getParameterDefs() {
                return parameterDefs;
            }

            @Override
            public DataType getResultDataType() {
                return returnDataType;
            }

            @Override
            protected Data doFunction(Data[] arguments, EvalContext evalContext) {
                Object[] argValues = getArgumentValues(arguments);
                Object result = null;
                try {
                    result = ReflectUtil.invoke(null, method, argValues);
                } catch (Exception e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new RuntimeException("Invoke failed", e);
                }
                return new AnyData(result, returnDataType);
            }
        };
        addFunctionIfAbsent(function, context);
    }

    private static void throwInvokeException(ReflectiveOperationException e) {
        if (e instanceof InvocationTargetException) {
            Throwable targetException = ((InvocationTargetException) e).getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            }
        }
        throw new RuntimeException(e);
    }

    private static Object[] getArgumentValues(Data[] arguments) {
        Object[] argValues = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            argValues[i] = arguments[i].get();
        }
        return argValues;
    }

    private static ParameterDef[] getParameterDefs(Parameter[] parameters, EvalContext context,
                                                     String module, Set<String> inProgress) {
        ParameterDef[] parameterDefs = new ParameterDef[parameters.length];
        int i = 0;
        for (Parameter parameter : parameters) {
            DataType dataType = getDataType(parameter.getType(), context, module, inProgress);
            String parameterName = parameter.getName();
            parameterDefs[i++] = new ParameterDef(parameterName, dataType);
        }
        return parameterDefs;
    }

    private static void registerNonStaticMethod(Method method, EvalContext context, String module,
                                                  DataType dt, Set<String> inProgress) {
        String methodName = method.getName();
        Parameter[] parameters = method.getParameters();
        DataType[] paramDts = new DataType[parameters.length + 1];
        String[] paramNames = new String[parameters.length + 1];
        DataType returnDt;
        paramNames[0] = "this";
        paramDts[0] = dt;
        int i = 1;
        for (Parameter parameter : parameters) {
            paramDts[i] = getDataType(parameter.getType(), context, module, inProgress);
            paramNames[i] = parameter.getName();
            i++;
        }
        Class<?> returnType = method.getReturnType();
        returnDt = getDataType(returnType, context, module, inProgress);

        Function function = new AbstractModuleFunction() {
            @Override
            public String getModule() {
                return module;
            }

            @Override
            public String getName() {
                return FunctionUtils.getModuleFunctionName(module, dt.getName(), methodName);
            }

            @Override
            public Identifier getId() {
                return new IdentifierExpression(getName());
            }

            @Override
            public ParameterDef[] getParameterDefs() {
                int length = paramDts.length;
                ParameterDef[] dataDefs = new ParameterDef[length];
                for (int i = 0; i < length; i++) {
                    dataDefs[i] = new ParameterDef(paramNames[i], paramDts[i]);
                }
                return dataDefs;
            }

            @Override
            public DataType getResultDataType() {
                return returnDt;
            }

            @Override
            protected Data doFunction(Data[] parameters, EvalContext evalContext) {
                Object target = parameters[0].get();
                Object[] args = new Object[parameters.length - 1];
                for (int i = 1; i < parameters.length; i++) {
                    args[i - 1] = parameters[i].get();
                }
                Object result = null;
                try {
                    // Invoke via the captured Method (from the registered class/interface),
                    // not via target.getClass() — this avoids InaccessibleObjectException
                    // when the runtime type is an internal JDK implementation class.
                    result = method.invoke(target, args);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throwInvokeException(e);
                } catch (Exception e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new RuntimeException("Invoke failed", e);
                }
                return new AnyData(result, returnDt);
            }
        };
        addFunctionIfAbsent(function, context);
    }

    private static void registerConstructors(Class<?> clazz, EvalContext context, String module,
                                              DataType dt, Set<String> inProgress) {
        Constructor<?>[] constructors = clazz.getConstructors();
        for (Constructor<?> constructor : constructors) {
            int modifiers = constructor.getModifiers();
            if (Modifier.isPublic(modifiers)) {
                Parameter[] parameters = constructor.getParameters();
                ParameterDef[] parameterDefs = getParameterDefs(parameters, context, module, inProgress);

                Function constructorFunc = new AbstractModuleFunction() {

                    @Override
                    public String getModule() {
                        return module;
                    }

                    @Override
                    public String getName() {
                        return FunctionUtils.getModuleFunctionName(module, dt.getName(), "new");
                    }

                    @Override
                    public Identifier getId() {
                        return new IdentifierExpression(getName());
                    }

                    @Override
                    public ParameterDef[] getParameterDefs() {
                        return parameterDefs;
                    }

                    @Override
                    public DataType getResultDataType() {
                        return dt;
                    }

                    @Override
                    protected Data doFunction(Data[] parameters, EvalContext evalContext) {
                        Object[] argValues = getArgumentValues(parameters);
                        try {
                            return new AnyData(constructor.newInstance(argValues), dt);
                        } catch (Exception e) {
                            throw new InitializeException(e);
                        }
                    }
                };
                addFunctionIfAbsent(constructorFunc, context);
            }
        }
    }

    /**
     * Add a function to the context only if its full name is not already registered.
     * Prevents AlreadyDeclaredException when the same type is encountered via multiple paths
     * during opaque-stub registration of unknown parameter/return types.
     */
    private static void addFunctionIfAbsent(Function function, EvalContext context) {
        if (context.getFunction(function.getFullName()) == null) {
            context.addFunction(function);
        }
    }

    /**
     * Map a Java class to a JCP DataType, registering unknown types via dependency-first ordering.
     *
     * <p>Unknown class types are registered via {@link #registerClass(Class, EvalContext, String, Set)}
     * so their methods are available, unless a circular dependency is detected via {@code inProgress},
     * in which case a stub is left in place to break the cycle.
     */
    private static DataType getDataType(Class<?> dataType, EvalContext context, String module,
                                         Set<String> inProgress) {
        // Fast-path: primitives and well-known types — mirrors CompileModeClassConverter
        if (dataType == int.class     || dataType == Integer.class)   return SystemDataType.INT;
        if (dataType == char.class    || dataType == Character.class)  return SystemDataType.INT;
        if (dataType == byte.class    || dataType == Byte.class)       return SystemDataType.INT;
        if (dataType == short.class   || dataType == Short.class)      return SystemDataType.INT;
        if (dataType == double.class  || dataType == Double.class)     return SystemDataType.DOUBLE;
        if (dataType == float.class   || dataType == Float.class)      return SystemDataType.DOUBLE;
        if (dataType == boolean.class || dataType == Boolean.class)    return SystemDataType.BOOLEAN;
        if (dataType == void.class    || dataType == Void.class)       return SystemDataType.VOID;
        if (dataType == String.class)                                   return SystemDataType.STRING;
        if (dataType == Object.class)                                   return SystemDataType.ANY;
        if (dataType == long.class    || dataType == Long.class)       return SystemDataType.ANY;

        // Array fast-paths
        if (dataType == int[].class || dataType == char[].class
                || dataType == byte[].class || dataType == short[].class) return SystemDataType.INT_ARRAY;
        if (dataType == long[].class)                                      return SystemDataType.ANY_ARRAY;
        if (dataType == double[].class || dataType == float[].class)       return SystemDataType.DOUBLE_ARRAY;
        if (dataType == boolean[].class)                                   return SystemDataType.BOOLEAN_ARRAY;
        if (dataType == String[].class)                                    return SystemDataType.STRING_ARRAY;
        if (dataType.isArray())                                            return SystemDataType.ANY_ARRAY;

        String simpleName = dataType.getSimpleName();
        if (simpleName.isEmpty()) {
            return SystemDataType.ANY;
        }
        String fqn = dataType.getName();
        // Return immediately if already registered (fully or as in-progress stub).
        DataType byFqn = context.getDataTypeByFqn(fqn);
        if (byFqn != null) {
            return byFqn;
        }
        // JCP-source types take precedence.
        try {
            DataType rdt = DataTypeUtils.getDataType(simpleName, context);
            if (rdt != null && !(rdt instanceof ExternalClassType)) {
                return rdt;
            }
        } catch (com.elminster.jcp.eval.excpetion.EvaluationException ignored) {
            // Simple name already ambiguous — fall through.
        }
        // Register dependency-first; circular references are caught by inProgress.
        registerClass(dataType, context, module, inProgress);
        DataType registered = context.getDataTypeByFqn(fqn);
        if (registered != null) {
            return registered;
        }
        return SystemDataType.ANY;
    }
}
