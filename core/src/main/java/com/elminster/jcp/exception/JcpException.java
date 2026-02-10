package com.elminster.jcp.exception;

import com.elminster.jcp.ast.SourceLocation;

/**
 * Base exception for all JCP runtime errors with source location support.
 *
 * <p>Provides location-aware error messages in GCC-style format:
 * <pre>
 * RuntimeError: Division by zero at math.jcp:15:12
 *   15 |   return a / b;
 *               ^~~~~
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Throw with location from AST node
 * throw new JcpException("Division by zero", node.getLocation());
 *
 * // Attach location via fluent API (preserves stack trace)
 * throw new SomeException("error").withLocation(node.getLocation());
 * }</pre>
 *
 * @see SourceLocation
 */
public class JcpException extends RuntimeException {

  private final SourceLocation location;

  /**
   * Creates an exception with message and source location.
   *
   * @param message  error message
   * @param location source location (may be null)
   */
  public JcpException(String message, SourceLocation location) {
    super(message);
    this.location = location;
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
    JcpException newEx = new JcpException(super.getMessage(), location, getCause());
    newEx.setStackTrace(this.getStackTrace());
    return (T) newEx;
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
}
