package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.data.FlowData;

public class ConstantExpression extends AbstractExpression {

  private FlowData constData;

  public ConstantExpression(FlowData constData) {
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
  public FlowData getConstData() {
    return constData;
  }
}
