package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConvertTest {

    private EvalContext newContext() {
        return new RootEvalContext();
    }

    private Object eval(String method, SystemDataType returnType, LiteralExpression... args) {
        EvalContext context = newContext();
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", returnType,
            new FunctionCallExpression(Identifier.fromName("Convert." + method), args)
        ));
        new EvalVisitor(context).visit(program);
        return context.getVariable("result").get();
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

    // --- *ToString primitive converters ---

    @Test
    void testIntToString() {
        assertEquals("42", eval("intToString", SystemDataType.STRING, int_(42)));
        assertEquals("-7", eval("intToString", SystemDataType.STRING, int_(-7)));
    }

    @Test
    void testDoubleToString() {
        assertEquals("3.14", eval("doubleToString", SystemDataType.STRING, dbl(3.14)));
        assertEquals("0.0", eval("doubleToString", SystemDataType.STRING, dbl(0.0)));
    }

    @Test
    void testBooleanToString() {
        assertEquals("true", eval("booleanToString", SystemDataType.STRING, bool(true)));
        assertEquals("false", eval("booleanToString", SystemDataType.STRING, bool(false)));
    }

    // --- toInt ---

    @Test
    void testToInt() {
        assertEquals(42, eval("toInt", SystemDataType.INT, str("42")));
        assertEquals(-7, eval("toInt", SystemDataType.INT, str("-7")));
        assertEquals(0, eval("toInt", SystemDataType.INT, str("0")));
    }

    // --- toDouble ---

    @Test
    void testToDouble() {
        assertEquals(3.14, eval("toDouble", SystemDataType.DOUBLE, str("3.14")));
        assertEquals(-0.5, eval("toDouble", SystemDataType.DOUBLE, str("-0.5")));
    }

    // --- toBoolean ---

    @Test
    void testToBoolean() {
        assertEquals(true, eval("toBoolean", SystemDataType.BOOLEAN, str("true")));
        assertEquals(false, eval("toBoolean", SystemDataType.BOOLEAN, str("false")));
        // Boolean.parseBoolean is case-insensitive for "true"
        assertEquals(true, eval("toBoolean", SystemDataType.BOOLEAN, str("TRUE")));
        // Anything not "true" (any case) → false
        assertEquals(false, eval("toBoolean", SystemDataType.BOOLEAN, str("yes")));
    }
}
