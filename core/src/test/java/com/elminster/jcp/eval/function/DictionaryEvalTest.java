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
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.module.base.dict.Dictionary;
import com.elminster.jcp.module.base.vb.ValueBuffer;
import com.elminster.jcp.util.DataTypeUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DictionaryEvalTest {

    @Test
    void testDictionaryNew() {
        EvalContext context = new RootEvalContext();
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("dict"),
                DataTypeUtils.getDataType("Dictionary", context),
                new FunctionCallExpression(Identifier.fromName("Dictionary.new"))
        ));

        new EvalVisitor(context).visit(block);

        Data dict = context.getVariables().get("dict");
        assertEquals("Dictionary", dict.getDataType().getName());
        assertTrue(dict.get() instanceof Dictionary);
    }

    @Test
    void testPutAndGet() {
        EvalContext context = new RootEvalContext();
        Block block = new BlockImpl();

        // Dictionary dict = Dictionary.new()
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("dict"),
                DataTypeUtils.getDataType("Dictionary", context),
                new FunctionCallExpression(Identifier.fromName("Dictionary.new"))
        ));

        // dict.put("name", "Alice")
        block.addStatement(ExpressionStatement.of(
                new MethodCallExpression(VariableExpression.of("dict"),
                        "put",
                        LiteralExpression.of(Literal.of("name")),
                        LiteralExpression.of(Literal.of("Alice")))
        ));

        // Any result = dict.get("name")
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("result"),
                SystemDataType.ANY,
                new MethodCallExpression(VariableExpression.of("dict"),
                        "get",
                        LiteralExpression.of(Literal.of("name")))
        ));

        new EvalVisitor(context).visit(block);

        Data result = context.getVariables().get("result");
        assertEquals("Alice", result.get());
    }

    @Test
    void testRemoveAndSize() {
        EvalContext context = new RootEvalContext();
        Block block = new BlockImpl();

        // Dictionary dict = Dictionary.new()
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("dict"),
                DataTypeUtils.getDataType("Dictionary", context),
                new FunctionCallExpression(Identifier.fromName("Dictionary.new"))
        ));

        // dict.put("a", 1)
        block.addStatement(ExpressionStatement.of(
                new MethodCallExpression(VariableExpression.of("dict"),
                        "put",
                        LiteralExpression.of(Literal.of("a")),
                        LiteralExpression.of(Literal.of(1)))
        ));

        // dict.put("b", 2)
        block.addStatement(ExpressionStatement.of(
                new MethodCallExpression(VariableExpression.of("dict"),
                        "put",
                        LiteralExpression.of(Literal.of("b")),
                        LiteralExpression.of(Literal.of(2)))
        ));

        // int sizeBefore = dict.size()
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("sizeBefore"),
                SystemDataType.INT,
                new MethodCallExpression(VariableExpression.of("dict"), "size")
        ));

        // dict.remove("a")
        block.addStatement(ExpressionStatement.of(
                new MethodCallExpression(VariableExpression.of("dict"),
                        "remove",
                        LiteralExpression.of(Literal.of("a")))
        ));

        // int sizeAfter = dict.size()
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("sizeAfter"),
                SystemDataType.INT,
                new MethodCallExpression(VariableExpression.of("dict"), "size")
        ));

        new EvalVisitor(context).visit(block);

        assertEquals(2, context.getVariables().get("sizeBefore").get());
        assertEquals(1, context.getVariables().get("sizeAfter").get());
    }

    @Test
    void testContainsKey() {
        EvalContext context = new RootEvalContext();
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("dict"),
                DataTypeUtils.getDataType("Dictionary", context),
                new FunctionCallExpression(Identifier.fromName("Dictionary.new"))
        ));

        block.addStatement(ExpressionStatement.of(
                new MethodCallExpression(VariableExpression.of("dict"),
                        "put",
                        LiteralExpression.of(Literal.of("key1")),
                        LiteralExpression.of(Literal.of("val1")))
        ));

        // boolean hasKey1 = dict.containsKey("key1")
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("hasKey1"),
                SystemDataType.BOOLEAN,
                new MethodCallExpression(VariableExpression.of("dict"),
                        "containsKey",
                        LiteralExpression.of(Literal.of("key1")))
        ));

        // boolean hasKey2 = dict.containsKey("key2")
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("hasKey2"),
                SystemDataType.BOOLEAN,
                new MethodCallExpression(VariableExpression.of("dict"),
                        "containsKey",
                        LiteralExpression.of(Literal.of("key2")))
        ));

        new EvalVisitor(context).visit(block);

        assertEquals(true, context.getVariables().get("hasKey1").get());
        assertEquals(false, context.getVariables().get("hasKey2").get());
    }

    @Test
    void testKeysReturnsValueBuffer() {
        EvalContext context = new RootEvalContext();
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("dict"),
                DataTypeUtils.getDataType("Dictionary", context),
                new FunctionCallExpression(Identifier.fromName("Dictionary.new"))
        ));

        block.addStatement(ExpressionStatement.of(
                new MethodCallExpression(VariableExpression.of("dict"),
                        "put",
                        LiteralExpression.of(Literal.of("x")),
                        LiteralExpression.of(Literal.of(10)))
        ));

        block.addStatement(ExpressionStatement.of(
                new MethodCallExpression(VariableExpression.of("dict"),
                        "put",
                        LiteralExpression.of(Literal.of("y")),
                        LiteralExpression.of(Literal.of(20)))
        ));

        // ValueBuffer keys = dict.keys()
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("keys"),
                DataTypeUtils.getDataType("ValueBuffer", context),
                new MethodCallExpression(VariableExpression.of("dict"), "keys")
        ));

        new EvalVisitor(context).visit(block);

        Data keysData = context.getVariables().get("keys");
        assertTrue(keysData.get() instanceof ValueBuffer);
        ValueBuffer keys = (ValueBuffer) keysData.get();
        assertArrayEquals(new String[]{"key"}, keys.getHeader());
        assertEquals(2, keys.length());
    }

    @Test
    void testValuesReturnsValueBuffer() {
        EvalContext context = new RootEvalContext();
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("dict"),
                DataTypeUtils.getDataType("Dictionary", context),
                new FunctionCallExpression(Identifier.fromName("Dictionary.new"))
        ));

        block.addStatement(ExpressionStatement.of(
                new MethodCallExpression(VariableExpression.of("dict"),
                        "put",
                        LiteralExpression.of(Literal.of("a")),
                        LiteralExpression.of(Literal.of("alpha")))
        ));

        // ValueBuffer vals = dict.values()
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("vals"),
                DataTypeUtils.getDataType("ValueBuffer", context),
                new MethodCallExpression(VariableExpression.of("dict"), "values")
        ));

        new EvalVisitor(context).visit(block);

        Data valsData = context.getVariables().get("vals");
        assertTrue(valsData.get() instanceof ValueBuffer);
        ValueBuffer vals = (ValueBuffer) valsData.get();
        assertArrayEquals(new String[]{"key", "value"}, vals.getHeader());
        assertEquals(1, vals.length());
        assertEquals("a", vals.getRow(0)[0]);
        assertEquals("alpha", vals.getRow(0)[1]);
    }

    @Test
    void testEntriesReturnsValueBuffer() {
        EvalContext context = new RootEvalContext();
        Block block = new BlockImpl();

        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("dict"),
                DataTypeUtils.getDataType("Dictionary", context),
                new FunctionCallExpression(Identifier.fromName("Dictionary.new"))
        ));

        block.addStatement(ExpressionStatement.of(
                new MethodCallExpression(VariableExpression.of("dict"),
                        "put",
                        LiteralExpression.of(Literal.of("color")),
                        LiteralExpression.of(Literal.of("red")))
        ));

        // ValueBuffer entries = dict.entries()
        block.addStatement(new VariableDeclarationImpl(
                Identifier.fromName("entries"),
                DataTypeUtils.getDataType("ValueBuffer", context),
                new MethodCallExpression(VariableExpression.of("dict"), "entries")
        ));

        new EvalVisitor(context).visit(block);

        Data entriesData = context.getVariables().get("entries");
        assertTrue(entriesData.get() instanceof ValueBuffer);
        ValueBuffer entries = (ValueBuffer) entriesData.get();
        assertArrayEquals(new String[]{"key", "value"}, entries.getHeader());
        assertEquals(1, entries.length());
        assertEquals("color", entries.getRow(0)[0]);
        assertEquals("red", entries.getRow(0)[1]);
    }
}
