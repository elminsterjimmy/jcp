package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.BooleanFlowData;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.eval.ast.excpetion.CannotCastException;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

abstract public class LogicalEvaluator extends AbstractAstEvaluator {

  public LogicalEvaluator(Node astNode) {
    super(astNode);
  }

  protected boolean evalBoolean(Expression expr, EvalContext evalContext) {
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(expr);
    FlowData data = evaluable.eval(evalContext);
    if (!(data instanceof BooleanFlowData)) {
      throw new CannotCastException(data.getDataType(), DataType.BOOLEAN);
    }
    Boolean value = ((BooleanFlowData)data).get();
    if (null == value) {
      throw new NullPointerException();
    }
    return value.booleanValue();
  }
}
