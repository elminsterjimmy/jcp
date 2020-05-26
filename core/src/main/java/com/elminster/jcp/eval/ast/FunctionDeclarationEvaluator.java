package com.elminster.jcp.eval.ast;

import com.elminster.common.util.CollectionUtil;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.ast.statement.AbstractFunction;
import com.elminster.jcp.ast.statement.Function;
import com.elminster.jcp.ast.statement.FunctionDeclaration;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.util.DataTypeUtil;

public class FunctionDeclarationEvaluator extends BlockEvaluator {
  public FunctionDeclarationEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    FunctionDeclaration functionDeclaration = (FunctionDeclaration) astNode;
    Function function = new AbstractFunction(functionDeclaration.getId(),
        functionDeclaration.getParameterDefines(),
        DataTypeUtil.getDataType(functionDeclaration.getDataType(), evalContext),
        (Statement[]) CollectionUtil.collection2Array(functionDeclaration.getBody()));
    evalContext.addFunction(function);
    return AnyData.EMPTY;
  }
}
