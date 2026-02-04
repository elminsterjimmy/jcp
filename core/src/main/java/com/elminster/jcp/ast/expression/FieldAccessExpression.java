package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;

/**
 * Represents field access: obj.field
 * Reads the value of a field from a struct instance.
 */
public class FieldAccessExpression extends AbstractExpression {

  private final Expression object;
  private final Identifier fieldName;

  public FieldAccessExpression(Expression object, Identifier fieldName) {
    this.object = object;
    this.fieldName = fieldName;
  }

  public FieldAccessExpression(Expression object, String fieldName) {
    this(object, Identifier.fromName(fieldName));
  }

  public Expression getObject() {
    return object;
  }

  public Identifier getFieldName() {
    return fieldName;
  }

  @Override
  public String getName() {
    return "FIELD_ACCESS";
  }
}
