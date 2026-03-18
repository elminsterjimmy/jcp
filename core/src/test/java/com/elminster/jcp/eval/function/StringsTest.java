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

public class StringsTest {

    private EvalContext newContext() {
        return new RootEvalContext();
    }

    private Object eval(String method, SystemDataType returnType, LiteralExpression... args) {
        EvalContext context = newContext();
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", returnType,
            new FunctionCallExpression(Identifier.fromName("Strings." + method), args)
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

    @Test
    void testLength() {
        assertEquals(5, eval("length", SystemDataType.INT, str("hello")));
        assertEquals(0, eval("length", SystemDataType.INT, str("")));
    }

    @Test
    void testSub() {
        assertEquals("el", eval("sub", SystemDataType.STRING, str("hello"), int_(1), int_(3)));
    }

    @Test
    void testConcat() {
        assertEquals("foobar", eval("concat", SystemDataType.STRING, str("foo"), str("bar")));
    }

    @Test
    void testIndexOf() {
        assertEquals(2, eval("indexOf", SystemDataType.INT, str("hello"), str("ll")));
        assertEquals(-1, eval("indexOf", SystemDataType.INT, str("hello"), str("x")));
    }

    @Test
    void testContains() {
        assertEquals(true, eval("contains", SystemDataType.BOOLEAN, str("hello"), str("ell")));
        assertEquals(false, eval("contains", SystemDataType.BOOLEAN, str("hello"), str("x")));
    }

    @Test
    void testUpper() {
        assertEquals("HELLO", eval("upper", SystemDataType.STRING, str("hello")));
    }

    @Test
    void testLower() {
        assertEquals("hello", eval("lower", SystemDataType.STRING, str("HELLO")));
    }

    @Test
    void testTrim() {
        assertEquals("hi", eval("trim", SystemDataType.STRING, str("  hi  ")));
    }

    @Test
    void testReplace() {
        assertEquals("a-b-c", eval("replace", SystemDataType.STRING, str("aXbXc"), str("X"), str("-")));
    }

    @Test
    void testStartsWith() {
        assertEquals(true, eval("startsWith", SystemDataType.BOOLEAN, str("hello"), str("he")));
        assertEquals(false, eval("startsWith", SystemDataType.BOOLEAN, str("hello"), str("lo")));
    }

    @Test
    void testEndsWith() {
        assertEquals(true, eval("endsWith", SystemDataType.BOOLEAN, str("hello"), str("lo")));
        assertEquals(false, eval("endsWith", SystemDataType.BOOLEAN, str("hello"), str("he")));
    }

    @Test
    void testIsEmpty() {
        assertEquals(true, eval("isEmpty", SystemDataType.BOOLEAN, str("")));
        assertEquals(false, eval("isEmpty", SystemDataType.BOOLEAN, str("x")));
    }

    @Test
    void testSplit() {
        String[] result = (String[]) eval("split", SystemDataType.STRING_ARRAY, str("a,b,c"), str(","));
        assertEquals(3, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
    }
}
