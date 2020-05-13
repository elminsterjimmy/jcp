package com.elminster.jcp.ast.expression.operator;

public enum PostfixOperator implements UnaryOperator {

  PLUS_PLUS("++"),
  MINUS_MINUS("--"),
  ;

  String name;

  PostfixOperator(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }
}
