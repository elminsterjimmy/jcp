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
import com.elminster.jcp.module.base.io.Stdio;
import com.elminster.jcp.test.util.TeeOutputStream;
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
 * Tests for the {@code Stdio} base-module STD class in compile mode.
 *
 * <p>Void print/println: tee-captured (output goes to both capture buffer and real stdout);
 * capture buffer reset immediately before invoking main so log noise is excluded.
 * Non-void readLine: in-DSL {@code Assertions.assertEquals} compiled into main,
 * then {@code assertDoesNotThrow} (Math pattern).
 */
public class IoCompileTest extends AbstractCompileTest {

    private PrintStream originalOut;
    private InputStream originalIn;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalIn = System.in;
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(new TeeOutputStream(originalOut, captured)));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
        Stdio.resetReaderForTest();
    }

    private StaticMethodCallExpression stdio(String method, Expression... args) {
        return new StaticMethodCallExpression("Stdio", method, args);
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

        captured.reset();
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
        return captured.toString();
    }

    // --- print: no trailing newline ---

    @Test
    void testPrintInt() throws Exception {
        String out = runCapturingOutput("TestStdioPrintInt", stdio("print", int_(42)));
        assertEquals("42", out);
    }

    @Test
    void testPrintDouble() throws Exception {
        String out = runCapturingOutput("TestStdioPrintDouble", stdio("print", dbl(3.14)));
        assertEquals("3.14", out);
    }

    @Test
    void testPrintBoolean() throws Exception {
        String out = runCapturingOutput("TestStdioPrintBoolean", stdio("print", bool(true)));
        assertEquals("true", out);
    }

    @Test
    void testPrintString() throws Exception {
        String out = runCapturingOutput("TestStdioPrintString", stdio("print", str("hello")));
        assertEquals("hello", out);
    }

    // --- println: trailing newline ---

    @Test
    void testPrintlnInt() throws Exception {
        String out = runCapturingOutput("TestStdioPrintlnInt", stdio("println", int_(7)));
        assertEquals("7" + System.lineSeparator(), out);
    }

    @Test
    void testPrintlnDouble() throws Exception {
        String out = runCapturingOutput("TestStdioPrintlnDouble", stdio("println", dbl(1.5)));
        assertEquals("1.5" + System.lineSeparator(), out);
    }

    @Test
    void testPrintlnBoolean() throws Exception {
        String out = runCapturingOutput("TestStdioPrintlnBoolean", stdio("println", bool(false)));
        assertEquals("false" + System.lineSeparator(), out);
    }

    @Test
    void testPrintlnString() throws Exception {
        String out = runCapturingOutput("TestStdioPrintlnString", stdio("println", str("world")));
        assertEquals("world" + System.lineSeparator(), out);
    }

    @Test
    void testPrintlnNoArg() throws Exception {
        String out = runCapturingOutput("TestStdioPrintlnNoArg", stdio("println"));
        assertEquals(System.lineSeparator(), out);
    }

    // --- readLine: in-DSL Assertions.assertEquals (Math pattern) ---

    @Test
    void testReadLineSingleLine() throws Exception {
        System.setIn(new ByteArrayInputStream("hello\n".getBytes()));
        Stdio.resetReaderForTest();

        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
            assertEq(str("hello"), stdio("readLine"))));
        String className = uniqueClassName("TestStdioReadLine");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method main = clazz.getMethod("main", String[].class);
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testReadLineMultiLine() throws Exception {
        System.setIn(new ByteArrayInputStream("first\nsecond\n".getBytes()));
        Stdio.resetReaderForTest();

        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(assertEq(str("first"), stdio("readLine"))));
        program.addStatement(new ExpressionStatement(assertEq(str("second"), stdio("readLine"))));
        String className = uniqueClassName("TestStdioReadLineMulti");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method main = clazz.getMethod("main", String[].class);
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testReadLineEof() throws Exception {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        Stdio.resetReaderForTest();

        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
            assertEq(str(""), stdio("readLine"))));
        String className = uniqueClassName("TestStdioReadLineEof");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method main = clazz.getMethod("main", String[].class);
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }
}
