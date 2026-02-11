package com.elminster.jcp.eval.excpetion;

import com.elminster.jcp.ast.SourceLocation;

public class FunctionArgumentsLengthException extends EvaluationException {

  public FunctionArgumentsLengthException() {
    this(null);
  }

  public FunctionArgumentsLengthException(SourceLocation location) {
    super("Function arguments length mismatch", location);
  }
}
