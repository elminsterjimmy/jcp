package com.elminster.jcp.eval.excpetion;

import com.elminster.jcp.ast.SourceLocation;

abstract public class DeclarationException extends EvaluationException {

  public DeclarationException() {
    super();
  }

  public DeclarationException(String message, SourceLocation location) {
    super(message, location);
  }
}
