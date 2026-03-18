package com.elminster.jcp.eval.function;

import com.elminster.common.util.AssertException;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.literal.DoubleLiteral;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.NullLiteral;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AssertionsTest {

    private EvalVisitor newVisitor() {
        EvalContext context = new RootEvalContext();
        return new EvalVisitor(context);
    }

    private ExpressionStatement call(String qualifiedName, LiteralExpression... args) {
        return ExpressionStatement.of(new FunctionCallExpression(
            Identifier.fromName(qualifiedName), args));
    }

    /**
     * Assertions.assertTrue(true);
     * Assertions.assertTrue(false); // throws
     */
    @Test
    public void testAssertTrue() {
        Block block = new BlockImpl();
        block.addStatement(call("Assertions.assertTrue", LiteralExpression.of(BooleanLiteral.of(true))));
        newVisitor().visit(block);

        Assertions.assertThrows(AssertException.class, () ->
            newVisitor().visit(call("Assertions.assertTrue", LiteralExpression.of(BooleanLiteral.of(false)))));
    }

    /**
     * Assertions.assertFalse(false); // passes
     * Assertions.assertFalse(true);  // throws
     */
    @Test
    public void testAssertFalse() {
        Block block = new BlockImpl();
        block.addStatement(call("Assertions.assertFalse", LiteralExpression.of(BooleanLiteral.of(false))));
        newVisitor().visit(block);

        Assertions.assertThrows(AssertException.class, () ->
            newVisitor().visit(call("Assertions.assertFalse", LiteralExpression.of(BooleanLiteral.of(true)))));
    }

    /**
     * Assertions.assertEquals(1, 1); // passes
     * Assertions.assertEquals(1, 2); // throws
     */
    @Test
    public void testAssertEqualsInt() {
        Block block = new BlockImpl();
        block.addStatement(call("Assertions.assertEquals",
            LiteralExpression.of(Literal.of(1)),
            LiteralExpression.of(Literal.of(1))));
        newVisitor().visit(block);

        Assertions.assertThrows(AssertException.class, () ->
            newVisitor().visit(call("Assertions.assertEquals",
                LiteralExpression.of(Literal.of(1)),
                LiteralExpression.of(Literal.of(2)))));
    }

    /**
     * Assertions.assertEquals(1.0, 1.0); // passes
     * Assertions.assertEquals(1.0, 2.0); // throws
     */
    @Test
    public void testAssertEqualsDouble() {
        Block block = new BlockImpl();
        block.addStatement(call("Assertions.assertEquals",
            LiteralExpression.of(DoubleLiteral.of(1.0)),
            LiteralExpression.of(DoubleLiteral.of(1.0))));
        newVisitor().visit(block);

        Assertions.assertThrows(AssertException.class, () ->
            newVisitor().visit(call("Assertions.assertEquals",
                LiteralExpression.of(DoubleLiteral.of(1.0)),
                LiteralExpression.of(DoubleLiteral.of(2.0)))));
    }

    /**
     * Assertions.assertEquals(true, true);  // passes
     * Assertions.assertEquals(true, false); // throws
     */
    @Test
    public void testAssertEqualsBoolean() {
        Block block = new BlockImpl();
        block.addStatement(call("Assertions.assertEquals",
            LiteralExpression.of(BooleanLiteral.of(true)),
            LiteralExpression.of(BooleanLiteral.of(true))));
        newVisitor().visit(block);

        Assertions.assertThrows(AssertException.class, () ->
            newVisitor().visit(call("Assertions.assertEquals",
                LiteralExpression.of(BooleanLiteral.of(true)),
                LiteralExpression.of(BooleanLiteral.of(false)))));
    }

    /**
     * Assertions.assertEquals("a", "a"); // passes
     * Assertions.assertEquals("a", "b"); // throws
     */
    @Test
    public void testAssertEqualsString() {
        Block block = new BlockImpl();
        block.addStatement(call("Assertions.assertEquals",
            LiteralExpression.of(StringLiteral.of("hello")),
            LiteralExpression.of(StringLiteral.of("hello"))));
        newVisitor().visit(block);

        Assertions.assertThrows(AssertException.class, () ->
            newVisitor().visit(call("Assertions.assertEquals",
                LiteralExpression.of(StringLiteral.of("hello")),
                LiteralExpression.of(StringLiteral.of("world")))));
    }

    /**
     * Assertions.assertNull(null);    // passes
     * Assertions.assertNull(nonNull); // throws
     */
    @Test
    public void testAssertNull() {
        Block block = new BlockImpl();
        block.addStatement(call("Assertions.assertNull", LiteralExpression.of(NullLiteral.INSTANCE)));
        newVisitor().visit(block);

        Assertions.assertThrows(AssertException.class, () ->
            newVisitor().visit(call("Assertions.assertNull",
                LiteralExpression.of(StringLiteral.of("not-null")))));
    }

    /**
     * Assertions.assertNotNull(nonNull); // passes
     * Assertions.assertNotNull(null);    // throws
     */
    @Test
    public void testAssertNotNull() {
        Block block = new BlockImpl();
        block.addStatement(call("Assertions.assertNotNull",
            LiteralExpression.of(StringLiteral.of("value"))));
        newVisitor().visit(block);

        Assertions.assertThrows(AssertException.class, () ->
            newVisitor().visit(call("Assertions.assertNotNull",
                LiteralExpression.of(NullLiteral.INSTANCE))));
    }
}
