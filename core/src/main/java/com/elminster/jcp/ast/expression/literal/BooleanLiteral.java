package com.elminster.jcp.ast.expression.literal;

/**
 * Boolean literal value.
 */
public class BooleanLiteral extends Literal<Boolean> {

  private BooleanLiteral(Boolean value) {
    super(value);
  }

  public static BooleanLiteral of(Boolean value) {
    return new BooleanLiteral(value);
  }

  @Override
  public String getName() {
    return "BooleanLiteral";
  }
}
