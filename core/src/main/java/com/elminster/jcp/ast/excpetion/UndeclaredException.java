package com.elminster.jcp.ast.excpetion;

public class UndeclaredException extends DeclarationException {

  private static final String MESSAGE_PATTERN = "Undeclared %s [%s].";

  public UndeclaredException(String id, Type type) {
    super(id, type);
  }

  @Override
  protected String getMessageFormat() {
    return MESSAGE_PATTERN;
  }

  public static UndeclaredException throwUndeclaredVariableException(String id) {
    throw new UndeclaredException(id, Type.VARIABLE);
  }

  public static UndeclaredException throwUndeclaredFunctionException(String id) {
    throw new UndeclaredException(id, Type.FUNCTION);
  }
}
