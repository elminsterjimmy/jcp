package com.elminster.jcp.exception;

import com.elminster.jcp.ast.SourceLocation;

/**
 * Base exception for all JCP runtime errors with source location and call stack support.
 *
 * <p>Provides location-aware error messages in GCC-style format:
 * <pre>
 * RuntimeError: Division by zero at math.jcp:15:12
 *   15 |   return a / b;
 *               ^~~~~
 *
 * Stack trace:
 *   at divide(math.jcp:15:12)
 *   at calculate(main.jcp:8:5)
 *   at main(main.jcp:3:1)
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Throw with location from AST node
 * throw new JcpException("Division by zero", node.getLocation());
 *
 * // Attach location via fluent API (preserves stack trace)
 * throw new SomeException("error").withLocation(node.getLocation());
 *
 * // Attach call stack via fluent API
 * throw e.withCallStack(context.getCallStack());
 * }</pre>
 *
 * @see SourceLocation
 * @see CallStack
 */
public class JcpException extends RuntimeException {

  private final SourceLocation location;
  private CallStack callStack;

  /**
   * Creates an exception with message and source location.
   *
   * @param message  error message
   * @param location source location (may be null)
   */
  public JcpException(String message, SourceLocation location) {
    super(message);
    this.location = location;
    this.callStack = null;
  }

  /**
   * Creates an exception with message, location, and cause chain.
   *
   * @param message  error message
   * @param location source location (may be null)
   * @param cause    underlying cause
   */
  public JcpException(String message, SourceLocation location, Throwable cause) {
    super(message, cause);
    this.location = location;
    this.callStack = null;
  }

  /**
   * Creates an exception with message, location, and call stack.
   *
   * @param message   error message
   * @param location  source location (may be null)
   * @param callStack call stack snapshot (may be null)
   */
  public JcpException(String message, SourceLocation location, CallStack callStack) {
    super(message);
    this.location = location;
    this.callStack = callStack != null ? callStack.copy() : null;
  }

  /**
   * Creates an exception with message, location, call stack, and cause.
   *
   * @param message   error message
   * @param location  source location (may be null)
   * @param callStack call stack snapshot (may be null)
   * @param cause     underlying cause
   */
  public JcpException(String message, SourceLocation location, CallStack callStack, Throwable cause) {
    super(message, cause);
    this.location = location;
    this.callStack = callStack != null ? callStack.copy() : null;
  }

  /**
   * Creates an exception with message only (backward compatible).
   *
   * @param message error message
   */
  public JcpException(String message) {
    this(message, (SourceLocation) null);
  }

  /**
   * Creates an exception with cause only (backward compatible).
   *
   * @param cause underlying cause
   */
  public JcpException(Throwable cause) {
    super(cause);
    this.location = null;
    this.callStack = null;
  }

  /**
   * Returns the source location where this error occurred.
   *
   * @return source location, or null if not available
   */
  public SourceLocation getLocation() {
    return location;
  }

  /**
   * Returns the call stack at the time of the error.
   *
   * @return call stack snapshot, or null if not available
   */
  public CallStack getCallStack() {
    return callStack;
  }

  /**
   * Returns the base message without location suffix.
   *
   * <p>Subclasses should call this method when creating new instances
   * in withLocation() or withCallStack() to avoid double location suffix.
   *
   * @return the original error message
   */
  protected String getBaseMessage() {
    return super.getMessage();
  }

  /**
   * Returns a new exception with the specified location, preserving the subclass type.
   *
   * <p>If this exception already has a location, returns this instance unchanged.
   * The returned exception preserves the original stack trace and cause chain.
   *
   * <p>Subclasses should override this method to return their own type:
   * <pre>{@code
   * @Override
   * @SuppressWarnings("unchecked")
   * public <T extends JcpException> T withLocation(SourceLocation location) {
   *     if (getLocation() != null) return (T) this;
   *     MyException newEx = new MyException(getBaseMessage(), location);
   *     newEx.setStackTrace(this.getStackTrace());
   *     return (T) newEx;
   * }
   * }</pre>
   *
   * @param <T>      the exception type
   * @param location source location to attach
   * @return new exception with location, or this if location already set
   */
  @SuppressWarnings("unchecked")
  public <T extends JcpException> T withLocation(SourceLocation location) {
    if (this.location != null) {
      return (T) this;
    }
    JcpException newEx = new JcpException(getBaseMessage(), location, callStack, getCause());
    newEx.setStackTrace(this.getStackTrace());
    return (T) newEx;
  }

  /**
   * Attaches the call stack to this exception, preserving the subclass type.
   *
   * <p>If this exception already has a call stack, returns this instance unchanged.
   * Unlike {@link #withLocation(SourceLocation)}, this method mutates the exception
   * in place to preserve the exact exception type for proper exception handling.
   *
   * @param <T>       the exception type
   * @param callStack call stack to attach
   * @return this exception with call stack attached
   */
  @SuppressWarnings("unchecked")
  public <T extends JcpException> T withCallStack(CallStack callStack) {
    if (this.callStack != null) {
      return (T) this;
    }
    this.callStack = callStack != null ? callStack.copy() : null;
    return (T) this;
  }

  /**
   * Returns the error message with simple location suffix.
   *
   * <p>Format: "message at file:line:col" or just "message" if no location.
   * For full source context display, use {@link #getFormattedMessage()}.
   *
   * <p>Subclasses that compute message dynamically should override this method
   * and call {@link #appendLocation(String)} to add the location suffix.
   *
   * @return error message with location suffix
   */
  @Override
  public String getMessage() {
    return appendLocation(super.getMessage());
  }

  /**
   * Appends location suffix to a message.
   *
   * <p>Subclasses that compute message dynamically should call this from their
   * overridden getMessage() method:
   * <pre>{@code
   * @Override
   * public String getMessage() {
   *     return appendLocation(String.format(MESSAGE_PATTERN, ...));
   * }
   * }</pre>
   *
   * @param message the base error message
   * @return message with " at file:line:col" suffix if location is set
   */
  protected String appendLocation(String message) {
    if (location == null) {
      return message;
    }
    return String.format("%s at %s", message, location.toString());
  }

  /**
   * Returns the formatted error message with full source context.
   *
   * <p>Example output:
   * <pre>
   * Division by zero at math.jcp:15:12
   * math.jcp:15:12
   *   15 |   return a / b;
   *               ^~~~~
   * </pre>
   *
   * @return formatted message with source context, or simple message if no location
   */
  public String getFormattedMessage() {
    if (location == null) {
      return getMessage();
    }
    return String.format("%s\n%s", getMessage(), location.formatWithSource());
  }

  /**
   * Returns the full error message with location context and call stack.
   *
   * <p>Example output:
   * <pre>
   * Division by zero at math.jcp:15:12
   * math.jcp:15:12
   *   15 |   return a / b;
   *               ^~~~~
   *
   * Stack trace:
   *   at divide(math.jcp:15:12)
   *   at calculate(main.jcp:8:5)
   *   at main(main.jcp:3:1)
   * </pre>
   *
   * @return full message with location and stack trace
   */
  public String getFullMessage() {
    StringBuilder sb = new StringBuilder(getFormattedMessage());
    if (callStack != null && !callStack.isEmpty()) {
      sb.append("\n\n").append(callStack.formatStackTrace());
    }
    return sb.toString();
  }
}
