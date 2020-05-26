package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.eval.data.Data;

public class ConstantExpression extends AbstractExpression {

  private Data constData;

  public ConstantExpression(Data constData) {
    this.constData = constData;
  }

  @Override
  public String getName() {
    return "CONSTANT";
  }

  /**
   * Gets constData.
   *
   * @return value of constData
   */
  public Data getConstData() {
    return constData;
  }
}
