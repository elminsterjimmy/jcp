package com.elminster.jcp.compile.module;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.compile.AbstractCompileTest;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Logger module type in compile mode.
 * Verifies that Logger.log() can be called from compiled JCP code.
 */
public class LoggerCompileTest extends AbstractCompileTest {

    @Test
    void testLoggerLogString() throws Exception {
        // Logger.log("Hello, World!");
        Block program = new BlockImpl();

        StaticMethodCallExpression logCall = new StaticMethodCallExpression(
            "Logger",
            "log",
            LiteralExpression.of(StringLiteral.of("Hello from compile mode!"))
        );
        program.addStatement(new ExpressionStatement(logCall));

        // Compile and run
        String className = uniqueClassName("TestLoggerLogString");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method mainMethod = clazz.getMethod("main", String[].class);

        // Should execute without throwing
        assertDoesNotThrow(() -> mainMethod.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testLoggerLogInt() throws Exception {
        // Logger.log(42);
        Block program = new BlockImpl();

        StaticMethodCallExpression logCall = new StaticMethodCallExpression(
            "Logger",
            "log",
            LiteralExpression.of(IntLiteral.of(42))
        );
        program.addStatement(new ExpressionStatement(logCall));

        // Compile and run
        String className = uniqueClassName("TestLoggerLogInt");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method mainMethod = clazz.getMethod("main", String[].class);

        // Should execute without throwing
        assertDoesNotThrow(() -> mainMethod.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testLoggerMultipleCalls() throws Exception {
        // Logger.log("First");
        // Logger.log("Second");
        // Logger.log(123);
        Block program = new BlockImpl();

        program.addStatement(new ExpressionStatement(new StaticMethodCallExpression(
            "Logger", "log", LiteralExpression.of(StringLiteral.of("First"))
        )));
        program.addStatement(new ExpressionStatement(new StaticMethodCallExpression(
            "Logger", "log", LiteralExpression.of(StringLiteral.of("Second"))
        )));
        program.addStatement(new ExpressionStatement(new StaticMethodCallExpression(
            "Logger", "log", LiteralExpression.of(IntLiteral.of(123))
        )));

        // Compile and run
        String className = uniqueClassName("TestLoggerMultipleCalls");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method mainMethod = clazz.getMethod("main", String[].class);

        // Should execute without throwing
        assertDoesNotThrow(() -> mainMethod.invoke(null, (Object) new String[]{}));
    }
}
