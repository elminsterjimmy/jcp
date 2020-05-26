package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.Identifier;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataFactory;
import com.elminster.jcp.eval.data.DataType;

public class IdentifierEvaluator extends AbstractAstEvaluator {

  public IdentifierEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) throws Exception {
    Identifier identifier = (Identifier) astNode;
    return DataFactory.INSTANCE.createSystemDataConst(identifier.getId(), DataType.SystemDataType.STRING,
        identifier.getId());
  }
}
