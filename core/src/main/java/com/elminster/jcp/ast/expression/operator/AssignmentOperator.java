package com.elminster.jcp.ast.expression.operator;

public enum AssignmentOperator implements Operator {

  ASSIGNMENT("="),
  PLUS_ASSIGNMENT("+="),
  MINUS_ASSIGNMENT("-="),
  MULTIPLE_ASSIGNMENT("*="),
  DEV_ASSIGNMENT("/="),
  MOD_ASSIGNMENT("%=");

  private String name;

  AssignmentOperator(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
