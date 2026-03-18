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
 * Tests for Strings module in compile mode.
 *
 * <p>All methods are verified by compiling inline {@code Assertions.assertEquals} /
 * {@code Assertions.assertTrue} / {@code Assertions.assertFalse} calls inside the
 * generated {@code main} method. If the assertion fails at runtime, the JCP-compiled
 * program throws, which surfaces as an {@link java.lang.reflect.InvocationTargetException}
 * and causes the test to fail.
 */
public class StringsCompileTest extends AbstractCompileTest {

    private StaticMethodCallExpression strings(String method, LiteralExpression... args) {
        return new StaticMethodCallExpression("Strings", method, args);
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

    private Method compileStatements(String baseName, StaticMethodCallExpression... calls) throws Exception {
        Block program = new BlockImpl();
        for (StaticMethodCallExpression c : calls) {
            program.addStatement(new ExpressionStatement(c));
        }
        String className = uniqueClassName(baseName);
        Class<?> clazz = compiler.compileAndLoad(program, className);
        return clazz.getMethod("main", String[].class);
    }

    // --- String-returning methods ---

    @Test
    void testUpper() throws Exception {
        Method main = compileStatements("TestStringsUpper",
            assertEq(str("HELLO"), strings("upper", str("hello"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testLower() throws Exception {
        Method main = compileStatements("TestStringsLower",
            assertEq(str("hello"), strings("lower", str("HELLO"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testTrim() throws Exception {
        Method main = compileStatements("TestStringsTrim",
            assertEq(str("hi"), strings("trim", str("  hi  "))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testConcat() throws Exception {
        Method main = compileStatements("TestStringsConcat",
            assertEq(str("foobar"), strings("concat", str("foo"), str("bar"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testReplace() throws Exception {
        Method main = compileStatements("TestStringsReplace",
            assertEq(str("a-b-c"), strings("replace", str("aXbXc"), str("X"), str("-"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testSub() throws Exception {
        Method main = compileStatements("TestStringsSub",
            assertEq(str("el"), strings("sub", str("hello"), int_(1), int_(3))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    // --- int-returning methods ---

    @Test
    void testLength() throws Exception {
        Method main = compileStatements("TestStringsLength",
            assertEq(int_(5), strings("length", str("hello"))),
            assertEq(int_(0), strings("length", str(""))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testIndexOf() throws Exception {
        Method main = compileStatements("TestStringsIndexOf",
            assertEq(int_(2), strings("indexOf", str("hello"), str("ll"))),
            assertEq(int_(-1), strings("indexOf", str("hello"), str("x"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    // --- boolean-returning methods ---

    @Test
    void testContains() throws Exception {
        Method main = compileStatements("TestStringsContains",
            assertTrue(strings("contains", str("hello"), str("ell"))),
            assertFalse(strings("contains", str("hello"), str("x"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testStartsWith() throws Exception {
        Method main = compileStatements("TestStringsStartsWith",
            assertTrue(strings("startsWith", str("hello"), str("he"))),
            assertFalse(strings("startsWith", str("hello"), str("lo"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testEndsWith() throws Exception {
        Method main = compileStatements("TestStringsEndsWith",
            assertTrue(strings("endsWith", str("hello"), str("lo"))),
            assertFalse(strings("endsWith", str("hello"), str("he"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testIsEmpty() throws Exception {
        Method main = compileStatements("TestStringsIsEmpty",
            assertTrue(strings("isEmpty", str(""))),
            assertFalse(strings("isEmpty", str("x"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    // --- String[] returning method ---

    @Test
    void testSplitCompiles() throws Exception {
        Method main = compileStatements("TestStringsSplit",
            strings("split", str("a,b,c"), str(",")));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }
}
