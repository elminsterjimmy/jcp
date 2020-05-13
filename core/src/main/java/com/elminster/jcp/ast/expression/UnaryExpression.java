package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.expression.operator.UnaryOperator;

public interface UnaryExpression extends Expression {
  Expression getExpress();
  UnaryOperator getOperator();
}
