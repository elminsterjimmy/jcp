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

  private SourceLocation location;

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
   * Sets the source location for this exception.
   *
   * <p>This method allows attaching location after construction while
   * preserving the exception subclass type. It's preferred over
   * {@link #withLocation(SourceLocation)} when you need to preserve
   * the specific exception type.
   *
   * @param location source location to attach (may be null)
   */
  public void setLocation(SourceLocation location) {
    if (this.location == null) {
      this.location = location;
    }
  }

  /**
   * Returns a new exception with the specified location.
   *
   * <p>If this exception already has a location, returns this instance unchanged.
   * The returned exception preserves the original stack trace and cause chain.
   *
   * <p>Note: This method returns a new JcpException instance, losing subclass type.
   * For subclasses, prefer using {@link #setLocation(SourceLocation)} instead.
   *
   * @param location source location to attach
   * @return new exception with location, or this if location already set
   */
  public JcpException withLocation(SourceLocation location) {
    if (this.location != null) {
      return this;
    }
    JcpException newEx = new JcpException(super.getMessage(), location, getCause());
    newEx.setStackTrace(this.getStackTrace());
    return newEx;
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
    String msg = getMessage();
    if (location == null) {
      return msg;
    }
    // Remove the " at location" suffix since we'll add full source context
    String baseMsg = msg;
    String locationStr = " at " + location.toString();
    if (msg.endsWith(locationStr)) {
      baseMsg = msg.substring(0, msg.length() - locationStr.length());
    }
    return String.format("%s at %s\n%s", baseMsg, location.toString(), location.formatWithSource());
  }
}
