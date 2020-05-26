package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.Identifier;
import com.elminster.jcp.ast.statement.ParameterDef;
import com.elminster.jcp.eval.ast.excpetion.CannotCastException;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.ast.excpetion.UndeclaredException;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.statement.Function;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import com.elminster.jcp.util.DataTypeUtil;

public class FunCallEvaluator extends AbstractAstEvaluator {

  public FunCallEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) throws Exception {
    FunctionCallExpression functionCallExpression = (FunctionCallExpression) astNode;
    Identifier id = functionCallExpression.getId();
    Expression[] arguments = functionCallExpression.getArguments();
    Function function = evalContext.getFunction(id.getId());
    if (null == function) {
      UndeclaredException.throwUndeclaredFunctionException(id);
    }
    Data[] parameters = new Data[arguments.length];
    int i = 0;
    for (Expression arg : arguments) {
      Evaluable evaluable = AstEvaluatorFactory.getEvaluator(arg);
      parameters[i++] = evaluable.eval(evalContext);
    }
    checkParameters(function, parameters, evalContext);
    function.setParameters(parameters);
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(function);
    Data data = evaluable.eval(evalContext);
    return data;
  }

  protected void checkParameters(Function function, Data[] parameters, EvalContext evalContext) {
    ParameterDef[] parameterDefs = function.getParameterDefs();
    if (parameters.length != parameterDefs.length) {
      // TODO
      UndeclaredException.throwUndeclaredFunctionException(function.getId());
    }
    int i = 0;
    for (Data parameter : parameters) {
      ParameterDef def = parameterDefs[i++];
      DataType expectDataType = DataTypeUtil.getDataType(def.getDataType(), evalContext);
      if (!parameter.getDataType().isCastableTo(expectDataType)) {
        throw new CannotCastException(parameter.getDataType(), expectDataType);
      }
    }
  }
}