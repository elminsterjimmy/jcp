package com.elminster.jcp.ast.expression.literal;

public class LongLiteral extends Literal<Long> {

  private LongLiteral(Long value) {
    super(value);
  }

  public static LongLiteral of(Long value) {
    return new LongLiteral(value);
  }

  public static LongLiteral of(long value) {
    return new LongLiteral(value);
  }

  @Override
  public String getName() {
    return "LongLiteral";
  }
}
