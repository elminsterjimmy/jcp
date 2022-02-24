package com.elminster.jcp.eval;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.BinaryExpressionImpl;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.operator.RelationalOperator;
import com.elminster.jcp.eval.base.ExpressionEvaluator;
import com.elminster.jcp.eval.context.EvalContextImpl;
import com.elminster.jcp.eval.data.BooleanData;
import com.elminster.jcp.eval.data.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class CompareEvaluatorTest {

    private static Stream<Arguments> getCompareEvaluatorAndExpected() {
        return Stream.of(
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(10)),
                                        RelationalOperator.GREATER_THAN
                                )),
                        true
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(20)),
                                        RelationalOperator.GREATER_THAN
                                )),
                        false
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(20)),
                                        RelationalOperator.GREATER_THAN_OR_EQUAL
                                )),
                        true
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(21)),
                                        RelationalOperator.GREATER_THAN_OR_EQUAL
                                )),
                        false
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(21)),
                                        RelationalOperator.LESS_THAN
                                )),
                        true
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(20)),
                                        RelationalOperator.LESS_THAN
                                )),
                        false
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(20)),
                                        RelationalOperator.LESS_THAN_OR_EQUAL
                                )),
                        true
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(19)),
                                        RelationalOperator.LESS_THAN_OR_EQUAL
                                )),
                        false
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(20)),
                                        RelationalOperator.EQUAL
                                )),
                        true
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(19)),
                                        RelationalOperator.EQUAL
                                )),
                        false
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(21)),
                                        RelationalOperator.EQUAL
                                )),
                        false
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(21)),
                                        RelationalOperator.NOT_EQUAL
                                )),
                        true
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(19)),
                                        RelationalOperator.NOT_EQUAL
                                )),
                        true
                ),
                Arguments.of(new ExpressionEvaluator(
                                new BinaryExpressionImpl(
                                        new LiteralExpression(IntLiteral.of(20)),
                                        new LiteralExpression(IntLiteral.of(20)),
                                        RelationalOperator.NOT_EQUAL
                                )),
                        false
                )
        );
    }

    @ParameterizedTest
    @MethodSource("getCompareEvaluatorAndExpected")
    void testCompareEvaluator_eval(ExpressionEvaluator compareEvaluator, Boolean expected) {
        Data eval = compareEvaluator.eval(new EvalContextImpl());
        Assertions.assertEquals(BooleanData.class, eval.getClass());
        Assertions.assertEquals(expected, eval.get());
    }
}