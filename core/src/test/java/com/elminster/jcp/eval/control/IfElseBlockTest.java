package com.elminster.jcp.eval.control;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.operation.AssignmentExpression;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.operator.AssignmentOperator;
import com.elminster.jcp.ast.statement.control.IfElseStatement;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclaration;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class IfElseBlockTest {

    @BeforeAll
    public static void init() {
    }

    /**
     * boolean b
     * if (true) {
     *   b = true
     * } else {
     *   b = false
     * }
     */
    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    public void testIfElseBlock(boolean condition) {
        VariableDeclaration variableDeclaration = new VariableDeclarationImpl("b",
                DataType.SystemDataType.BOOLEAN);
        Expression conditionExpression = LiteralExpression.of(condition);
        Block ifBlock = new BlockImpl();
        ifBlock.addStatement(ExpressionStatement.of(new AssignmentExpression(
                Identifier.fromName("b"),
                AssignmentOperator.ASSIGNMENT,
                LiteralExpression.of(true))));
        Block elseBlock = new BlockImpl();
        elseBlock.addStatement(ExpressionStatement.of(new AssignmentExpression(
                Identifier.fromName("b"),
                AssignmentOperator.ASSIGNMENT,
                LiteralExpression.of(false))));

        IfElseStatement ifElseStatement = new IfElseStatement(
                ifBlock, elseBlock, conditionExpression);
        Block block = new BlockImpl();
        block.addStatement(variableDeclaration);
        block.addStatement(ifElseStatement);

        EvalContext context = new RootEvalContext();

        EvalVisitor visitor = new EvalVisitor(context);
        visitor.visit(block);
        Assertions.assertEquals(condition, context.getVariable("b").get());
    }
}