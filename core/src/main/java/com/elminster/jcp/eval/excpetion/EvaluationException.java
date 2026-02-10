package com.elminster.jcp.eval.excpetion;

import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.exception.JcpException;

/**
 * Base exception for interpreter evaluation errors.
 */
public class EvaluationException extends JcpException {

  public EvaluationException() {
    super((String) null);
  }

  public EvaluationException(String message) {
    super(message);
  }

  public EvaluationException(Throwable e) {
    super(e);
  }

  public EvaluationException(String message, SourceLocation location) {
    super(message, location);
  }

  public EvaluationException(String message, SourceLocation location, Throwable cause) {
    super(message, location, cause);
  }
}
