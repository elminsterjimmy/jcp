package com.elminster.jcp.eval.ast.excpetion;

import com.elminster.jcp.ast.Identifier;

public class UndeclaredException extends DeclarationException {

  private static final String MESSAGE_PATTERN = "Undeclared %s [%s].";

  public UndeclaredException(Identifier id, Type type) {
    super(id, type);
  }

  @Override
  protected String getMessageFormat() {
    return MESSAGE_PATTERN;
  }

  public static UndeclaredException throwUndeclaredVariableException(Identifier id) {
    throw new UndeclaredException(id, Type.VARIABLE);
  }

  public static UndeclaredException throwUndeclaredFunctionException(Identifier id) {
    throw new UndeclaredException(id, Type.FUNCTION);
  }
}
