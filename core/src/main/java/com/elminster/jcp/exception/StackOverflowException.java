package com.elminster.jcp.exception;

/**
 * Thrown when call stack depth exceeds configured limit.
 *
 * <p>This is a security measure to prevent DoS attacks via infinite recursion.
 * Default limit is 1000, configurable via -Djcp.maxStackDepth system property.
 */
public class StackOverflowException extends JcpException {

  /**
   * Creates a stack overflow exception with the given message.
   *
   * @param message error message
   */
  public StackOverflowException(String message) {
    super(message);
  }
}
