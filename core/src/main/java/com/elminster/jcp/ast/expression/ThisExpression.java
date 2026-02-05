package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.AbstractExpression;

/**
 * AST node for the 'this' keyword, referencing the current instance
 * in constructors and instance methods.
 */
public class ThisExpression extends AbstractExpression {

  @Override
  public String getName() {
    return "THIS";
  }
}
