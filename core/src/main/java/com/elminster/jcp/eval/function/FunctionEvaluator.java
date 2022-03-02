package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.collection.FastStack;
import com.elminster.jcp.eval.base.BlockEvaluator;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import com.elminster.jcp.ast.statement.function.AbstractFunction;
import com.elminster.jcp.eval.context.EvalContext;

abstract public class FunctionEvaluator extends BlockEvaluator {

  public FunctionEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    AbstractFunction function = (AbstractFunction) astNode;
    Data[] parameters = function.getArguments();
    DataType resultDataType = function.getResultDataType();
    // nested function support
    FastStack funcStack = evalContext.getFuncVariableStack();
    int paramCount = parameters.length;
    for (Data parameter : parameters) {
      funcStack.push(parameter);
    }
    try {
      Data result = doFunc(parameters, evalContext);
      if (!resultDataType.isCastableTo(result.getDataType())) {
        throw new CannotCastException(result.getDataType(), resultDataType);
      }
      return result;
    } finally {
      for (int i = 0; i < paramCount; i++) {
        funcStack.pop();
      }
    }
  }

  abstract protected Data doFunc(Data[] parameters, EvalContext evalContext);
}
