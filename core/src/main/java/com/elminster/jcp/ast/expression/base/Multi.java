package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.data.IntegerFlowData;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.ArithmeticOperator;

public class Multi extends ArithmeticExpression {

  public Multi(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, ArithmeticOperator.MULTIPLY);
  }
}
