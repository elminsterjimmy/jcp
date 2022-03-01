package com.elminster.jcp.eval.operator.assignment;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.*;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.expression.operator.AssignmentOperator;
import com.elminster.jcp.ast.statement.*;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.EvalContextImpl;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.test.LogFunction;
import org.junit.jupiter.api.Test;

class AssignmentEvaluatorTest {

    /**
     * int i = 10;
     * i += 20;
     * log("" + i);
     */
    @Test
    public void testAssignmentEvaluator() {
        Block block = new BlockImpl();

        VariableDeclaration variableDeclaration = new VariableDeclarationImpl(Identifier.fromName("i"),
                DataType.SystemDataType.INT, new LiteralExpression(IntLiteral.of(10)));

        AssignmentExpression assignmentExpression = new AssignmentExpression(Identifier.fromName("i"), AssignmentOperator.PLUS_ASSIGNMENT,
                new LiteralExpression(IntLiteral.of(20)));

        FunctionCallExpression logCall = new FunctionCallExpression(new IdentifierExpression("log"),
                new Plus(new LiteralExpression(StringLiteral.of("")), new VariableExpression(Identifier.fromName("i"))));

        block.addStatement(variableDeclaration)
                .addStatement(new ExpressionStatement(assignmentExpression))
                .addStatement(new ExpressionStatement(logCall));

        EvalContext context = new EvalContextImpl();
        context.addFunction(new LogFunction());

        EvalVisitor visitor = new EvalVisitor(context);
        visitor.visit(block);
    }

}