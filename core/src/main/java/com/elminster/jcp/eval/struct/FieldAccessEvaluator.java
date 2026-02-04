package com.elminster.jcp.eval.struct;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.FieldAccessExpression;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.StructData;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

/**
 * Evaluator for field access: obj.field
 * Reads and returns the value of a field from a struct instance.
 */
public class FieldAccessEvaluator extends AbstractAstEvaluator {

  public FieldAccessEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    FieldAccessExpression fieldAccess = (FieldAccessExpression) astNode;
    Expression objectExpr = fieldAccess.getObject();
    String fieldName = fieldAccess.getFieldName().getId();

    // Evaluate the object expression
    Data objectData = AstEvaluatorFactory.getEvaluator(objectExpr).eval(evalContext);

    // Ensure it's a struct
    if (!(objectData instanceof StructData)) {
      throw new IllegalArgumentException(
          "Field access requires a struct instance, got: " + objectData.getDataType().getName());
    }

    StructData structData = (StructData) objectData;

    // Get the field value
    Data fieldValue = structData.getField(fieldName);
    if (fieldValue == null) {
      throw new IllegalArgumentException(
          "Struct " + structData.getStructType().getName() + " has no field: " + fieldName);
    }

    return fieldValue;
  }
}
