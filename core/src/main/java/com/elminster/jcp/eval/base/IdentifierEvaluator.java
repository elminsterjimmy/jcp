package com.elminster.jcp.eval.base;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataFactory;
import com.elminster.jcp.eval.data.DataType;

public class IdentifierEvaluator extends AbstractAstEvaluator {

  public IdentifierEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    Identifier identifier = (Identifier) astNode;
    return DataFactory.INSTANCE.createSystemDataConst(identifier, DataType.SystemDataType.STRING,
        identifier.getId());
  }
}
