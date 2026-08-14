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

/**
 * The helper class to convert a class to a registered datatype and functions.
 *
 * @author jgu
 * @version 1.0
 */
public class ClassConverter {

    public static void registerClass(Class<?> clazz, EvalContext context, String module) {
        String name = clazz.getSimpleName();

        // FQN lookup first — if this exact class is already registered, reuse it.
        DataType byFqn = context.getDataTypeByFqn(clazz.getName());
        if (byFqn instanceof ExternalClassType) {
            ExternalClassType existingExt = (ExternalClassType) byFqn;
            // Already fully registered — skip method/constructor registration.
            if (!existingExt.getInstanceMethods().isEmpty() || !existingExt.getStaticMethods().isEmpty()) {
                return;
            }
            // Opaque stub for the same class — promote in-place without re-registering.
        }

        // Simple-name lookup to check for system/struct types and detect cross-FQN stubs.
        DataType existing = byFqn != null ? byFqn : DataTypeUtils.getDataType(name, context);
        if (existing instanceof ExternalClassType) {
            ExternalClassType existingExt = (ExternalClassType) existing;
            // Already fully registered for this exact class — skip
            if (existingExt.getJavaClass() == clazz
                    && (!existingExt.getInstanceMethods().isEmpty()
                        || !existingExt.getStaticMethods().isEmpty())) {
                return;
            }
        } else if (existing != null && !(existing instanceof DataTypeImpl)) {
            // Already a fully-registered system or struct type — skip entirely
            return;
        }
        // Use an existing ExternalClassType stub for this exact class, or create a fresh one.
        DataType dt;
        if (existing instanceof ExternalClassType && ((ExternalClassType) existing).getJavaClass() == clazz) {
            dt = existing;
        } else {
            dt = new ExternalClassType(name, clazz);
            context.addDataType(dt);
        }
        Method[] methods = clazz.getMethods();
        // auto register all public methods
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers())) {
                // register static public methods
                if (Modifier.isStatic(method.getModifiers())) {
                    registerStaticMethod(method, context, module, dt);
                } else {
                    // register none static public methods
                    registerNonStaticMethod(method, context, module, dt);
                }
            }
        }
        // register constructor
        registerConstructors(clazz, context, module, dt);
    }

    private static void registerStaticMethod(Method method, EvalContext context, String module, DataType dt) {
        String methodName = method.getName();
        Parameter[] parameters = method.getParameters();
        ParameterDef[] parameterDefs = getParameterDefs(parameters, context, module);

        Class<?> returnType = method.getReturnType();
        DataType returnDataType = getDataType(returnType, context, module);

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

    private static ParameterDef[] getParameterDefs(Parameter[] parameters, EvalContext context, String module) {
        ParameterDef[] parameterDefs = new ParameterDef[parameters.length];
        int i = 0;
        for (Parameter parameter : parameters) {
            DataType dataType = getDataType(parameter.getType(), context, module);
            String parameterName = parameter.getName();
            parameterDefs[i++] = new ParameterDef(parameterName, dataType);
        }
        return parameterDefs;
    }

    private static void registerNonStaticMethod(Method method, EvalContext context, String module, DataType dt) {
        String methodName = method.getName();
        Parameter[] parameters = method.getParameters();
        DataType[] paramDts = new DataType[parameters.length + 1];
        String[] paramNames = new String[parameters.length + 1];
        DataType returnDt;
        paramNames[0] = "this";
        paramDts[0] = dt;
        int i = 1;
        for (Parameter parameter : parameters) {
            paramDts[i] = getDataType(parameter.getType(), context, module);
            paramNames[i] = parameter.getName();
            i++;
        }
        Class<?> returnType = method.getReturnType();
        returnDt = getDataType(returnType, context, module);

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

    private static void registerConstructors(Class<?> clazz, EvalContext context, String module, DataType dt) {
        Constructor<?>[] constructors = clazz.getConstructors();
        for (Constructor<?> constructor : constructors) {
            int modifiers = constructor.getModifiers();
            if (Modifier.isPublic(modifiers)) {
                Parameter[] parameters = constructor.getParameters();
                ParameterDef[] parameterDefs = getParameterDefs(parameters, context, module);

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
     * Map a Java class to a JCP DataType, registering unknown types as opaque stubs.
     *
     * <p>Primitives and well-known types are mapped directly to {@link SystemDataType}.
     * Unknown class types (e.g. {@code CharSequence}, {@code IntStream}) are registered
     * as opaque stubs — a {@link DataTypeImpl}-backed entry with no functions — so that
     * Java-level assignability checks resolve without cascading into the full JVM stdlib.
     * To expose a type's own methods, call {@link #registerClass} explicitly.
     */
    private static DataType getDataType(Class<?> dataType, EvalContext context, String module) {
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

        // Unknown type — register an opaque ExternalClassType stub (no functions) so the type
        // name is in the context and assignability checks resolve, without cascading into the
        // JVM stdlib. NamespacedTypeTable keys by FQN so same-simple-name/different-package
        // classes coexist correctly.
        String simpleName = dataType.getSimpleName();
        if (simpleName.isEmpty()) {
            return SystemDataType.ANY;
        }
        // Look up by FQN first — avoids triggering ambiguity when a same-simple-name class
        // was already registered, and is idempotent for the exact-same class.
        DataType byFqn = context.getDataTypeByFqn(dataType.getName());
        if (byFqn != null) {
            return byFqn;
        }
        // Check if a non-ExternalClassType (struct/system) is registered under this simple name.
        // If so, return it — JCP-source types take precedence and no stub is needed.
        try {
            DataType rdt = DataTypeUtils.getDataType(simpleName, context);
            if (rdt != null && !(rdt instanceof ExternalClassType)) {
                return rdt;
            }
        } catch (com.elminster.jcp.eval.excpetion.EvaluationException ignored) {
            // Simple name already ambiguous — fall through to register this class under its own FQN.
        }
        ExternalClassType stub = new ExternalClassType(simpleName, dataType);
        context.addDataType(stub);
        return stub;
    }
}
