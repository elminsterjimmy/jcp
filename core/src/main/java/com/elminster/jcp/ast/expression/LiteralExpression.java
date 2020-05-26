package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.eval.data.Data;

public class LiteralExpression<T> extends AbstractExpression implements Literal {

  private T constData;

  public LiteralExpression(T constData) {
    this.constData = constData;
  }

  @Override
  public String getName() {
    return "LITERAL";
  }

  /**
   * Gets constData.
   *
   * @return value of constData
   */
  public T getValue() {
    return constData;
  }
}
