package com.elminster.jcp.compile.module;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.compile.AbstractCompileTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@code Math} base-module STD class in compile mode.
 *
 * <p>Each method is verified by compiling an inline {@code Assertions.assertEquals}
 * call inside the generated {@code main} method. The exact-match overload resolver
 * is exercised by passing int vs double literals: {@code Math.abs(-5)} selects
 * {@code abs(int)} (INT result), {@code Math.abs(-5.0)} selects {@code abs(double)}
 * (DOUBLE result). Mixed-arg {@code Math.min(1, 2.0)} promotes to {@code (double,double)}.
 */
public class MathCompileTest extends AbstractCompileTest {

    private StaticMethodCallExpression math(String method, LiteralExpression... args) {
        return new StaticMethodCallExpression("Math", method, args);
    }

    private StaticMethodCallExpression assertEq(LiteralExpression expected,
                                                StaticMethodCallExpression actual) {
        return new StaticMethodCallExpression("Assertions", "assertEquals", expected, actual);
    }

    private LiteralExpression int_(int n) {
        return LiteralExpression.of(Literal.of(n));
    }

    private LiteralExpression dbl(double d) {
        return LiteralExpression.of(Literal.of(d));
    }

    private Method compileStatements(String baseName, StaticMethodCallExpression... calls) throws Exception {
        Block program = new BlockImpl();
        for (StaticMethodCallExpression c : calls) {
            program.addStatement(new ExpressionStatement(c));
        }
        String className = uniqueClassName(baseName);
        Class<?> clazz = compiler.compileAndLoad(program, className);
        return clazz.getMethod("main", String[].class);
    }

    // --- abs: exact-match dispatch keeps int as int, double as double ---

    @Test
    void testAbsInt() throws Exception {
        Method main = compileStatements("TestMathAbsInt",
            assertEq(int_(5), math("abs", int_(-5))),
            assertEq(int_(7), math("abs", int_(7))),
            assertEq(int_(0), math("abs", int_(0))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testAbsDouble() throws Exception {
        Method main = compileStatements("TestMathAbsDouble",
            assertEq(dbl(5.0), math("abs", dbl(-5.0))),
            assertEq(dbl(3.14), math("abs", dbl(3.14))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    // --- sqrt: double-only, int arg widens ---

    @Test
    void testSqrt() throws Exception {
        Method main = compileStatements("TestMathSqrt",
            assertEq(dbl(3.0), math("sqrt", dbl(9.0))),
            assertEq(dbl(0.0), math("sqrt", dbl(0.0))),
            assertEq(dbl(2.0), math("sqrt", int_(4))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    // --- min / max: int and double overloads ---

    @Test
    void testMinInt() throws Exception {
        Method main = compileStatements("TestMathMinInt",
            assertEq(int_(1), math("min", int_(1), int_(2))),
            assertEq(int_(-3), math("min", int_(-3), int_(0))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testMinDouble() throws Exception {
        Method main = compileStatements("TestMathMinDouble",
            assertEq(dbl(1.5), math("min", dbl(1.5), dbl(2.5))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testMinMixedArgsPromote() throws Exception {
        // (int, double) matches only (double,double) → 1.0
        Method main = compileStatements("TestMathMinMixed",
            assertEq(dbl(1.0), math("min", int_(1), dbl(2.0))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testMaxInt() throws Exception {
        Method main = compileStatements("TestMathMaxInt",
            assertEq(int_(2), math("max", int_(1), int_(2))),
            assertEq(int_(0), math("max", int_(-3), int_(0))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testMaxDouble() throws Exception {
        Method main = compileStatements("TestMathMaxDouble",
            assertEq(dbl(2.5), math("max", dbl(1.5), dbl(2.5))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    // --- pow: double-only, int args widen ---

    @Test
    void testPow() throws Exception {
        Method main = compileStatements("TestMathPow",
            assertEq(dbl(1024.0), math("pow", dbl(2.0), dbl(10.0))),
            assertEq(dbl(8.0), math("pow", int_(2), int_(3))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    // --- floor / ceil: double-only ---

    @Test
    void testFloor() throws Exception {
        Method main = compileStatements("TestMathFloor",
            assertEq(dbl(2.0), math("floor", dbl(2.7))),
            assertEq(dbl(-3.0), math("floor", dbl(-2.1))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testCeil() throws Exception {
        Method main = compileStatements("TestMathCeil",
            assertEq(dbl(3.0), math("ceil", dbl(2.1))),
            assertEq(dbl(-2.0), math("ceil", dbl(-2.7))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }
}
