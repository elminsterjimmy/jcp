package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.collection.FastStack;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.base.BlockEvaluator;
import com.elminster.jcp.eval.context.DefaultEvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import com.elminster.jcp.ast.statement.function.AbstractFunction;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.excpetion.FunctionAmbiguityException;
import com.elminster.jcp.eval.excpetion.FunctionArgumentsLengthException;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This is an internal call for a function.
 *
 * @author jgu
 * @version 1.0
 */
public class FunctionEvaluator extends BlockEvaluator {

  private static final Logger logger = LoggerFactory.getLogger(FunctionEvaluator.class);

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
    if (logger.isDebugEnabled()) {
      logger.debug("calling function [{}]", function);
    }
    Data[] arguments = function.getArguments();
    ParameterDef[] parameterDefs = function.getParameterDefs();
    if (parameterDefs.length != arguments.length) {
      throw new FunctionArgumentsLengthException();
    }
    for (int i = 0, len = parameterDefs.length; i< len; i++) {
      evalContext.addVariable(cloneArgument(arguments[i], parameterDefs[i]));
    }
    List<Statement> body = function.getBody();
    Data rtn = null;
    for (Statement statement : body) {
      Evaluable evaluator = AstEvaluatorFactory.getEvaluator(statement);
      rtn = evaluator.eval(evalContext);
      if (evalContext.isReturn()) {
        break;
      }
    }
    return rtn;
  }

  private Data cloneArgument(Data argument, ParameterDef parameterDef) {
    return new AnyData(Identifier.fromName(parameterDef.getId()),
            parameterDef.getDataType(),
            argument.get(),
            false);
  }
}
