package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;

/**
 * Represents field assignment: obj.field = value
 * Writes a value to a field of a struct instance.
 */
public class FieldAssignmentExpression extends AbstractExpression {

  private final Expression object;
  private final Identifier fieldName;
  private final Expression value;

  public FieldAssignmentExpression(Expression object, Identifier fieldName, Expression value) {
    this.object = object;
    this.fieldName = fieldName;
    this.value = value;
  }

  public FieldAssignmentExpression(Expression object, String fieldName, Expression value) {
    this(object, Identifier.fromName(fieldName), value);
  }

  public Expression getObject() {
    return object;
  }

  public Identifier getFieldName() {
    return fieldName;
  }

  public Expression getValue() {
    return value;
  }

  @Override
  public String getName() {
    return "FIELD_ASSIGNMENT";
  }
}
