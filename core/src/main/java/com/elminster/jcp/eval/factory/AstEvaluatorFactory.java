package com.elminster.jcp.eval.factory;

import com.elminster.common.util.ReflectUtil;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.Evaluable;
import com.google.common.reflect.ClassPath;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

abstract public class AstEvaluatorFactory {

  private static final String AST_EVAL_BASE_PACKAGE = "com.elminster.jcp.eval";
  private static final Map<String, ? extends Class<?>> SYSTEM_EVALUATOR;

  static {
    try {
      SYSTEM_EVALUATOR = ClassPath.from(ClassLoader.getSystemClassLoader())
              .getAllClasses()
              .stream()
              .filter(clazz -> clazz.getPackageName().startsWith(AST_EVAL_BASE_PACKAGE) && clazz.getSimpleName().endsWith("Evaluator"))
              .map(clazz -> clazz.load())
              .collect(Collectors.toMap(Class::getSimpleName, Function.identity()));
    } catch (IOException e) {
      throw new RuntimeException("failed to load the system evaluators", e);
    }
  }

  public static Evaluable getEvaluator(Node astNode) {
    if (astNode instanceof Evaluable) {
      return (Evaluable) astNode;
    }
    String name = astNode.getName();
    try {
      Class clazz = SYSTEM_EVALUATOR.get(getEvaluatorClassName(name));
      Constructor<Evaluable> constructor = ReflectUtil.getConstructor(clazz, Node.class);
      return constructor.newInstance(astNode);
    } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
      throw new RuntimeException(String.format("cannot get evaluator for node: %s", astNode), e);
    }
  }

  private static String getEvaluatorClassName(String name) {
    return normalize(name) + "Evaluator";
  }

  private static String normalize(String name) {
    int len = name.length();
    StringBuilder sb = new StringBuilder(len);
    boolean upper = true;
    for (int i = 0; i < len; i++) {
      char ch = name.charAt(i);
      if ('_' == ch) {
        upper = true;
      } else {
        if (upper) {
          sb.append(Character.toUpperCase(ch));
          upper = false;
        } else {
          sb.append(Character.toLowerCase(ch));
        }
      }
    }
    return sb.toString();
  }
}
