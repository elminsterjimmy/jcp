package com.elminster.jcp.ast.expression.literal;

/**
 * Integer literal value.
 */
public class IntLiteral extends Literal<Integer> {

  private IntLiteral(Integer value) {
    super(value);
  }

  public static IntLiteral of(Integer value) {
    return new IntLiteral(value);
  }

  @Override
  public String getName() {
    return "IntLiteral";
  }
}
