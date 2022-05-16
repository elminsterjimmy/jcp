package com.elminster.jcp.ast.expression.operation.operator;

public enum AssignmentOperator implements BinaryOperator {

  ASSIGNMENT("="),
  PLUS_ASSIGNMENT("+="),
  MINUS_ASSIGNMENT("-="),
  MULTI_ASSIGNMENT("*="),
  DIVIDE_ASSIGNMENT("/="),
  MOD_ASSIGNMENT("%=");

  private String symbol;

  AssignmentOperator(String symbol) {
    this.symbol = symbol;
  }

  public String getName() {
    return this.name();
  }

  public String getSymbol() {
    return symbol;
  }

  public String toString() {
    return this.getSymbol();
  }
}
