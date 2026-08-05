package com.elminster.jcp.compile.module;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.compile.AbstractCompileTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Convert module in compile mode.
 *
 * <p>Each method (and each {@code toString} overload) is verified by compiling an
 * inline {@code Assertions.assertEquals} / {@code Assertions.assertTrue} /
 * {@code Assertions.assertFalse} call inside the generated {@code main} method.
 * Overload resolution for {@code toString(int|double|boolean)} is exercised by
 * passing literal arguments of each type.
 */
public class ConvertCompileTest extends AbstractCompileTest {

    private StaticMethodCallExpression convert(String method, LiteralExpression... args) {
        return new StaticMethodCallExpression("Convert", method, args);
    }

    private StaticMethodCallExpression assertTrue(StaticMethodCallExpression actual) {
        return new StaticMethodCallExpression("Assertions", "assertTrue", actual);
    }

    private StaticMethodCallExpression assertFalse(StaticMethodCallExpression actual) {
        return new StaticMethodCallExpression("Assertions", "assertFalse", actual);
    }

    private StaticMethodCallExpression assertEq(LiteralExpression expected,
                                                StaticMethodCallExpression actual) {
        return new StaticMethodCallExpression("Assertions", "assertEquals", expected, actual);
    }

    private LiteralExpression str(String s) {
        return LiteralExpression.of(StringLiteral.of(s));
    }

    private LiteralExpression int_(int n) {
        return LiteralExpression.of(Literal.of(n));
    }

    private LiteralExpression dbl(double d) {
        return LiteralExpression.of(Literal.of(d));
    }

    private LiteralExpression bool(boolean b) {
        return LiteralExpression.of(Literal.of(b));
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

    // --- *ToString primitive converters (each takes a different param type) ---

    @Test
    void testIntToString() throws Exception {
        Method main = compileStatements("TestConvertIntToString",
            assertEq(str("42"), convert("intToString", int_(42))),
            assertEq(str("-7"), convert("intToString", int_(-7))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testDoubleToString() throws Exception {
        Method main = compileStatements("TestConvertDoubleToString",
            assertEq(str("3.14"), convert("doubleToString", dbl(3.14))),
            assertEq(str("0.0"), convert("doubleToString", dbl(0.0))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testBooleanToString() throws Exception {
        Method main = compileStatements("TestConvertBooleanToString",
            assertEq(str("true"), convert("booleanToString", bool(true))),
            assertEq(str("false"), convert("booleanToString", bool(false))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    // --- toInt ---

    @Test
    void testToInt() throws Exception {
        Method main = compileStatements("TestConvertToInt",
            assertEq(int_(42), convert("toInt", str("42"))),
            assertEq(int_(-7), convert("toInt", str("-7"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    // --- toDouble ---

    @Test
    void testToDouble() throws Exception {
        Method main = compileStatements("TestConvertToDouble",
            assertEq(dbl(3.14), convert("toDouble", str("3.14"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    // --- toBoolean (covers both Boolean.parseBoolean branches) ---

    @Test
    void testToBoolean() throws Exception {
        Method main = compileStatements("TestConvertToBoolean",
            assertTrue(convert("toBoolean", str("true"))),
            assertFalse(convert("toBoolean", str("false"))),
            assertFalse(convert("toBoolean", str("yes"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }
}
