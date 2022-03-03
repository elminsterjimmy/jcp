package com.elminster.jcp.eval.declare;

import com.elminster.common.util.CollectionUtil;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.statement.function.AbstractFunction;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclaration;
import com.elminster.jcp.eval.base.BlockEvaluator;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;

public class FunctionDeclarationEvaluator extends BlockEvaluator {

  public FunctionDeclarationEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    FunctionDeclaration functionDeclaration = (FunctionDeclaration) astNode;
    Function function = new AbstractFunction(functionDeclaration.getId(),
        functionDeclaration.getParameterDefines(),
        functionDeclaration.getDataType(),
        functionDeclaration.getBody().toArray(new Statement[functionDeclaration.getBody().size()]));
    evalContext.addFunction(function);
    return AnyData.EMPTY;
  }
}
