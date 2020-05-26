package com.elminster.jcp.ast.expression.operator;

public enum PostfixOperator implements UnaryOperator {

  PLUS_PLUS("PLUS_PLUS"),
  MINUS_MINUS("MINUS_MINUS"),
  ARRAY("ARRAY"),
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
