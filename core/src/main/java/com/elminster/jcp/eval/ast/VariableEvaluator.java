package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.eval.ast.excpetion.UndeclaredException;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.eval.context.EvalContext;

public class VariableEvaluator extends AbstractAstEvaluator {

  public VariableEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    VariableExpression variableExpression = (VariableExpression) astNode;
    String id = variableExpression.getId();
    FlowData variable = evalContext.getVariable(id);
    if (null == variable) {
      UndeclaredException.throwUndeclaredVariableException(id);
    }
    return variable;
  }
}
