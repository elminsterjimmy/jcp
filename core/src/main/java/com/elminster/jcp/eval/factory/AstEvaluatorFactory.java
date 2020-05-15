package com.elminster.jcp.eval.factory;

import com.elminster.common.util.ReflectUtil;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.Evaluable;

import javax.xml.stream.events.Characters;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

abstract public class AstEvaluatorFactory {

  private static final String AST_EVAL_BASE_PACKAGE = "com.elminster.jcp.eval.ast";

  public static Evaluable getEvaluator(Node astNode) {
    if (astNode instanceof Evaluable) {
      return (Evaluable) astNode;
    }
    String name = astNode.getName();
    try {
      Class clazz = Class.forName(getEvaluatorClassName(name));
      Constructor<Evaluable> constructor = ReflectUtil.getConstructor(clazz, Node.class);
      return constructor.newInstance(astNode);
    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getEvaluatorClassName(String name) {
    return AST_EVAL_BASE_PACKAGE + "." + normalize(name) + "Evaluator";
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
