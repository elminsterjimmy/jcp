package com.elminster.jcp.eval.struct;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.FieldAssignmentExpression;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.StructData;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

/**
 * Evaluator for field assignment: obj.field = value
 * Writes a value to a field of a struct instance.
 */
public class FieldAssignmentEvaluator extends AbstractAstEvaluator {

  public FieldAssignmentEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    FieldAssignmentExpression fieldAssign = (FieldAssignmentExpression) astNode;
    Expression objectExpr = fieldAssign.getObject();
    String fieldName = fieldAssign.getFieldName().getId();
    Expression valueExpr = fieldAssign.getValue();

    // Evaluate the object expression
    Data objectData = AstEvaluatorFactory.getEvaluator(objectExpr).eval(evalContext);

    // Ensure it's a struct
    if (!(objectData instanceof StructData)) {
      throw new IllegalArgumentException(
          "Field assignment requires a struct instance, got: " + objectData.getDataType().getName());
    }

    StructData structData = (StructData) objectData;

    // Check that the field exists
    StructFieldDef fieldDef = structData.getStructType().getField(fieldName);
    if (fieldDef == null) {
      throw new IllegalArgumentException(
          "Struct " + structData.getStructType().getName() + " has no field: " + fieldName);
    }

    // Evaluate the value expression
    Data value = AstEvaluatorFactory.getEvaluator(valueExpr).eval(evalContext);

    // Type check
    if (!value.getDataType().isCastableTo(fieldDef.getDataType())) {
      throw new CannotCastException(value.getDataType(), fieldDef.getDataType());
    }

    // Set the field value
    structData.setField(fieldName, value);

    return value;
  }
}
