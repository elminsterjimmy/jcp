package com.elminster.jcp.ast.expression.operator;

public enum LogicalOperator {
  AND("AND"),
  OR("OR"),
  NOT("NOT")
  ;

  String name;

  LogicalOperator(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
