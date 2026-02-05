package com.elminster.common.util;

public class AssertException extends RuntimeException {
  public AssertException(String message) {
    super(message);
  }

  public AssertException(String message, Throwable cause) {
    super(message, cause);
  }
}
