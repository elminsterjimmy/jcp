package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.operator.UnaryOperator;

public interface UnaryExpression extends Expression {
  Expression getExpress();
  UnaryOperator getOperator();
}
