package com.elminster.jcp.compile.module;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.compile.AbstractCompileTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for module function compilation (e.g., Assertions.assertTrue).
 */
public class ModuleFunctionCompileTest extends AbstractCompileTest {

    @Test
    void testAssertionsTrueSucceeds() throws Exception {
        // Assertions.assertTrue(true)
        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
                new FunctionCallExpression(
                        Identifier.fromName("Assertions.assertTrue"),
                        LiteralExpression.of(BooleanLiteral.of(true))
                )
        ));

        Class<?> clazz = compiler.compileAndLoad(program, uniqueClassName("TestAssertTrue"));
        Method main = clazz.getMethod("main", String[].class);

        // Should not throw exception
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testAssertionsFalseFails() throws Exception {
        // Assertions.assertTrue(false)
        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
                new FunctionCallExpression(
                        Identifier.fromName("Assertions.assertTrue"),
                        LiteralExpression.of(BooleanLiteral.of(false))
                )
        ));

        Class<?> clazz = compiler.compileAndLoad(program, uniqueClassName("TestAssertFalse"));
        Method main = clazz.getMethod("main", String[].class);

        // Should throw AssertException
        assertThrows(java.lang.reflect.InvocationTargetException.class, () ->
                main.invoke(null, (Object) new String[]{})
        );
    }

    @Test
    void testExplicitBaseModuleSyntax() throws Exception {
        // base::Assertions.assertTrue(true) - explicit module syntax
        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
                new FunctionCallExpression(
                        Identifier.fromName("base::Assertions.assertTrue"),
                        LiteralExpression.of(BooleanLiteral.of(true))
                )
        ));

        Class<?> clazz = compiler.compileAndLoad(program, uniqueClassName("TestExplicitModule"));
        Method main = clazz.getMethod("main", String[].class);

        // Should not throw exception
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testInvalidModuleFunctionNotFound() {
        // NonExistent.method() - should fall through to user-defined function lookup
        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
                new FunctionCallExpression(
                        Identifier.fromName("NonExistent.method"),
                        LiteralExpression.of(BooleanLiteral.of(true))
                )
        ));

        // Should throw CompileException for undefined function
        assertThrows(Exception.class, () ->
                compiler.compileAndLoad(program, uniqueClassName("TestNonExistent"))
        );
    }
}
