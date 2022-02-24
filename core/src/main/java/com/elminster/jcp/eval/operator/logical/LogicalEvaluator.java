package com.elminster.jcp.eval.operator.logical;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.data.BooleanData;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

abstract public class LogicalEvaluator extends AbstractAstEvaluator {

  public LogicalEvaluator(Node astNode) {
    super(astNode);
  }

  protected boolean evalBoolean(Expression expr, EvalContext evalContext) {
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(expr);
    Data data = evaluable.eval(evalContext);
    if (!(data instanceof BooleanData)) {
      throw new CannotCastException(data.getDataType(), DataType.SystemDataType.BOOLEAN);
    }
    Boolean value = ((BooleanData)data).get();
    if (null == value) {
      throw new NullPointerException();
    }
    return value.booleanValue();
  }
}
