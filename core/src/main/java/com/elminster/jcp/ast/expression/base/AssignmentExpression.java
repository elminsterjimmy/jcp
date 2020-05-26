package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.Identifier;
import com.elminster.jcp.ast.expression.operator.AssignmentOperator;

public class AssignmentExpression extends AbstractExpression {

  private Identifier id;
  private AssignmentOperator operation;
  private Expression expression;

  public AssignmentExpression(Identifier id, AssignmentOperator operation, Expression expression) {
    this.id = id;
    this.operation = operation;
    this.expression = expression;
  }

  @Override
  public String getName() {
    return "ASSIGNMENT";
  }

  /**
   * Gets id.
   *
   * @return value of id
   */
  public Identifier getId() {
    return id;
  }

  /**
   * Gets operation.
   *
   * @return value of operation
   */
  public AssignmentOperator getOperation() {
    return operation;
  }

  /**
   * Gets expression.
   *
   * @return value of expression
   */
  public Expression getExpression() {
    return expression;
  }
}
