package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents struct instantiation: Point(10, 20)
 * Creates a new instance of a struct type with field values.
 */
public class StructInstantiation extends AbstractExpression {

  private final Identifier structType;
  private final List<Expression> fieldValues;

  public StructInstantiation(Identifier structType, List<Expression> fieldValues) {
    this.structType = structType;
    this.fieldValues = new ArrayList<>(fieldValues);
  }

  public StructInstantiation(String structType, List<Expression> fieldValues) {
    this(Identifier.fromName(structType), fieldValues);
  }

  public StructInstantiation(String structType, Expression... fieldValues) {
    this(Identifier.fromName(structType), Arrays.asList(fieldValues));
  }

  public Identifier getStructType() {
    return structType;
  }

  public List<Expression> getFieldValues() {
    return fieldValues;
  }

  @Override
  public String getName() {
    return "STRUCT_INSTANTIATION";
  }
}
