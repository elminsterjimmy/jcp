package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.expression.literal.Literal;

public class LiteralExpression extends AbstractExpression {

  private Literal literal;

  public LiteralExpression(Literal literal) {
    this.literal = literal;
  }

  @Override
  public String getName() {
    return "LITERAL";
  }

  public Literal getLiteral() {
    return literal;
  }
}

