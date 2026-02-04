package com.elminster.jcp.eval.struct;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.StructInstantiation;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.StructData;
import com.elminster.jcp.eval.data.StructType;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluator for struct instantiation.
 * Creates a new struct instance with provided field values.
 */
public class StructInstantiationEvaluator extends AbstractAstEvaluator {

  public StructInstantiationEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    StructInstantiation structInst = (StructInstantiation) astNode;
    String structTypeName = structInst.getStructType().getId();

    // Look up the struct type
    StructType structType = (StructType) evalContext.getDataType(structTypeName);
    if (structType == null) {
      throw new IllegalArgumentException("Unknown struct type: " + structTypeName);
    }

    // Validate that we have the right number of field values
    List<StructFieldDef> fields = structType.getFields();
    List<Expression> fieldValueExprs = structInst.getFieldValues();
    if (fields.size() != fieldValueExprs.size()) {
      throw new IllegalArgumentException(
          String.format("Struct %s expects %d fields, but got %d",
              structTypeName, fields.size(), fieldValueExprs.size()));
    }

    // Evaluate field values and type-check them
    Map<String, Data> fieldValues = new HashMap<>();
    for (int i = 0; i < fields.size(); i++) {
      StructFieldDef fieldDef = fields.get(i);
      Expression valueExpr = fieldValueExprs.get(i);

      Data value = AstEvaluatorFactory.getEvaluator(valueExpr).eval(evalContext);

      // Type check
      if (!value.getDataType().isCastableTo(fieldDef.getDataType())) {
        throw new CannotCastException(value.getDataType(), fieldDef.getDataType());
      }

      fieldValues.put(fieldDef.getName().getId(), value);
    }

    // Create and return the struct instance
    StructData structData = new StructData(structType);
    for (Map.Entry<String, Data> entry : fieldValues.entrySet()) {
      structData.setField(entry.getKey(), entry.getValue());
    }
    return structData;
  }
}
