package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.collection.FastStack;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.base.BlockEvaluator;
import com.elminster.jcp.eval.context.DefaultEvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import com.elminster.jcp.ast.statement.function.AbstractFunction;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

import java.util.List;

public class FunctionEvaluator extends BlockEvaluator {

  public FunctionEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    AbstractFunction function = (AbstractFunction) astNode;
    DataType resultDataType = function.getResultDataType();
    FastStack funcStack = evalContext.getContextStack();
    DefaultEvalContext defaultEvalContext = new DefaultEvalContext();
    funcStack.push(defaultEvalContext);
    try {
      Data result = doFunc(function, evalContext);
      if (!resultDataType.isCastableTo(result.getDataType())) {
        throw new CannotCastException(result.getDataType(), resultDataType);
      }
      return result;
    } finally {
      funcStack.pop();
    }
  }

  protected Data doFunc(AbstractFunction function, EvalContext evalContext) {
    Data[] arguments = function.getArguments();
    for (Data argument : arguments) {
      evalContext.addVariable(argument);
    }
    List<Statement> body = function.getBody();
    Data rtn = null;
    for (Statement statement : body) {
      Evaluable evaluator = AstEvaluatorFactory.getEvaluator(statement);
      rtn = evaluator.eval(evalContext);
    }
    return rtn;
  }
}
