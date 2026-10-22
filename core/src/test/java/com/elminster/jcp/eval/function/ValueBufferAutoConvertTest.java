package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.module.base.vb.ValueBuffer;
import com.elminster.jcp.util.ClassConverter;
import com.elminster.jcp.util.DataTypeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValueBufferAutoConvertTest {

    private EvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
        ClassConverter.registerClass(ValueBufferAutoConvertFixture.class, context, "test");
    }

    @Test
    void testListReturnAutoConvertsToValueBuffer() {
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("names"),
                DataTypeUtils.getDataType("ValueBuffer", context),
                new FunctionCallExpression(
                        Identifier.fromName("ValueBufferAutoConvertFixture.getNames"))
        ));

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("len"),
                SystemDataType.INT,
                new MethodCallExpression(VariableExpression.of("names"), "length")
        ));

        new EvalVisitor(context).visit(block);

        Data names = context.getVariables().get("names");
        assertTrue(names.get() instanceof ValueBuffer);
        assertEquals("ValueBuffer", names.getDataType().getName());

        Data len = context.getVariables().get("len");
        assertEquals(3, len.get());
    }

    @Test
    void testListReturnWithIntegerElements() {
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("numbers"),
                DataTypeUtils.getDataType("ValueBuffer", context),
                new FunctionCallExpression(
                        Identifier.fromName("ValueBufferAutoConvertFixture.getNumbers"))
        ));

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("len"),
                SystemDataType.INT,
                new MethodCallExpression(VariableExpression.of("numbers"), "length")
        ));

        new EvalVisitor(context).visit(block);

        Data numbers = context.getVariables().get("numbers");
        assertTrue(numbers.get() instanceof ValueBuffer);
        assertEquals(3, context.getVariables().get("len").get());
    }

    @Test
    void testValueBufferArgAutoConvertsToList() {
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("numbers"),
                DataTypeUtils.getDataType("ValueBuffer", context),
                new FunctionCallExpression(
                        Identifier.fromName("ValueBufferAutoConvertFixture.getNumbers"))
        ));

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("total"),
                SystemDataType.INT,
                new FunctionCallExpression(
                        Identifier.fromName("ValueBufferAutoConvertFixture.sumList"),
                        VariableExpression.of("numbers"))
        ));

        new EvalVisitor(context).visit(block);

        Data total = context.getVariables().get("total");
        assertEquals(60, total.get());
    }

    @Test
    void testEmptyListReturnConvertsToEmptyValueBuffer() {
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("empty"),
                DataTypeUtils.getDataType("ValueBuffer", context),
                new FunctionCallExpression(
                        Identifier.fromName("ValueBufferAutoConvertFixture.emptyList"))
        ));

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("len"),
                SystemDataType.INT,
                new MethodCallExpression(VariableExpression.of("empty"), "length")
        ));

        new EvalVisitor(context).visit(block);

        assertTrue(context.getVariables().get("empty").get() instanceof ValueBuffer);
        assertEquals(0, context.getVariables().get("len").get());
    }

    @Test
    void testNullListReturnRemainsNull() {
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("result"),
                SystemDataType.ANY,
                new FunctionCallExpression(
                        Identifier.fromName("ValueBufferAutoConvertFixture.nullList"))
        ));

        new EvalVisitor(context).visit(block);

        Data result = context.getVariables().get("result");
        assertNull(result.get());
    }
}
