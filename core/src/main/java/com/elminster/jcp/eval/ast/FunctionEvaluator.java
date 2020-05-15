package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.eval.ast.excpetion.CannotCastException;
import com.elminster.jcp.ast.statement.AbstractFunction;
import com.elminster.jcp.eval.context.EvalContext;

import java.util.HashMap;
import java.util.Map;

abstract public class FunctionEvaluator extends BlockEvaluator {

  public FunctionEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    AbstractFunction function = (AbstractFunction) astNode;
    FlowData[] parameters = function.getParameters();
    DataType resultDataType = function.getResultDataType();
    Map<String, FlowData> variables = evalContext.getVariables();
    try {
      evalContext.setVariables(new HashMap<>(parameters.length));
      // TODO: init function variables.
      FlowData result = doFunc(evalContext);
      if (!resultDataType.isCastableTo(result.getDataType())) {
        throw new CannotCastException(result.getDataType(), resultDataType);
      }
      return result;
    } finally {
      evalContext.setVariables(variables);
    }
  }

  protected FlowData doFunc(EvalContext evalContext) {
    return super.eval(evalContext);
  }
}
