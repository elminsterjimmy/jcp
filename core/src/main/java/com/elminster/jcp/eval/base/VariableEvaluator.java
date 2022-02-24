package com.elminster.jcp.eval.base;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.excpetion.UndeclaredException;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.eval.context.EvalContext;

public class VariableEvaluator extends AbstractAstEvaluator {

  public VariableEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    VariableExpression variableExpression = (VariableExpression) astNode;
    Identifier id = variableExpression.getId();
    Data variable = evalContext.getVariable(id.getId());
    if (null == variable) {
      UndeclaredException.throwVariableUndeclaredException(id);
    }
    return variable;
  }
}
