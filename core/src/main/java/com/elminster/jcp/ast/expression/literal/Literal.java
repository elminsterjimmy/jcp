package com.elminster.jcp.ast.expression.literal;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;

public interface Literal<T> extends Node, Expression {

  T getValue();

  default String getName() {
    return "Literal";
  }

  static <T> Literal of(T value) {
    return () -> value;
  }
}
