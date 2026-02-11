package com.elminster.jcp.eval;

import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.ast.statement.function.AbstractFunction;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.context.DefaultEvalContext;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import com.elminster.jcp.exception.CallStack;
import com.elminster.jcp.exception.JcpException;
import com.elminster.jcp.exception.StackFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for call stack tracking in the interpreter.
 */
class StackTraceIntegrationTest {

  @Test
  void testCallStackPushPopDuringFunctionExecution() {
    // Verify that call stack is properly maintained during function execution
    // by checking the stack during execution (via a successful call)

    EvalContext context = new RootEvalContext();
    EvalVisitor visitor = new EvalVisitor(context);

    // Create simple function: func add(a: int, b: int) -> int { return a + b }
    Plus addExpr = new Plus(
        new VariableExpression(IdentifierExpression.of("a")),
        new VariableExpression(IdentifierExpression.of("b"))
    );
    addExpr.setLocation(SourceLocation.of("test.jcp", 2, 10));

    ReturnStatement returnStmt = new ReturnStatement(addExpr);
    returnStmt.setLocation(SourceLocation.of("test.jcp", 2, 3));

    AbstractFunction addFunc = new AbstractFunction(
        IdentifierExpression.of("add"),
        new ParameterDef[]{
            ParameterDef.of("a", SystemDataType.INT),
            ParameterDef.of("b", SystemDataType.INT)
        },
        SystemDataType.INT,
        returnStmt
    );
    addFunc.setLocation(SourceLocation.of("test.jcp", 1, 1));
    context.addFunction(addFunc);

    // Call add(3, 5)
    FunctionCallExpression call = new FunctionCallExpression(
        IdentifierExpression.of("add"),
        LiteralExpression.of(3),
        LiteralExpression.of(5)
    );
    call.setLocation(SourceLocation.of("test.jcp", 5, 1));

    Block callBlock = new BlockImpl();
    callBlock.addStatement(new VariableDeclarationImpl(
        "result", SystemDataType.INT, call));

    // After successful execution, stack should be empty
    visitor.visit(callBlock);

    assertEquals(8, context.getVariable("result").get());
    assertTrue(context.getCallStack().isEmpty(), "Call stack should be empty after function returns");
  }

  @Test
  void testCallStackWithExceptionAttachment() {
    // Test that exceptions get call stack attached when thrown during execution
    // For now, we verify the exception type preservation and stack attachment mechanism

    RootEvalContext context = new RootEvalContext();

    // Push contexts with stack frames to simulate function calls
    // (Now stack frames are embedded in EvalContext, not pushed to a separate CallStack)
    DefaultEvalContext mainContext = new DefaultEvalContext();
    mainContext.setStackFrame(StackFrame.of("main", SourceLocation.of("main.jcp", 1, 1)));
    context.getContextStack().push(mainContext);

    DefaultEvalContext workContext = new DefaultEvalContext();
    workContext.setStackFrame(StackFrame.of("doWork", SourceLocation.of("work.jcp", 10, 5)));
    context.getContextStack().push(workContext);

    // getCallStack() now builds from context stack
    assertEquals(2, context.getCallStack().size());

    // Create exception and attach call stack
    CannotCastException ex = new CannotCastException(SystemDataType.INT, SystemDataType.STRING);
    ex.withCallStack(context.getCallStack());

    // Verify exception has call stack
    assertNotNull(ex.getCallStack());
    assertEquals(2, ex.getCallStack().size());

    // Verify exception type is preserved
    assertTrue(ex instanceof CannotCastException);

    // Verify stack trace format
    String trace = ex.getCallStack().formatStackTrace();
    assertTrue(trace.contains("doWork"));
    assertTrue(trace.contains("main"));
  }

  @Test
  void testStackTracePreservedAcrossRethrow() {
    // Verify stack trace is not overwritten when withCallStack is called multiple times

    RootEvalContext context = new RootEvalContext();

    // Push a context with a frame
    DefaultEvalContext func1Context = new DefaultEvalContext();
    func1Context.setStackFrame(StackFrame.of("func1", SourceLocation.of("test.jcp", 1, 1)));
    context.getContextStack().push(func1Context);

    // Create exception and attach stack
    JcpException ex = new JcpException("test error");
    ex.withCallStack(context.getCallStack());

    // Push another context with a frame
    DefaultEvalContext func2Context = new DefaultEvalContext();
    func2Context.setStackFrame(StackFrame.of("func2", SourceLocation.of("test.jcp", 2, 1)));
    context.getContextStack().push(func2Context);

    // Try to attach stack again - should be no-op (exception keeps original stack)
    ex.withCallStack(context.getCallStack());

    // Exception should still have only one frame (the one captured at first attachment)
    assertEquals(1, ex.getCallStack().size());
    assertEquals("func1", ex.getCallStack().peek().getFunctionName());
  }

  @Test
  void testGetFullMessageWithStack() {
    // Verify getFullMessage() includes stack trace

    RootEvalContext context = new RootEvalContext();

    // Push contexts with stack frames
    DefaultEvalContext mainContext = new DefaultEvalContext();
    mainContext.setStackFrame(StackFrame.of("main", SourceLocation.of("main.jcp", 3, 1)));
    context.getContextStack().push(mainContext);

    DefaultEvalContext calcContext = new DefaultEvalContext();
    calcContext.setStackFrame(StackFrame.of("calculate", SourceLocation.of("main.jcp", 8, 5)));
    context.getContextStack().push(calcContext);

    DefaultEvalContext divContext = new DefaultEvalContext();
    divContext.setStackFrame(StackFrame.of("divide", SourceLocation.of("math.jcp", 15, 12)));
    context.getContextStack().push(divContext);

    SourceLocation errorLoc = SourceLocation.of("math.jcp", 15, 12, "  return a / b;");
    JcpException ex = new JcpException("Division by zero", errorLoc);
    ex.withCallStack(context.getCallStack());

    String fullMessage = ex.getFullMessage();

    // Should contain error message
    assertTrue(fullMessage.contains("Division by zero"));

    // Should contain location
    assertTrue(fullMessage.contains("math.jcp:15:12"));

    // Should contain source line
    assertTrue(fullMessage.contains("return a / b"));

    // Should contain stack trace header
    assertTrue(fullMessage.contains("Stack trace:"));

    // Should contain all frames
    assertTrue(fullMessage.contains("at divide"));
    assertTrue(fullMessage.contains("at calculate"));
    assertTrue(fullMessage.contains("at main"));
  }
}
