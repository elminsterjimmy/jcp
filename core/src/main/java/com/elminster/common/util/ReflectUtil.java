package com.elminster.common.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtil {
  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... parameterTypes) {
    try {
      return clazz.getConstructor(parameterTypes);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Constructor not found: " + clazz.getName(), e);
    }
  }

  public static Object invoke(Object target, String methodName, Object... args) {
    try {
      Class<?>[] parameterTypes = new Class<?>[args.length];
      for (int i = 0; i < args.length; i++) {
        parameterTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
      }

      Method method = findMethod(target != null ? target.getClass() : null, methodName, parameterTypes);
      if (method == null) {
        throw new NoSuchMethodException("Method not found: " + methodName);
      }

      method.setAccessible(true);
      return method.invoke(target, args);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException("Failed to invoke method: " + methodName, e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw new RuntimeException("Failed to invoke method: " + methodName, cause);
    }
  }

  public static Object invoke(Object target, Method method, Object... args) {
    try {
      method.setAccessible(true);
      return method.invoke(target, args);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to invoke method: " + method.getName(), e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw new RuntimeException("Failed to invoke method: " + method.getName(), cause);
    }
  }

  private static Method findMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
    if (clazz == null) {
      return null;
    }

    try {
      return clazz.getMethod(methodName, parameterTypes);
    } catch (NoSuchMethodException e) {
      // Try to find method with compatible parameter types
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals(methodName) &&
            method.getParameterCount() == parameterTypes.length &&
            isCompatible(method.getParameterTypes(), parameterTypes)) {
          return method;
        }
      }
      return null;
    }
  }

  private static boolean isCompatible(Class<?>[] methodParams, Class<?>[] argTypes) {
    for (int i = 0; i < methodParams.length; i++) {
      if (!isAssignable(methodParams[i], argTypes[i])) {
        return false;
      }
    }
    return true;
  }

  private static boolean isAssignable(Class<?> targetType, Class<?> argType) {
    if (targetType.isAssignableFrom(argType)) {
      return true;
    }
    // Handle primitive types
    if (targetType.isPrimitive()) {
      if (targetType == int.class && (argType == Integer.class)) return true;
      if (targetType == long.class && (argType == Long.class)) return true;
      if (targetType == double.class && (argType == Double.class)) return true;
      if (targetType == float.class && (argType == Float.class)) return true;
      if (targetType == boolean.class && (argType == Boolean.class)) return true;
      if (targetType == char.class && (argType == Character.class)) return true;
      if (targetType == byte.class && (argType == Byte.class)) return true;
      if (targetType == short.class && (argType == Short.class)) return true;
    }
    return false;
  }
}
