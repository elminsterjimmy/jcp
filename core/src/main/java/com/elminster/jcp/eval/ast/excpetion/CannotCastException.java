package com.elminster.jcp.eval.ast.excpetion;

import com.elminster.jcp.eval.data.DataType;

public class CannotCastException extends RuntimeException {

  public CannotCastException(DataType actual, DataType expect) {
    super(String.format("Cannot cast data type from [%s] to [%s]", actual, expect));
  }
}
