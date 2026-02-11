package com.elminster.jcp.eval.excpetion;

import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.eval.data.DataType;

public class CannotCastException extends EvaluationException {

  public CannotCastException(DataType actual, DataType expect) {
    this(actual, expect, null);
  }

  public CannotCastException(DataType actual, DataType expect, SourceLocation location) {
    super(String.format("Cannot cast data type from [%s] to [%s]", actual, expect), location);
  }
}
