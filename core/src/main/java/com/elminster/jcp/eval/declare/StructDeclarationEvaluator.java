package com.elminster.jcp.eval.declare;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.declaration.StructDeclaration;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.StructType;

/**
 * Evaluator for struct declarations.
 * Registers a new struct type in the evaluation context.
 */
public class StructDeclarationEvaluator extends AbstractAstEvaluator {

  public StructDeclarationEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    StructDeclaration structDecl = (StructDeclaration) astNode;
    String structName = structDecl.getId().getId();

    // Create and register the struct type
    StructType structType = new StructType(structName, structDecl.getFields());
    evalContext.addDataType(structType);

    return AnyData.EMPTY;
  }
}
