package com.elminster.jcp.ast.expression.operator;

public enum ArithmeticOperator implements BinaryOperator {

  PLUS("+"),
  MINUS("-"),
  MULTIPLY("*"),
  DIVIDE("/"),
  MOD("%"),
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
