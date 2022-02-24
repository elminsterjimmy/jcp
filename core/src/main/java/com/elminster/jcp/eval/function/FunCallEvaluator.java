package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.excpetion.UndeclaredException;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.statement.Function;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import com.elminster.jcp.util.FunctionUtils;

import java.util.Arrays;

public class FunCallEvaluator extends AbstractAstEvaluator {

  public FunCallEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    FunctionCallExpression functionCallExpression = (FunctionCallExpression) astNode;

    Function function = getFunction(functionCallExpression, evalContext);
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(function);
    Data data = evaluable.eval(evalContext);
    return data;
  }

  private Function getFunction(FunctionCallExpression functionCallExpression, EvalContext evalContext) {
    Identifier id = functionCallExpression.getId();
    Expression[] arguments = functionCallExpression.getArguments();
    Data[] parameters = new Data[arguments.length];
    int i = 0;
    for (Expression arg : arguments) {
      Evaluable evaluable = AstEvaluatorFactory.getEvaluator(arg);
      parameters[i++] = evaluable.eval(evalContext);
    }

    String functionFullName = FunctionUtils.generateFunctionFullName(id, parameters);
    // FIXME: need to check castable
    Function function = evalContext.getFunction(functionFullName);
    if (null == function) {
      DataType[] dataTypes = Arrays.stream(parameters).map(
              parameter -> parameter.getDataType()
      ).toArray(DataType[]::new);
      UndeclaredException.throwFunctionUndeclaredException(id, dataTypes);
    }
    function.setArguments(parameters);
    return function;
  }
}