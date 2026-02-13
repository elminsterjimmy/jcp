package com.elminster.jcp.debug;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclaration;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import com.elminster.jcp.exception.JcpException;

/**
 * EvalVisitor extension that supports debugging.
 *
 * <p>Hooks into the evaluation process to check breakpoints and
 * implement stepping controls. Uses double-checked locking for
 * fast-path when no breakpoints are active.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DefaultDebugger debugger = new DefaultDebugger();
 * EvalContext context = new RootEvalContext();
 * DebuggingEvalVisitor visitor = new DebuggingEvalVisitor(context, debugger);
 *
 * // Set breakpoint at line 10
 * Breakpoint bp = debugger.setBreakpoint(10);
 *
 * // Start debugging in another thread (debug() handles attach/detach)
 * new Thread(() -> visitor.debug(program)).start();
 *
 * // Wait for breakpoint with timeout handling
 * long timeoutMs = 30_000;
 * long deadline = System.currentTimeMillis() + timeoutMs;
 * while (!debugger.isPaused()) {
 *     if (System.currentTimeMillis() > deadline) {
 *         debugger.stop();
 *         throw new RuntimeException("Timeout waiting for breakpoint");
 *     }
 *     Thread.sleep(10);
 * }
 *
 * // Inspect variables at breakpoint
 * Map<String, Data<?>> vars = debugger.getVariables();
 * }</pre>
 */
public class DebuggingEvalVisitor extends EvalVisitor {

  private final DefaultDebugger debugger;
  private int callDepth = 0;

  /**
   * Creates a debugging visitor.
   *
   * @param context  the evaluation context
   * @param debugger the debugger instance
   */
  public DebuggingEvalVisitor(EvalContext context, DefaultDebugger debugger) {
    super(context);
    this.debugger = debugger;
  }

  /**
   * Returns the associated debugger.
   *
   * @return the debugger instance
   */
  public DefaultDebugger getDebugger() {
    return debugger;
  }

  @Override
  public void visit(Node node) {
    // Fast-path: skip debugger checks when not attached
    if (!debugger.isAttached()) {
      super.visit(node);
      return;
    }

    // Check if we need to pause (breakpoint or stepping)
    boolean isStepping = debugger.getState() != DebugState.RUNNING;
    if (debugger.shouldPause(node, callDepth)) {
      debugger.pause(node, getContext(), callDepth, isStepping);
    }

    // Track call depth for step over/into/out
    boolean isFunctionCall = isFunctionCall(node);
    if (isFunctionCall) {
      callDepth++;
    }

    try {
      // Evaluate the node using parent implementation
      Evaluable evaluable = AstEvaluatorFactory.getEvaluator(node);
      Data eval = evaluable.eval(getContext());
      afterEval(eval);
    } catch (JcpException e) {
      throw e.withCallStack(getContext().getCallStack());
    } finally {
      if (isFunctionCall) {
        callDepth--;
      }
    }
  }

  /**
   * Starts the debugging session.
   * Attaches the debugger before visiting the program.
   *
   * @param program the program node to debug
   */
  public void debug(Node program) {
    debugger.attach();
    try {
      visit(program);
    } finally {
      debugger.detach();
    }
  }

  /**
   * Returns the current call depth.
   *
   * @return the call depth (0 = top level)
   */
  public int getCallDepth() {
    return callDepth;
  }

  private boolean isFunctionCall(Node node) {
    return node instanceof FunctionCallExpression
        || node instanceof FunctionDeclaration;
  }
}
