package com.elminster.jcp.ast.expression.operator;

public enum ArithmeticOperator implements BinaryOperator {

  PLUS("PLUS"),
  MINUS("MINUS"),
  MULTIPLY("MULTI"),
  DIVIDE("DIVIDE"),
  MOD("MOD"),
  ;

  String name;

  ArithmeticOperator(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }
}
