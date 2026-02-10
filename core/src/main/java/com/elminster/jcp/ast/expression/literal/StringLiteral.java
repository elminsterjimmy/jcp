package com.elminster.jcp.ast.expression.literal;

/**
 * String literal value.
 */
public class StringLiteral extends Literal<String> {

  private StringLiteral(String value) {
    super(value);
  }

  public static StringLiteral of(String value) {
    return new StringLiteral(value);
  }

  @Override
  public String getName() {
    return "StringLiteral";
  }
}
