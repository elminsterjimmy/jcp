package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@code Math} base-module STD class in eval mode.
 *
 * <p>Exercises every method with int and double arguments and verifies the
 * exact-match overload resolver: {@code Math.abs(-5)} returns an INT while
 * {@code Math.abs(-5.0)} returns a DOUBLE, with no type loss. Mixed-arg
 * {@code Math.min(1, 2.0)} promotes to the {@code (double,double)} overload.
 */
public class MathTest {

    private EvalContext newContext() {
        return new RootEvalContext();
    }

    private Object eval(String method, SystemDataType returnType, LiteralExpression... args) {
        EvalContext context = newContext();
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", returnType,
            new FunctionCallExpression(Identifier.fromName("Math." + method), args)
        ));
        new EvalVisitor(context).visit(program);
        return context.getVariable("result").get();
    }

    private FunctionCallExpression call(String method, Expression... args) {
        return new FunctionCallExpression(Identifier.fromName("Math." + method), args);
    }

    private Object evalExpr(SystemDataType returnType, Expression expr) {
        EvalContext context = newContext();
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("result", returnType, expr));
        new EvalVisitor(context).visit(program);
        return context.getVariable("result").get();
    }

    private LiteralExpression int_(int n) {
        return LiteralExpression.of(Literal.of(n));
    }

    private LiteralExpression dbl(double d) {
        return LiteralExpression.of(Literal.of(d));
    }

    // --- abs: exact-match dispatch keeps int as int, double as double ---

    @Test
    void testAbsInt() {
        assertEquals(5, eval("abs", SystemDataType.INT, int_(-5)));
        assertEquals(7, eval("abs", SystemDataType.INT, int_(7)));
        assertEquals(0, eval("abs", SystemDataType.INT, int_(0)));
    }

    @Test
    void testAbsDouble() {
        assertEquals(5.0, eval("abs", SystemDataType.DOUBLE, dbl(-5.0)));
        assertEquals(3.14, eval("abs", SystemDataType.DOUBLE, dbl(3.14)));
    }

    // --- sqrt: double-only, int arg widens ---

    @Test
    void testSqrt() {
        assertEquals(3.0, eval("sqrt", SystemDataType.DOUBLE, dbl(9.0)));
        assertEquals(0.0, eval("sqrt", SystemDataType.DOUBLE, dbl(0.0)));
        // int argument widens to double
        assertEquals(2.0, eval("sqrt", SystemDataType.DOUBLE, int_(4)));
    }

    // --- min / max: int and double overloads ---

    @Test
    void testMinInt() {
        assertEquals(1, eval("min", SystemDataType.INT, int_(1), int_(2)));
        assertEquals(-3, eval("min", SystemDataType.INT, int_(-3), int_(0)));
    }

    @Test
    void testMinDouble() {
        assertEquals(1.5, eval("min", SystemDataType.DOUBLE, dbl(1.5), dbl(2.5)));
    }

    @Test
    void testMinMixedArgsPromote() {
        // (int, double) has no int,int overload match; only (double,double) matches → 1.0
        assertEquals(1.0, eval("min", SystemDataType.DOUBLE, int_(1), dbl(2.0)));
    }

    @Test
    void testMaxInt() {
        assertEquals(2, eval("max", SystemDataType.INT, int_(1), int_(2)));
        assertEquals(0, eval("max", SystemDataType.INT, int_(-3), int_(0)));
    }

    @Test
    void testMaxDouble() {
        assertEquals(2.5, eval("max", SystemDataType.DOUBLE, dbl(1.5), dbl(2.5)));
    }

    // --- pow: double-only, int args widen ---

    @Test
    void testPow() {
        assertEquals(1024.0, eval("pow", SystemDataType.DOUBLE, dbl(2.0), dbl(10.0)));
        // int args widen to double
        assertEquals(8.0, eval("pow", SystemDataType.DOUBLE, int_(2), int_(3)));
    }

    // --- floor / ceil: double-only ---

    @Test
    void testFloor() {
        assertEquals(2.0, eval("floor", SystemDataType.DOUBLE, dbl(2.7)));
        assertEquals(-3.0, eval("floor", SystemDataType.DOUBLE, dbl(-2.1)));
    }

    @Test
    void testCeil() {
        assertEquals(3.0, eval("ceil", SystemDataType.DOUBLE, dbl(2.1)));
        assertEquals(-2.0, eval("ceil", SystemDataType.DOUBLE, dbl(-2.7)));
    }

    // --- implicit cast: combining an INT-returning and a DOUBLE-returning
    //     Math call in an arithmetic expression widens the result to DOUBLE ---

    @Test
    void testAbsIntPlusAbsDoubleWidensToDouble() {
        // Math.abs(-5) [INT 5] + Math.abs(-5.0) [DOUBLE 5.0] => 10.0 (DOUBLE)
        assertEquals(10.0, evalExpr(SystemDataType.DOUBLE,
            new Plus(call("abs", int_(-5)), call("abs", dbl(-5.0)))));
    }

    @Test
    void testMaxIntPlusSqrtDoubleWidensToDouble() {
        // Math.max(2, 3) [INT 3] + Math.sqrt(9.0) [DOUBLE 3.0] => 6.0 (DOUBLE)
        assertEquals(6.0, evalExpr(SystemDataType.DOUBLE,
            new Plus(call("max", int_(2), int_(3)), call("sqrt", dbl(9.0)))));
    }

    @Test
    void testFloorDoublePlusMinIntWidensToDouble() {
        // Math.floor(2.7) [DOUBLE 2.0] + Math.min(1, 4) [INT 1] => 3.0 (DOUBLE)
        assertEquals(3.0, evalExpr(SystemDataType.DOUBLE,
            new Plus(call("floor", dbl(2.7)), call("min", int_(1), int_(4)))));
    }
}
