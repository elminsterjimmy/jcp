package com.elminster.jcp.eval;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.exception.JcpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvalVisitorTest {

    private EvalContext context;
    private EvalVisitor visitor;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
        visitor = new EvalVisitor(context);
    }

    @Test
    void getContext_ReturnsContext() {
        assertSame(context, visitor.getContext());
    }

    @Test
    void visit_ValidNode_Succeeds() {
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "x",
            SystemDataType.INT,
            LiteralExpression.of(42)
        ));

        visitor.visit(program);
        assertEquals(42, context.getVariable("x").get());
    }

    @Test
    void visit_UndefinedVariable_ThrowsJcpExceptionWithCallStack() {
        Block program = new BlockImpl();
        // Try to read undefined variable - throws VariableNotFoundException which is a JcpException
        program.addStatement(new VariableDeclarationImpl(
            "result",
            SystemDataType.INT,
            new VariableExpression(Identifier.fromName("undefinedVar"))
        ));

        JcpException ex = assertThrows(JcpException.class, () -> visitor.visit(program));
        assertNotNull(ex.getCallStack());
    }
}
