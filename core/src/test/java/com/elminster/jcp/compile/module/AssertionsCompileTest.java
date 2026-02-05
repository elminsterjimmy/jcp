package com.elminster.jcp.compile.module;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.compile.AbstractCompileTest;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Assertions module type in compile mode.
 * Verifies that Assertions.assertTrue() can be called from compiled JCP code.
 */
public class AssertionsCompileTest extends AbstractCompileTest {

    @Test
    void testAssertTrueWithTrue() throws Exception {
        // Assertions.assertTrue(true);
        Block program = new BlockImpl();

        StaticMethodCallExpression assertCall = new StaticMethodCallExpression(
            "Assertions",
            "assertTrue",
            LiteralExpression.of(BooleanLiteral.of(true))
        );
        program.addStatement(new ExpressionStatement(assertCall));

        // Compile and run
        String className = uniqueClassName("TestAssertTrueWithTrue");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method mainMethod = clazz.getMethod("main", String[].class);

        // Should execute without throwing
        assertDoesNotThrow(() -> mainMethod.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testAssertTrueWithFalse() throws Exception {
        // Assertions.assertTrue(false); - should throw
        Block program = new BlockImpl();

        StaticMethodCallExpression assertCall = new StaticMethodCallExpression(
            "Assertions",
            "assertTrue",
            LiteralExpression.of(BooleanLiteral.of(false))
        );
        program.addStatement(new ExpressionStatement(assertCall));

        // Compile and run
        String className = uniqueClassName("TestAssertTrueWithFalse");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method mainMethod = clazz.getMethod("main", String[].class);

        // Should throw AssertException
        InvocationTargetException ex = assertThrows(
            InvocationTargetException.class,
            () -> mainMethod.invoke(null, (Object) new String[]{})
        );
        assertTrue(ex.getCause().getMessage().contains("Assertions failed"));
    }

    @Test
    void testMultipleAssertions() throws Exception {
        // Assertions.assertTrue(true);
        // Assertions.assertTrue(true);
        Block program = new BlockImpl();

        program.addStatement(new ExpressionStatement(new StaticMethodCallExpression(
            "Assertions", "assertTrue", LiteralExpression.of(BooleanLiteral.of(true))
        )));
        program.addStatement(new ExpressionStatement(new StaticMethodCallExpression(
            "Assertions", "assertTrue", LiteralExpression.of(BooleanLiteral.of(true))
        )));

        // Compile and run
        String className = uniqueClassName("TestMultipleAssertions");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method mainMethod = clazz.getMethod("main", String[].class);

        // Should execute without throwing
        assertDoesNotThrow(() -> mainMethod.invoke(null, (Object) new String[]{}));
    }
}
