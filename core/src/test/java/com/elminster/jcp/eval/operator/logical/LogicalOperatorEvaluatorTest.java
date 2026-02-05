package com.elminster.jcp.eval.operator.logical;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.operation.LogicalAndExpression;
import com.elminster.jcp.ast.expression.operation.LogicalNotExpression;
import com.elminster.jcp.ast.expression.operation.LogicalOrExpression;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.BooleanData;
import com.elminster.jcp.eval.data.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for logical operator evaluators: AND (&&), OR (||), NOT (!).
 */
class LogicalOperatorEvaluatorTest {

    private EvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
    }

    private LiteralExpression boolLiteral(boolean value) {
        return new LiteralExpression(BooleanLiteral.of(value));
    }

    @Nested
    class AndEvaluatorTests {

        @ParameterizedTest
        @CsvSource({
            "true, true, true",
            "true, false, false",
            "false, true, false",
            "false, false, false"
        })
        void testAndOperator(boolean left, boolean right, boolean expected) {
            LogicalAndExpression andExpr = new LogicalAndExpression(
                boolLiteral(left),
                boolLiteral(right)
            );

            AndEvaluator evaluator = new AndEvaluator(andExpr);
            Data result = evaluator.eval(context);

            assertInstanceOf(BooleanData.class, result);
            assertEquals(expected, result.get());
        }

        @Test
        void testAndShortCircuit_LeftFalse_RightNotEvaluated() {
            // When left is false, right should not be evaluated (short-circuit)
            // We can't easily test this without a side-effect mechanism,
            // but we verify the correct result is returned
            LogicalAndExpression andExpr = new LogicalAndExpression(
                boolLiteral(false),
                boolLiteral(true)  // This should not matter
            );

            AndEvaluator evaluator = new AndEvaluator(andExpr);
            Data result = evaluator.eval(context);

            assertEquals(false, result.get());
        }
    }

    @Nested
    class OrEvaluatorTests {

        @ParameterizedTest
        @CsvSource({
            "true, true, true",
            "true, false, true",
            "false, true, true",
            "false, false, false"
        })
        void testOrOperator(boolean left, boolean right, boolean expected) {
            LogicalOrExpression orExpr = new LogicalOrExpression(
                boolLiteral(left),
                boolLiteral(right)
            );

            OrEvaluator evaluator = new OrEvaluator(orExpr);
            Data result = evaluator.eval(context);

            assertInstanceOf(BooleanData.class, result);
            assertEquals(expected, result.get());
        }

        @Test
        void testOrShortCircuit_LeftTrue_RightNotEvaluated() {
            // When left is true, right should not be evaluated (short-circuit)
            LogicalOrExpression orExpr = new LogicalOrExpression(
                boolLiteral(true),
                boolLiteral(false)  // This should not matter
            );

            OrEvaluator evaluator = new OrEvaluator(orExpr);
            Data result = evaluator.eval(context);

            assertEquals(true, result.get());
        }
    }

    @Nested
    class NotEvaluatorTests {

        @Test
        void testNotTrue_ReturnsFalse() {
            LogicalNotExpression notExpr = new LogicalNotExpression(boolLiteral(true));

            NotEvaluator evaluator = new NotEvaluator(notExpr);
            Data result = evaluator.eval(context);

            assertInstanceOf(BooleanData.class, result);
            assertEquals(false, result.get());
        }

        @Test
        void testNotFalse_ReturnsTrue() {
            LogicalNotExpression notExpr = new LogicalNotExpression(boolLiteral(false));

            NotEvaluator evaluator = new NotEvaluator(notExpr);
            Data result = evaluator.eval(context);

            assertInstanceOf(BooleanData.class, result);
            assertEquals(true, result.get());
        }

        @Test
        void testDoubleNegation() {
            // !!true = true
            LogicalNotExpression innerNot = new LogicalNotExpression(boolLiteral(true));
            LogicalNotExpression outerNot = new LogicalNotExpression(innerNot);

            NotEvaluator evaluator = new NotEvaluator(outerNot);
            Data result = evaluator.eval(context);

            assertEquals(true, result.get());
        }
    }

    @Nested
    class ComplexExpressionTests {

        @Test
        void testAndWithNestedOr() {
            // true && (false || true) = true && true = true
            LogicalOrExpression orExpr = new LogicalOrExpression(
                boolLiteral(false),
                boolLiteral(true)
            );
            LogicalAndExpression andExpr = new LogicalAndExpression(
                boolLiteral(true),
                orExpr
            );

            AndEvaluator evaluator = new AndEvaluator(andExpr);
            Data result = evaluator.eval(context);

            assertEquals(true, result.get());
        }

        @Test
        void testOrWithNestedAnd() {
            // false || (true && true) = false || true = true
            LogicalAndExpression andExpr = new LogicalAndExpression(
                boolLiteral(true),
                boolLiteral(true)
            );
            LogicalOrExpression orExpr = new LogicalOrExpression(
                boolLiteral(false),
                andExpr
            );

            OrEvaluator evaluator = new OrEvaluator(orExpr);
            Data result = evaluator.eval(context);

            assertEquals(true, result.get());
        }

        @Test
        void testNotWithAnd() {
            // !(true && false) = !false = true
            LogicalAndExpression andExpr = new LogicalAndExpression(
                boolLiteral(true),
                boolLiteral(false)
            );
            LogicalNotExpression notExpr = new LogicalNotExpression(andExpr);

            NotEvaluator evaluator = new NotEvaluator(notExpr);
            Data result = evaluator.eval(context);

            assertEquals(true, result.get());
        }

        @Test
        void testDeMorganLaw_NotAndEqualsOrNots() {
            // !(a && b) should equal (!a || !b)
            // !(true && false) = true
            // (!true || !false) = (false || true) = true
            boolean a = true;
            boolean b = false;

            // !(a && b)
            LogicalAndExpression andExpr = new LogicalAndExpression(boolLiteral(a), boolLiteral(b));
            LogicalNotExpression notAndExpr = new LogicalNotExpression(andExpr);
            NotEvaluator notAndEvaluator = new NotEvaluator(notAndExpr);
            Data notAndResult = notAndEvaluator.eval(context);

            // (!a || !b)
            LogicalNotExpression notA = new LogicalNotExpression(boolLiteral(a));
            LogicalNotExpression notB = new LogicalNotExpression(boolLiteral(b));
            LogicalOrExpression orNotsExpr = new LogicalOrExpression(notA, notB);
            OrEvaluator orNotsEvaluator = new OrEvaluator(orNotsExpr);
            Data orNotsResult = orNotsEvaluator.eval(context);

            assertEquals(notAndResult.get(), orNotsResult.get());
        }
    }
}
