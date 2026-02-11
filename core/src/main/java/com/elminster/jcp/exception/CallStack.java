package com.elminster.jcp.exception;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Thread-safe call stack for tracking function execution.
 *
 * <p>Used by interpreter to track function calls.
 * Used by compiler adapter to convert JVM stack traces.
 *
 * <p><b>Security:</b> Stack depth is limited to prevent DoS attacks
 * via infinite recursion. Configure via system property.
 */
public final class CallStack {
  /**
   * Maximum stack depth to prevent DoS via infinite recursion.
   * Configurable via: -Djcp.maxStackDepth=2000
   */
  public static final int MAX_STACK_DEPTH =
      Integer.parseInt(System.getProperty("jcp.maxStackDepth", "1000"));

  private final Deque<StackFrame> frames;

  /**
   * Creates an empty call stack.
   */
  public CallStack() {
    this.frames = new ArrayDeque<>(32);
  }

  private CallStack(Deque<StackFrame> frames) {
    this.frames = new ArrayDeque<>(frames);
  }

  /**
   * Push a frame onto the stack.
   *
   * @param frame the stack frame to push
   * @throws StackOverflowException if max depth exceeded (security limit)
   */
  public void push(StackFrame frame) {
    if (frames.size() >= MAX_STACK_DEPTH) {
      throw new StackOverflowException(
          "Maximum call stack depth exceeded: " + MAX_STACK_DEPTH +
          "\nLast frame: " + frame.toString()
      );
    }
    frames.push(frame);
  }

  /**
   * Pop a frame from the stack.
   *
   * @return the popped frame, or null if stack is empty
   */
  public StackFrame pop() {
    return frames.isEmpty() ? null : frames.pop();
  }

  /**
   * Peek at the top frame without removing it.
   *
   * @return the top frame, or null if stack is empty
   */
  public StackFrame peek() {
    return frames.isEmpty() ? null : frames.peek();
  }

  /**
   * Checks if the stack is empty.
   *
   * @return true if no frames on stack
   */
  public boolean isEmpty() {
    return frames.isEmpty();
  }

  /**
   * Returns the number of frames on the stack.
   *
   * @return frame count
   */
  public int size() {
    return frames.size();
  }

  /**
   * Returns frames in call order (most recent first).
   *
   * @return unmodifiable list of frames
   */
  public List<StackFrame> getFrames() {
    return Collections.unmodifiableList(new ArrayList<>(frames));
  }

  /**
   * Creates an immutable copy for exception attachment.
   *
   * @return new CallStack with same frames
   */
  public CallStack copy() {
    return new CallStack(this.frames);
  }

  /**
   * Formats stack trace for display.
   *
   * <pre>
   * Stack trace:
   *   at divide(math.jcp:15:12)
   *   at calculate(main.jcp:8:5)
   *   at main(main.jcp:3:1)
   * </pre>
   *
   * @return formatted stack trace string, or empty string if no frames
   */
  public String formatStackTrace() {
    if (frames.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder("Stack trace:\n");
    for (StackFrame frame : frames) {
      sb.append("  at ").append(frame).append("\n");
    }
    return sb.toString().trim();
  }
}
