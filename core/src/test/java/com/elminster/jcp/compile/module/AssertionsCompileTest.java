package com.elminster.jcp.compile.module;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.literal.DoubleLiteral;
import com.elminster.jcp.ast.expression.literal.NullLiteral;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.compile.AbstractCompileTest;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Assertions module in compile mode.
 */
public class AssertionsCompileTest extends AbstractCompileTest {

    private StaticMethodCallExpression call(String method, LiteralExpression... args) {
        return new StaticMethodCallExpression("Assertions", method, args);
    }

    private Method compileAndGetMain(String baseName, StaticMethodCallExpression... calls) throws Exception {
        Block program = new BlockImpl();
        for (StaticMethodCallExpression c : calls) {
            program.addStatement(new ExpressionStatement(c));
        }
        String className = uniqueClassName(baseName);
        Class<?> clazz = compiler.compileAndLoad(program, className);
        return clazz.getMethod("main", String[].class);
    }

    @Test
    void testAssertTrueWithTrue() throws Exception {
        Method main = compileAndGetMain("TestAssertTruePass",
            call("assertTrue", LiteralExpression.of(BooleanLiteral.of(true))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testAssertTrueWithFalse() throws Exception {
        Method main = compileAndGetMain("TestAssertTrueFail",
            call("assertTrue", LiteralExpression.of(BooleanLiteral.of(false))));
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
            () -> main.invoke(null, (Object) new String[]{}));
        assertTrue(ex.getCause().getMessage().contains("Assertions failed"));
    }

    @Test
    void testAssertFalseWithFalse() throws Exception {
        Method main = compileAndGetMain("TestAssertFalsePass",
            call("assertFalse", LiteralExpression.of(BooleanLiteral.of(false))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testAssertFalseWithTrue() throws Exception {
        Method main = compileAndGetMain("TestAssertFalseFail",
            call("assertFalse", LiteralExpression.of(BooleanLiteral.of(true))));
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
            () -> main.invoke(null, (Object) new String[]{}));
        assertTrue(ex.getCause().getMessage().contains("Assertions failed"));
    }

    @Test
    void testAssertEqualsIntPass() throws Exception {
        Method main = compileAndGetMain("TestAssertEqualsIntPass",
            call("assertEquals", LiteralExpression.of(42), LiteralExpression.of(42)));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testAssertEqualsIntFail() throws Exception {
        Method main = compileAndGetMain("TestAssertEqualsIntFail",
            call("assertEquals", LiteralExpression.of(1), LiteralExpression.of(2)));
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
            () -> main.invoke(null, (Object) new String[]{}));
        assertTrue(ex.getCause().getMessage().contains("Assertions failed"));
    }

    @Test
    void testAssertEqualsDoublePass() throws Exception {
        Method main = compileAndGetMain("TestAssertEqualsDoublePass",
            call("assertEquals",
                LiteralExpression.of(DoubleLiteral.of(1.0)),
                LiteralExpression.of(DoubleLiteral.of(1.0))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testAssertEqualsDoubleFail() throws Exception {
        Method main = compileAndGetMain("TestAssertEqualsDoubleFail",
            call("assertEquals",
                LiteralExpression.of(DoubleLiteral.of(1.0)),
                LiteralExpression.of(DoubleLiteral.of(2.0))));
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
            () -> main.invoke(null, (Object) new String[]{}));
        assertTrue(ex.getCause().getMessage().contains("Assertions failed"));
    }

    @Test
    void testAssertEqualsBooleanPass() throws Exception {
        Method main = compileAndGetMain("TestAssertEqualsBooleanPass",
            call("assertEquals",
                LiteralExpression.of(BooleanLiteral.of(true)),
                LiteralExpression.of(BooleanLiteral.of(true))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testAssertEqualsBooleanFail() throws Exception {
        Method main = compileAndGetMain("TestAssertEqualsBooleanFail",
            call("assertEquals",
                LiteralExpression.of(BooleanLiteral.of(true)),
                LiteralExpression.of(BooleanLiteral.of(false))));
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
            () -> main.invoke(null, (Object) new String[]{}));
        assertTrue(ex.getCause().getMessage().contains("Assertions failed"));
    }

    @Test
    void testAssertEqualsStringPass() throws Exception {
        Method main = compileAndGetMain("TestAssertEqualsStringPass",
            call("assertEquals",
                LiteralExpression.of(StringLiteral.of("hello")),
                LiteralExpression.of(StringLiteral.of("hello"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testAssertEqualsStringFail() throws Exception {
        Method main = compileAndGetMain("TestAssertEqualsStringFail",
            call("assertEquals",
                LiteralExpression.of(StringLiteral.of("hello")),
                LiteralExpression.of(StringLiteral.of("world"))));
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
            () -> main.invoke(null, (Object) new String[]{}));
        assertTrue(ex.getCause().getMessage().contains("Assertions failed"));
    }

    @Test
    void testAssertNullWithNull() throws Exception {
        Method main = compileAndGetMain("TestAssertNullPass",
            call("assertNull", LiteralExpression.of(NullLiteral.INSTANCE)));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testAssertNullWithNonNull() throws Exception {
        Method main = compileAndGetMain("TestAssertNullFail",
            call("assertNull", LiteralExpression.of(StringLiteral.of("not-null"))));
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
            () -> main.invoke(null, (Object) new String[]{}));
        assertTrue(ex.getCause().getMessage().contains("Assertions failed"));
    }

    @Test
    void testAssertNotNullWithNonNull() throws Exception {
        Method main = compileAndGetMain("TestAssertNotNullPass",
            call("assertNotNull", LiteralExpression.of(StringLiteral.of("value"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testAssertNotNullWithNull() throws Exception {
        Method main = compileAndGetMain("TestAssertNotNullFail",
            call("assertNotNull", LiteralExpression.of(NullLiteral.INSTANCE)));
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
            () -> main.invoke(null, (Object) new String[]{}));
        assertTrue(ex.getCause().getMessage().contains("Assertions failed"));
    }

    @Test
    void testMultipleAssertions() throws Exception {
        Method main = compileAndGetMain("TestMultipleAssertions",
            call("assertTrue", LiteralExpression.of(BooleanLiteral.of(true))),
            call("assertFalse", LiteralExpression.of(BooleanLiteral.of(false))),
            call("assertEquals", LiteralExpression.of(1), LiteralExpression.of(1)));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }
}
