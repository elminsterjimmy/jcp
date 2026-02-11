package com.elminster.jcp.compile.exception;

import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.exception.JcpException;

/**
 * Exception thrown during compilation.
 */
public class CompileException extends JcpException {

  public CompileException(String message) {
    super(message);
  }

  public CompileException(String message, Throwable cause) {
    super(message, null, cause);
  }

  public CompileException(String message, SourceLocation location) {
    super(message, location);
  }

  public CompileException(String message, SourceLocation location, Throwable cause) {
    super(message, location, cause);
  }
}
