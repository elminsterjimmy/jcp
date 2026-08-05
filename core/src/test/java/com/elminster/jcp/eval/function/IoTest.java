package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.module.base.io.IO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@code IO} base-module STD class in eval mode.
 *
 * <p>print/println: captured via System.setOut; each overload verified.
 * readLine: fed via System.setIn + resetReaderForTest(); multi-line and EOF verified.
 */
public class IoTest {

    private PrintStream originalOut;
    private InputStream originalIn;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalIn = System.in;
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
        IO.resetReaderForTest();
    }

    private void run(Block program) {
        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(program);
    }

    private ExpressionStatement ioCall(String method, LiteralExpression... args) {
        return new ExpressionStatement(
            new FunctionCallExpression(Identifier.fromName("IO." + method), args));
    }

    private Object evalReadLine() {
        EvalContext context = new RootEvalContext();
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", SystemDataType.STRING,
            new FunctionCallExpression(Identifier.fromName("IO.readLine"))));
        new EvalVisitor(context).visit(program);
        return context.getVariable("result").get();
    }

    private LiteralExpression int_(int n)       { return LiteralExpression.of(Literal.of(n)); }
    private LiteralExpression dbl(double d)     { return LiteralExpression.of(Literal.of(d)); }
    private LiteralExpression bool(boolean b)   { return LiteralExpression.of(Literal.of(b)); }
    private LiteralExpression str(String s)     { return LiteralExpression.of(StringLiteral.of(s)); }

    // --- print: no trailing newline ---

    @Test
    void testPrintInt() {
        Block p = new BlockImpl();
        p.addStatement(ioCall("print", int_(42)));
        run(p);
        assertEquals("42", captured.toString());
    }

    @Test
    void testPrintDouble() {
        Block p = new BlockImpl();
        p.addStatement(ioCall("print", dbl(3.14)));
        run(p);
        assertEquals("3.14", captured.toString());
    }

    @Test
    void testPrintBoolean() {
        Block p = new BlockImpl();
        p.addStatement(ioCall("print", bool(true)));
        run(p);
        assertEquals("true", captured.toString());

        captured.reset();
        Block p2 = new BlockImpl();
        p2.addStatement(ioCall("print", bool(false)));
        run(p2);
        assertEquals("false", captured.toString());
    }

    @Test
    void testPrintString() {
        Block p = new BlockImpl();
        p.addStatement(ioCall("print", str("hello")));
        run(p);
        assertEquals("hello", captured.toString());
    }

    // --- println: trailing newline ---

    @Test
    void testPrintlnInt() {
        Block p = new BlockImpl();
        p.addStatement(ioCall("println", int_(7)));
        run(p);
        assertEquals("7" + System.lineSeparator(), captured.toString());
    }

    @Test
    void testPrintlnDouble() {
        Block p = new BlockImpl();
        p.addStatement(ioCall("println", dbl(1.5)));
        run(p);
        assertEquals("1.5" + System.lineSeparator(), captured.toString());
    }

    @Test
    void testPrintlnBoolean() {
        Block p = new BlockImpl();
        p.addStatement(ioCall("println", bool(false)));
        run(p);
        assertEquals("false" + System.lineSeparator(), captured.toString());
    }

    @Test
    void testPrintlnString() {
        Block p = new BlockImpl();
        p.addStatement(ioCall("println", str("world")));
        run(p);
        assertEquals("world" + System.lineSeparator(), captured.toString());
    }

    // --- readLine ---

    @Test
    void testReadLineSingleLine() {
        System.setIn(new ByteArrayInputStream("hello\n".getBytes()));
        IO.resetReaderForTest();
        assertEquals("hello", evalReadLine());
    }

    @Test
    void testReadLineMultiLine() {
        System.setIn(new ByteArrayInputStream("first\nsecond\n".getBytes()));
        IO.resetReaderForTest();

        EvalContext context = new RootEvalContext();
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "a", SystemDataType.STRING,
            new FunctionCallExpression(Identifier.fromName("IO.readLine"))));
        program.addStatement(new VariableDeclarationImpl(
            "b", SystemDataType.STRING,
            new FunctionCallExpression(Identifier.fromName("IO.readLine"))));
        new EvalVisitor(context).visit(program);

        assertEquals("first", context.getVariable("a").get());
        assertEquals("second", context.getVariable("b").get());
    }

    @Test
    void testReadLineEof() {
        System.setIn(new ByteArrayInputStream(new byte[0]));
        IO.resetReaderForTest();
        assertEquals("", evalReadLine());
    }
}
