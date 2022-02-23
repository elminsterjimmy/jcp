package com.elminster.jcp.eval.ast.excpetion;

import com.elminster.jcp.ast.Identifier;

abstract public class DeclarationException extends EvaluationException {

  protected Identifier id;
  protected Type type;

  public DeclarationException(Identifier id, Type type) {
    this.id = id;
    this.type = type;
  }

  /**
   * Returns the detail message string of this throwable.
   *
   * @return the detail message string of this {@code Throwable} instance
   * (which may be {@code null}).
   */
  @Override
  public String getMessage() {
    return String.format(getMessageFormat(), type, id);
  }

  abstract protected String getMessageFormat();

  public enum Type {
    VARIABLE,
    FUNCTION,
    DATATYPE
  }
}
