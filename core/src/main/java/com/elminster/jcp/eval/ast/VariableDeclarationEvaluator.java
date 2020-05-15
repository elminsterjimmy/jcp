package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.data.FlowDataFactory;
import com.elminster.jcp.eval.ast.excpetion.AlreadyDeclaredException;
import com.elminster.jcp.eval.ast.excpetion.CannotCastException;
import com.elminster.jcp.ast.statement.VariableDeclaration;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

public class VariableDeclarationEvaluator extends AbstractAstEvaluator {
  public VariableDeclarationEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    VariableDeclaration variableDeclaration = (VariableDeclaration) astNode;
    String id = variableDeclaration.getId();
    Expression initExpress = variableDeclaration.getInit();
    FlowData flowData = evalContext.getVariable(id);
    if (null != flowData) {
      AlreadyDeclaredException.throwAlreadyDeclaredVariableException(id);
    }
    FlowData variable = FlowDataFactory.INSTANCE.createFlowDataVariable(id, variableDeclaration.getDataType());
    evalContext.addVariable(variable);
    if (null != initExpress) {
      FlowData data = AstEvaluatorFactory.getEvaluator(initExpress).eval(evalContext);
      if (!data.getDataType().isCastableTo(variable.getDataType())) {
        throw new CannotCastException(data.getDataType(), variable.getDataType());
      }
      variable.set(data.get());
    }
    return AnyFlowData.EMPTY;
  }
}
