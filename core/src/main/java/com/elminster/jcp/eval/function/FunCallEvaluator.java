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
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DoubleData;
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

    String rawName = functionCallExpression.getId().getId();
    String moduleName = Modulable.DEFAULT_MODULE;
    String functionName;
    if (functionCallExpression instanceof Modulable) {
      moduleName = ((Modulable) functionCallExpression).getModule();
      functionName = rawName;
    } else {
      // Support "module::TypeName.method" embedded in the identifier string.
      int sep = rawName.indexOf("::");
      if (sep >= 0) {
        moduleName = rawName.substring(0, sep);
        functionName = rawName.substring(sep + 2);
      } else {
        functionName = rawName;
      }
    }

    List<Function> functionCandidates = getFunctionCandidates(functionName,
            moduleName,
            argumentData, evalContext);
    // When multiple candidates are compatible, prefer an exact-type match over a
    // widening/hierarchy match (e.g. abs(int) vs abs(double) for an INT arg), mirroring
    // the compile-mode resolver in ExternalClassType.
    if (functionCandidates.size() > 1) {
      functionCandidates = narrowToExactMatches(functionCandidates, argumentData);
    }
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
      throw new FunctionAmbiguityException(id, getSourceLocation(), dataTypes);
    }

    Function function = functionCandidates.get(0);
    // Apply widening conversions to arguments if needed
    Data[] convertedArgs = applyWideningConversions(argumentData, function.getParameterDefs());
    function.setArguments(convertedArgs);
    return function;
  }

  /**
   * Apply widening conversions to arguments where needed (e.g., int → double).
   */
  private Data[] applyWideningConversions(Data[] arguments, ParameterDef[] params) {
    if (arguments == null || params == null) {
      return arguments;
    }
    Data[] result = new Data[arguments.length];
    for (int i = 0; i < arguments.length; i++) {
      Data arg = arguments[i];
      DataType argType = arg.getDataType();
      DataType paramType = params[i].getDataType();
      // Check if widening conversion needed
      if (argType.isTypePromotableTo(paramType)) {
        if (argType == SystemDataType.INT && paramType == SystemDataType.DOUBLE) {
          // Convert IntegerData to DoubleData
          Integer intValue = (Integer) arg.get();
          result[i] = new DoubleData(intValue.doubleValue());
          continue;
        }
      }
      result[i] = arg;  // No conversion needed
    }
    return result;
  }

  /**
   * From a list of already-compatible candidates, keep only those whose parameter
   * types are an exact match for the argument types. If exactly one exact match
   * exists, return just that one; otherwise (zero or multiple exact matches) return
   * the original candidates unchanged so the caller preserves its ambiguity handling.
   */
  private List<Function> narrowToExactMatches(List<Function> candidates, Data[] arguments) {
    if (arguments == null) {
      return candidates;
    }
    DataType[] argTypes = Arrays.stream(arguments)
            .map(Data::getDataType)
            .toArray(DataType[]::new);
    List<Function> exactMatches = candidates.stream()
            .filter(function -> {
              ParameterDef[] params = function.getParameterDefs();
              DataType[] paramTypes = Arrays.stream(params)
                      .map(ParameterDef::getDataType)
                      .toArray(DataType[]::new);
              return DataType.allExactMatch(argTypes, paramTypes);
            })
            .collect(Collectors.toList());
    return exactMatches.size() == 1 ? exactMatches : candidates;
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
        DataType argType = arguments[i].getDataType();
        DataType paramType = parameterDefs[i].getDataType();
        // When both sides are ExternalClassType, use FQN-aware isCastableTo to
        // distinguish same-simple-name types (e.g. HttpRequest$Builder vs HttpClient$Builder).
        if (argType instanceof com.elminster.jcp.eval.data.ExternalClassType
                && paramType instanceof com.elminster.jcp.eval.data.ExternalClassType) {
          if (!argType.isCompatibleWith(paramType)) {
            return false;
          }
          continue;
        }
        // Opaque stubs (DataTypeImpl, not SystemDataType or StructType) represent external
        // Java types whose full interface/class hierarchy is not modeled in JCP. Accept any
        // reference (non-primitive) argument — the JVM will enforce real type safety at call time.
        if (isOpaqueStub(paramType) && isReferenceType(argType)) {
          continue;
        }
        // Check type compatibility (hierarchy + widening)
        if (!argType.isCompatibleWith(paramType)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private boolean isOpaqueStub(DataType type) {
    // ExternalClassType stubs (compile + eval mode) and legacy DataTypeImpl stubs both represent
    // external Java types whose full hierarchy is not modeled in JCP — accept any reference arg
    return !(type instanceof DataType.SystemDataType)
        && !(type instanceof com.elminster.jcp.eval.data.StructType)
        && (type instanceof com.elminster.jcp.eval.data.DataTypeImpl
            || type instanceof com.elminster.jcp.eval.data.ExternalClassType);
  }

  private boolean isReferenceType(DataType type) {
    return type != DataType.SystemDataType.INT
        && type != DataType.SystemDataType.DOUBLE
        && type != DataType.SystemDataType.BOOLEAN
        && type != DataType.SystemDataType.VOID;
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