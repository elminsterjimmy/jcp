package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.excpetion.FunctionAmbiguityException;
import com.elminster.jcp.eval.excpetion.UndeclaredException;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import com.elminster.jcp.module.Modulable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The function call evaluator.
 *
 * @author jgu
 * @version 1.0
 */
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
    Data[] argumentData = new Data[arguments.length];
    int i = 0;
    for (Expression arg : arguments) {
      Evaluable evaluable = AstEvaluatorFactory.getEvaluator(arg);
      argumentData[i++] = evaluable.eval(evalContext);
    }

    String functionName = functionCallExpression.getId().getId();
    String moduleName = Modulable.DEFAULT_MODULE;
    if (functionCallExpression instanceof Modulable) {
      moduleName = ((Modulable) functionCallExpression).getModule();
    }

    List<Function> functionCandidates = getFunctionCandidates(functionName,
            moduleName,
            argumentData, evalContext);
    int functionCandidateSize = functionCandidates.size();
    if (0 == functionCandidateSize) {
      DataType[] dataTypes = Arrays.stream(argumentData).map(
              parameter -> parameter.getDataType()
      ).toArray(DataType[]::new);
      UndeclaredException.throwFunctionUndeclaredException(id, dataTypes);
    } else if (functionCandidateSize > 1) {
      DataType[] dataTypes = Arrays.stream(argumentData).map(
              parameter -> parameter.getDataType()
      ).toArray(DataType[]::new);
      throw new FunctionAmbiguityException(id, dataTypes);
    }

    Function function = functionCandidates.get(0);
    function.setArguments(argumentData);
    return function;
  }

  private List<Function> getFunctionCandidates(final String functionName,
                                               final String moduleName,
                                               final Data[] arguments,
                                               EvalContext evalContext) {
    return evalContext.getFunctions()
            .values().stream()
            .filter(function -> hasSameFunctionName(functionName, function))
            .filter(function -> hasSameModule(moduleName, function))
            .filter(function -> hasSameParameterDefinition(arguments, function))
            .collect(Collectors.toList());

  }

  private boolean hasSameFunctionName(String functionName2Test, Function function) {
    return functionName2Test.equals(function.getId().getId());
  }

  private boolean hasSameParameterDefinition(Data[] arguments, Function function) {
    ParameterDef[] parameterDefs = function.getParameterDefs();
    if (null == arguments) {
      return null == parameterDefs;
    }
    if (parameterDefs.length == arguments.length) {
      for (int i = 0; i < parameterDefs.length; i++) {
        if (!arguments[i].getDataType().isCastableTo(parameterDefs[i].getDataType())) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private boolean hasSameModule(String moduleName2Test, Function function) {
    if (Modulable.DEFAULT_MODULE.equals(moduleName2Test)) {
      return true;
    }
    if (function instanceof Modulable) {
      return ((Modulable) function).getModule().equals(moduleName2Test);
    }
    return false;
  }
}