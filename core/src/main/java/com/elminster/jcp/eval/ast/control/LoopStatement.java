package com.elminster.jcp.eval.ast.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.statement.Block;

/**
 * The loop block.
 *
 * @author jgu
 * @version 1.0
 */
abstract public class LoopStatement extends ControlStatement {

  protected Expression conditionExpression;
  protected Block body;

  public LoopStatement(Expression conditionExpression, Block body) {
    this.conditionExpression = conditionExpression;
    this.body = body;
  }

  /**
   * Gets conditionExpression.
   *
   * @return value of conditionExpression
   */
  public Expression getConditionExpression() {
    return conditionExpression;
  }

  /**
   * Gets body.
   *
   * @return value of body
   */
  public Block getBody() {
    return body;
  }
}
