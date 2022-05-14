package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.expression.literal.Literal;

public class LiteralExpression extends AbstractExpression {

  private Literal literal;

  public LiteralExpression(Literal literal) {
    this.literal = literal;
  }

  public static LiteralExpression of(Literal literal) {
    return new LiteralExpression(literal);
  }

  public static <T> LiteralExpression of(T value) {
    return new LiteralExpression(Literal.of(value));
  }

  @Override
  public String getName() {
    return "LITERAL";
  }

  public Literal getLiteral() {
    return literal;
  }
}

