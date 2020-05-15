package com.elminster.jcp.eval.ast.excpetion;

public class AlreadyDeclaredException extends DeclarationException {

  private static final String MESSAGE_PATTERN = "Already declared %s [%s].";

  public AlreadyDeclaredException(String id, Type type) {
    super(id, type);
  }

  @Override
  protected String getMessageFormat() {
    return MESSAGE_PATTERN;
  }

  public static AlreadyDeclaredException throwAlreadyDeclaredVariableException(String id) {
    throw new AlreadyDeclaredException(id, Type.VARIABLE);
  }

  public static AlreadyDeclaredException throwAlreadyDeclaredFunctionException(String id) {
    throw new AlreadyDeclaredException(id, Type.FUNCTION);
  }
}
