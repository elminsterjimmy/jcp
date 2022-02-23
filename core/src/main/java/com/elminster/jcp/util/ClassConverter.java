package com.elminster.jcp.util;

import com.elminster.common.util.ReflectUtil;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.base.IdentifierExpression;
import com.elminster.jcp.ast.statement.Function;
import com.elminster.jcp.ast.statement.ParameterDef;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.module.AbstractModuleFunction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

public class ClassConverter {

  public static DataType registerClass(Class<?> clazz, EvalContext context, String module) {
    String name = clazz.getSimpleName();
    DataType dt = DataTypeUtil.getDataType(name, context);
    context.addDataType(dt);
    Method[] methods = clazz.getDeclaredMethods();
    for (Method method : methods) {
      if (Modifier.isPublic(method.getModifiers())) {
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
            return dt.getName() + "." + methodName;
          }

          @Override
          public Identifier getId() {
            return new IdentifierExpression(getName());
          }

          @Override
          public ParameterDef[] getParameterDefs() {
            ParameterDef[] dataDefs = new ParameterDef[paramDts.length];
            int i = 0;
            for (DataType dt : paramDts) {
              dataDefs[i++] = new ParameterDef(paramNames[i], paramDts[i].getName());
            }
            return dataDefs;
          }

          @Override
          public DataType getResultDataType() {
            return returnDt;
          }

          @Override
          protected Data doFunction(Data[] parameters) {
            Object target = parameters[0];
            Object[] args = new Object[parameters.length - 1];
            for (int i = 1; i < parameters.length; i++) {
              args[i - 1] = parameters[i].get();
            }
            Object result = null;
            try {
              result = ReflectUtil.invoke(target, methodName, args);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
              throw new RuntimeException(e);
            }
            return new AnyData(result) {
              @Override
              public DataType getDataType() {
                return returnDt;
              }
            };
          }
        };
        context.addFunction(function);
      }
    }

    // constructor


    context.addFunction(new AbstractModuleFunction() {

      @Override
      public String getModule() {
        return module;
      }

      @Override
      public String getName() {
        return dt.getName() + ".new";
      }

      @Override
      public Identifier getId() {
        return new IdentifierExpression(getName());
      }

      @Override
      public ParameterDef[] getParameterDefs() {
        return new ParameterDef[0];
      }

      @Override
      public DataType getResultDataType() {
        return dt;
      }

      @Override
      protected Data doFunction(Data[] parameters) {
        try {
          return new AnyData(clazz.getDeclaredConstructor().newInstance()) {
            @Override
            public DataType getDataType() {
              return dt;
            }
          };
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
    return dt;
  }

  private static DataType getDataType(Class<?> dataType, EvalContext context, String module) {
    DataType rdt = DataTypeUtil.getDataType(dataType.getSimpleName(), context);
    if (null == rdt) {
      rdt = registerClass(dataType, context, module);
    }
    return rdt;
  }
}
