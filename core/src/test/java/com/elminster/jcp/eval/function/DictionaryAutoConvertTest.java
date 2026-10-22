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
import com.elminster.jcp.module.base.dict.Dictionary;
import com.elminster.jcp.util.ClassConverter;
import com.elminster.jcp.util.DataTypeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DictionaryAutoConvertTest {

    private EvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
        ClassConverter.registerClass(DictionaryAutoConvertFixture.class, context, "test");
    }

    @Test
    void testMapReturnAutoConvertsToDictionary() {
        Block block = new BlockImpl();

        // Dictionary scores = DictionaryAutoConvertFixture.getScores()
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("scores"),
                DataTypeUtils.getDataType("Dictionary", context),
                new FunctionCallExpression(
                        Identifier.fromName("DictionaryAutoConvertFixture.getScores"))
        ));

        // int size = scores.size()
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("size"),
                SystemDataType.INT,
                new MethodCallExpression(VariableExpression.of("scores"), "size")
        ));

        new EvalVisitor(context).visit(block);

        Data scores = context.getVariables().get("scores");
        assertTrue(scores.get() instanceof Dictionary);
        assertEquals("Dictionary", scores.getDataType().getName());

        Data size = context.getVariables().get("size");
        assertEquals(2, size.get());
    }

    @Test
    void testMapReturnWithNonStringKeys() {
        Block block = new BlockImpl();

        // Dictionary indexed = DictionaryAutoConvertFixture.getIndexedNames()
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("indexed"),
                DataTypeUtils.getDataType("Dictionary", context),
                new FunctionCallExpression(
                        Identifier.fromName("DictionaryAutoConvertFixture.getIndexedNames"))
        ));

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("size"),
                SystemDataType.INT,
                new MethodCallExpression(VariableExpression.of("indexed"), "size")
        ));

        new EvalVisitor(context).visit(block);

        Data indexed = context.getVariables().get("indexed");
        assertTrue(indexed.get() instanceof Dictionary);
        assertEquals(2, context.getVariables().get("size").get());
    }

    @Test
    void testDictionaryArgAutoConvertsToMap() {
        Block block = new BlockImpl();

        // Dictionary scores = DictionaryAutoConvertFixture.getScores()
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("scores"),
                DataTypeUtils.getDataType("Dictionary", context),
                new FunctionCallExpression(
                        Identifier.fromName("DictionaryAutoConvertFixture.getScores"))
        ));

        // int total = DictionaryAutoConvertFixture.sumValues(scores)
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("total"),
                SystemDataType.INT,
                new FunctionCallExpression(
                        Identifier.fromName("DictionaryAutoConvertFixture.sumValues"),
                        VariableExpression.of("scores"))
        ));

        new EvalVisitor(context).visit(block);

        Data total = context.getVariables().get("total");
        assertEquals(95 + 87, total.get());
    }

    @Test
    void testEmptyMapReturnConvertsToEmptyDictionary() {
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("empty"),
                DataTypeUtils.getDataType("Dictionary", context),
                new FunctionCallExpression(
                        Identifier.fromName("DictionaryAutoConvertFixture.emptyMap"))
        ));

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("size"),
                SystemDataType.INT,
                new MethodCallExpression(VariableExpression.of("empty"), "size")
        ));

        new EvalVisitor(context).visit(block);

        assertTrue(context.getVariables().get("empty").get() instanceof Dictionary);
        assertEquals(0, context.getVariables().get("size").get());
    }

    @Test
    void testNullMapReturnRemainsNull() {
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("result"),
                SystemDataType.ANY,
                new FunctionCallExpression(
                        Identifier.fromName("DictionaryAutoConvertFixture.nullMap"))
        ));

        new EvalVisitor(context).visit(block);

        Data result = context.getVariables().get("result");
        assertNull(result.get());
    }
}
