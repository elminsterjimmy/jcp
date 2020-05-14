package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.excpetion.UndeclaredException;
import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.AssignmentOperator;
import com.elminster.jcp.eval.context.EvalContext;

public class AssignmentExpression extends AbstractExpression {

  private String id;
  private AssignmentOperator operation;
  private Expression expression;

  public AssignmentExpression(String id, AssignmentOperator operation, Expression expression) {
    this.id = id;
    this.operation = operation;
    this.expression = expression;
  }

  @Override
  public String getName() {
    return "ASSIGNMENT";
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    FlowData data = evalContext.getVariable(id);
    if (null == data) {
      UndeclaredException.throwUndeclaredVariableException(id);
    }
    FlowData result = expression.eval(evalContext);
    doOperation(data, result);
    data.set(result.get());
    return AnyFlowData.EMPTY;
  }

  private void doOperation(FlowData data, FlowData result) {
    switch (operation) {
      case ASSIGNMENT:
        data.set(result);
        break;
      default:
        throw new UnsupportedOperationException(operation.getName());
    }
  }
}
