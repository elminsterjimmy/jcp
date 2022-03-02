package com.elminster.jcp.ast.expression.operation.operator;

public enum RelationalOperator implements BinaryOperator {

  EQUAL("=="),
  NOT_EQUAL("<>"),
  GREATER_THAN(">"),
  GREATER_THAN_OR_EQUAL(">="),
  LESS_THAN("<"),
  LESS_THAN_OR_EQUAL("<="),
  ;

  String symbol;

  RelationalOperator(String symbol) {
    this.symbol = symbol;
  }

  @Override
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
