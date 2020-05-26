package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.Identifier;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataFactory;
import com.elminster.jcp.eval.ast.excpetion.AlreadyDeclaredException;
import com.elminster.jcp.eval.ast.excpetion.CannotCastException;
import com.elminster.jcp.ast.statement.VariableDeclaration;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import com.elminster.jcp.util.DataTypeUtil;

public class VariableDeclarationEvaluator extends AbstractAstEvaluator {
  public VariableDeclarationEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) throws Exception {
    VariableDeclaration variableDeclaration = (VariableDeclaration) astNode;
    Identifier id = variableDeclaration.getId();
    Expression initExpress = variableDeclaration.getInit();
    Data variable = evalContext.getVariable(id.getId());
    if (null != variable) {
      AlreadyDeclaredException.throwAlreadyDeclaredVariableException(id.getId());
    }
    variable = DataFactory.INSTANCE.createSystemDataVariable(id.getId(),
        DataTypeUtil.getDataType(variableDeclaration.getDataType(), evalContext));
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
