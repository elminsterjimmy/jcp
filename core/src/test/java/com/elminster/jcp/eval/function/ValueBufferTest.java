package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.module.base.ValueBuffer;
import com.elminster.jcp.util.DataTypeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ValueBufferTest {


    /**
     * ValueBuffer vb = ValueBuffer.new()
     */
    @Test
    public void testValueBufferNew() {
        EvalContext context = new RootEvalContext();
        Block block = new BlockImpl();
        String functionName = "ValueBuffer.new";

        block.addStatement(new VariableDeclarationImpl(
                        Identifier.fromName("vb"),
                        DataTypeUtils.getDataType("ValueBuffer", context),
                        new FunctionCallExpression(Identifier.fromName(functionName))
                )
        );
        EvalVisitor visitor = new EvalVisitor(context);
        visitor.visit(block);

        Data vb = context.getVariables().get("vb");
        Assertions.assertEquals("ValueBuffer", vb.getDataType().getName());
        Assertions.assertTrue(vb.get() instanceof ValueBuffer);
    }


    /**
     * ValueBuffer vb = ValueBuffer.new()
     * vb.setHeader(new String[]{"id", "name"})
     * String[] header = vb.getHeader()
     */
    @Test
    public void testSetGetHeader() {
        EvalContext context = new RootEvalContext();
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                        Identifier.fromName("vb"),
                        DataTypeUtils.getDataTypeAndCreateOnMissing("ValueBuffer", context),
                        new FunctionCallExpression(Identifier.fromName("ValueBuffer.new"))
                ));

        block.addStatement(ExpressionStatement.of(
                new MethodCallExpression(VariableExpression.of("vb"),
                        "setHeader",
                        LiteralExpression.of(Literal.of(new String[]{"id", "name"}))))
        );

        block.addStatement(
                new VariableDeclarationImpl(
                        Identifier.fromName("header"),
                        DataTypeUtils.getDataType("String[]", context),
                        new MethodCallExpression(VariableExpression.of("vb"),
                                "getHeader")
                )
        );
        EvalVisitor visitor = new EvalVisitor(context);
        visitor.visit(block);

        Data header = context.getVariables().get("header");
        Assertions.assertEquals("String[]", header.getDataType().getName());
        Assertions.assertArrayEquals(new String[]{"id", "name"}, (String[]) header.get());
    }
}
