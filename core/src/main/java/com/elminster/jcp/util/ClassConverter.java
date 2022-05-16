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
import com.elminster.jcp.eval.excpetion.InitializeException;
import com.elminster.jcp.module.AbstractModuleFunction;
import com.google.common.reflect.ClassPath;

import java.lang.reflect.*;

public class ClassConverter {

    public static void registerClass(Class<?> clazz, EvalContext context, String module) {
        String name = clazz.getSimpleName();
        DataType dt = DataTypeUtils.getDataTypeAndCreateOnMissing(name, context);
        context.addDataType(dt);
        Method[] methods = clazz.getDeclaredMethods();
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
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throwInvokeException(e);
                }
                return new AnyData(result, returnDataType);
            }
        };
        context.addFunction(function);
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
                    result = ReflectUtil.invoke(target, methodName, args);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    throwInvokeException(e);
                }
                return new AnyData(result, returnDt);
            }
        };
        context.addFunction(function);
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
                        Object[] argValues = getArgumentValues(arguments);
                        try {
                            return new AnyData(constructor.newInstance(argValues), dt);
                        } catch (Exception e) {
                            throw new InitializeException(e);
                        }
                    }
                };
                context.addFunction(constructorFunc);
            }
        }
    }

    private static DataType getDataType(Class<?> dataType, EvalContext context, String module) {
        DataType rdt = DataTypeUtils.getDataType(dataType.getSimpleName(), context);
        if (null == rdt) {
            registerClass(dataType, context, module);
        }
        return rdt;
    }
}
