package com.elminster.jcp.compile.module;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.module.base.io.IO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@code IO} base-module STD class in compile mode.
 *
 * <p>Void print/println: System.out captured around the reflective main invocation
 * and asserted (Logger pattern + output capture).
 * Non-void readLine: in-DSL {@code Assertions.assertEquals} compiled into main,
 * then {@code assertDoesNotThrow} (Math pattern).
 */
public class IoCompileTest extends AbstractCompileTest {

    private PrintStream originalOut;
    private InputStream originalIn;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalIn = System.in;
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
        IO.resetReaderForTest();
    }

    private StaticMethodCallExpression io(String method, Expression... args) {
        return new StaticMethodCallExpression("IO", method, args);
    }

    private StaticMethodCallExpression assertEq(Expression expected, Expression actual) {
        return new StaticMethodCallExpression("Assertions", "assertEquals", expected, actual);
    }

    private LiteralExpression int_(int n)     { return LiteralExpression.of(Literal.of(n)); }
    private LiteralExpression dbl(double d)   { return LiteralExpression.of(Literal.of(d)); }
    private LiteralExpression bool(boolean b) { return LiteralExpression.of(Literal.of(b)); }
    private LiteralExpression str(String s)   { return LiteralExpression.of(StringLiteral.of(s)); }

    private String runCapturingOutput(String baseName, StaticMethodCallExpression... calls) throws Exception {
        Block program = new BlockImpl();
        for (StaticMethodCallExpression c : calls) {
            program.addStatement(new ExpressionStatement(c));
        }
        String className = uniqueClassName(baseName);
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method main = clazz.getMethod("main", String[].class);

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        try {
            assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
        } finally {
            System.setOut(originalOut);
        }
        return captured.toString();
    }

    // --- print: no trailing newline ---

    @Test
    void testPrintInt() throws Exception {
        String out = runCapturingOutput("TestIoPrintInt", io("print", int_(42)));
        assertEquals("42", out);
    }

    @Test
    void testPrintDouble() throws Exception {
        String out = runCapturingOutput("TestIoPrintDouble", io("print", dbl(3.14)));
        assertEquals("3.14", out);
    }

    @Test
    void testPrintBoolean() throws Exception {
        String out = runCapturingOutput("TestIoPrintBoolean", io("print", bool(true)));
        assertEquals("true", out);
    }

    @Test
    void testPrintString() throws Exception {
        String out = runCapturingOutput("TestIoPrintString", io("print", str("hello")));
        assertEquals("hello", out);
    }

    // --- println: trailing newline ---

    @Test
    void testPrintlnInt() throws Exception {
        String out = runCapturingOutput("TestIoPrintlnInt", io("println", int_(7)));
        assertEquals("7" + System.lineSeparator(), out);
    }

    @Test
    void testPrintlnDouble() throws Exception {
        String out = runCapturingOutput("TestIoPrintlnDouble", io("println", dbl(1.5)));
        assertEquals("1.5" + System.lineSeparator(), out);
    }

    @Test
    void testPrintlnBoolean() throws Exception {
        String out = runCapturingOutput("TestIoPrintlnBoolean", io("println", bool(false)));
        assertEquals("false" + System.lineSeparator(), out);
    }

    @Test
    void testPrintlnString() throws Exception {
        String out = runCapturingOutput("TestIoPrintlnString", io("println", str("world")));
        assertEquals("world" + System.lineSeparator(), out);
    }

    // --- readLine: in-DSL Assertions.assertEquals (Math pattern) ---

    @Test
    void testReadLineSingleLine() throws Exception {
        System.setIn(new ByteArrayInputStream("hello\n".getBytes()));
        IO.resetReaderForTest();

        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
            assertEq(str("hello"), io("readLine"))));
        String className = uniqueClassName("TestIoReadLine");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method main = clazz.getMethod("main", String[].class);
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testReadLineMultiLine() throws Exception {
        System.setIn(new ByteArrayInputStream("first\nsecond\n".getBytes()));
        IO.resetReaderForTest();

        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(assertEq(str("first"), io("readLine"))));
        program.addStatement(new ExpressionStatement(assertEq(str("second"), io("readLine"))));
        String className = uniqueClassName("TestIoReadLineMulti");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method main = clazz.getMethod("main", String[].class);
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testReadLineEof() throws Exception {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        IO.resetReaderForTest();

        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
            assertEq(str(""), io("readLine"))));
        String className = uniqueClassName("TestIoReadLineEof");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method main = clazz.getMethod("main", String[].class);
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }
}
