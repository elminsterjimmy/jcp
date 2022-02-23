package com.elminster.jcp.eval.ast.excpetion;

import com.elminster.jcp.ast.Identifier;

public class AlreadyDeclaredException extends DeclarationException {

  private static final String MESSAGE_PATTERN = "Already declared %s [%s].";

  public AlreadyDeclaredException(Identifier id, Type type) {
    super(id, type);
  }

  @Override
  protected String getMessageFormat() {
    return MESSAGE_PATTERN;
  }

  public static AlreadyDeclaredException throwAlreadyDeclaredVariableException(Identifier id) {
    throw new AlreadyDeclaredException(id, Type.VARIABLE);
  }

  public static AlreadyDeclaredException throwAlreadyDeclaredFunctionException(Identifier id) {
    throw new AlreadyDeclaredException(id, Type.FUNCTION);
  }

  public static AlreadyDeclaredException throwAlreadyDeclaredDataTypeException(Identifier id) {
    throw new AlreadyDeclaredException(id, Type.DATATYPE);
  }
}
