package com.elminster.jcp.ast.expression.literal;

/**
 * Double literal value.
 */
public class DoubleLiteral extends Literal<Double> {

  private DoubleLiteral(Double value) {
    super(value);
  }

  public static DoubleLiteral of(Double value) {
    return new DoubleLiteral(value);
  }

  @Override
  public String getName() {
    return "DoubleLiteral";
  }
}
