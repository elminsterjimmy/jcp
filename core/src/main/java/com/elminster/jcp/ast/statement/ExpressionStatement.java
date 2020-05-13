package com.elminster.jcp.ast.statement;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.eval.context.EvalContext;

public class ExpressionStatement extends AbstractStatement implements Statement {

  private final Expression expression;

  public ExpressionStatement(Expression expression) {
    Assert.notNull(expression);
    this.expression = expression;
  }

  @Override
  public String getName() {
    return "EXPRESSION_STATEMENT";
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    return this.expression.eval(evalContext);
  }
}
