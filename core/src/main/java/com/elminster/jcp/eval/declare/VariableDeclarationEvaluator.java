package com.elminster.jcp.eval.declare;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.declaration.VariableDeclaration;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataFactory;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import com.elminster.jcp.util.DataTypeUtils;

public class VariableDeclarationEvaluator extends AbstractAstEvaluator {

  public VariableDeclarationEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    VariableDeclaration variableDeclaration = (VariableDeclaration) astNode;
    Identifier id = variableDeclaration.getId();
    Expression initExpress = variableDeclaration.getInit();
    Data variable = DataFactory.INSTANCE.createVariable(id,
        DataTypeUtils.getDataType(variableDeclaration.getDataType().getName(), evalContext));
    evalContext.addVariable(variable);
    if (null != initExpress) {
      Data initValue = AstEvaluatorFactory.getEvaluator(initExpress).eval(evalContext);
      if (!initValue.getDataType().isCastableTo(variable.getDataType())) {
        throw new CannotCastException(initValue.getDataType(), variable.getDataType());
      }
      variable.set(initValue.get());
    }
    return AnyData.EMPTY;
  }
}
