package com.elminster.jcp.ast.expression.operator;

public enum RelationalOperator implements BinaryOperator {

  EQUAL("EQUAL"),
  NOT_EQUAL("NOT_EQUAL"),
  GREATER_THAN("GREATER_THAN"),
  GREATER_THAN_OR_EQUAL("GREATER_THAN_EQUAL"),
  LESS_THAN("LESS_THAN"),
  LESS_THAN_OR_EQUAL("LESS_THAN_EQUAL"),
  ;

  String name;

  RelationalOperator(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }
}
