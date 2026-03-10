package com.elminster.jcp.eval;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.NullLiteral;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LiteralEvaluator, focusing on NullLiteral handling.
 */
class LiteralEvaluatorTest {

    @Test
    void nullLiteral_evaluatesToNullValue() {
        EvalContext context = new RootEvalContext();
        LiteralExpression nullExpr = LiteralExpression.of(NullLiteral.INSTANCE);

        Evaluable evaluable = AstEvaluatorFactory.getEvaluator(nullExpr);
        Data result = evaluable.eval(context);

        assertNull(result.get());
    }
}
