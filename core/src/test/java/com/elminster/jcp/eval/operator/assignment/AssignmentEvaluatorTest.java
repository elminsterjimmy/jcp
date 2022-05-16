package com.elminster.jcp.eval.operator.assignment;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.expression.operation.AssignmentExpression;
import com.elminster.jcp.ast.expression.operation.Equal;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.expression.operation.operator.AssignmentOperator;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclaration;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class AssignmentEvaluatorTest {

    private static Stream<Arguments> assignments() {
        return Stream.of(
                Arguments.of(AssignmentOperator.ASSIGNMENT, 2),
                Arguments.of(AssignmentOperator.PLUS_ASSIGNMENT, 12),
                Arguments.of(AssignmentOperator.MINUS_ASSIGNMENT, 8),
                Arguments.of(AssignmentOperator.MULTI_ASSIGNMENT, 20),
                Arguments.of(AssignmentOperator.DIVIDE_ASSIGNMENT, 5),
                Arguments.of(AssignmentOperator.MOD_ASSIGNMENT, 0)
        );
    }

    /**
     * int i = 10;
     * i op 2;
     * Assertions.assertTrue(expected, i);
     */
    @MethodSource("assignments")
    @ParameterizedTest
    public void testAssignmentEvaluator(AssignmentOperator operator, int expected) {
        Block block = new BlockImpl();

        VariableDeclaration variableDeclaration = new VariableDeclarationImpl(Identifier.fromName("i"),
                DataType.SystemDataType.INT, new LiteralExpression(IntLiteral.of(10)));

        AssignmentExpression assignmentExpression = new AssignmentExpression(Identifier.fromName("i"),
                operator,
                new LiteralExpression(IntLiteral.of(2)));

        FunctionCallExpression logCall = new FunctionCallExpression(new IdentifierExpression("Assertions.assertTrue"),
                new Equal(LiteralExpression.of(expected), VariableExpression.of("i")));

        block.addStatement(variableDeclaration)
                .addStatement(new ExpressionStatement(assignmentExpression))
                .addStatement(new ExpressionStatement(logCall));

        EvalContext context = new RootEvalContext();

        EvalVisitor visitor = new EvalVisitor(context);
        visitor.visit(block);
    }
}