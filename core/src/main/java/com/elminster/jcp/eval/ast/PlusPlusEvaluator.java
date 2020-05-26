package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.ast.expression.UnaryExpression;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

public class PlusPlusEvaluator extends UnaryEvaluator {
  public PlusPlusEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) throws Exception {
    UnaryExpression unaryExpression = (UnaryExpression) astNode;
    Expression expression = unaryExpression.getExpress();
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(expression);
    Data data = evaluable.eval(evalContext);
    DataType dt = data.getDataType();
    if (dt instanceof DataType.SystemDataType) {
      switch ((DataType.SystemDataType)dt) {
        case INT:
          Integer i = (Integer) data.get();
          data.set(++i);
          break;
        default:
          throw new UnsupportedOperationException();
      }
    } else {
      throw new UnsupportedOperationException();
    }
    return data;
  }
}
