package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.BinaryOperator;

public interface BinaryExpression extends Expression {

  Expression getLeft();
  Expression getRight();
  BinaryOperator getOperator();
}
