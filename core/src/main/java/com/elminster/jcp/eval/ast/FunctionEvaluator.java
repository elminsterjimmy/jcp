package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.Data;
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
  public Data eval(EvalContext evalContext) throws Exception {
    AbstractFunction function = (AbstractFunction) astNode;
    Data[] parameters = function.getParameters();
    DataType resultDataType = function.getResultDataType();
    Map<String, Data> variables = evalContext.getVariables();
    try {
      evalContext.setVariables(new HashMap<>(parameters.length));
      // TODO: init function variables.
      Data result = doFunc(evalContext);
      if (!resultDataType.isCastableTo(result.getDataType())) {
        throw new CannotCastException(result.getDataType(), resultDataType);
      }
      return result;
    } finally {
      evalContext.setVariables(variables);
    }
  }

  protected Data doFunc(EvalContext evalContext) throws Exception {
    return super.eval(evalContext);
  }
}
