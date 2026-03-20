package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.IntegerData;
import com.elminster.jcp.eval.data.StringData;
import com.elminster.jcp.eval.data.StructData;
import com.elminster.jcp.eval.data.StructType;
import com.elminster.jcp.module.base.arrays.SortKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@code Arrays.sort(Object[], SortKey...)} overloads via
 * {@link FunctionCallExpression} dispatch — validates the full JCP runtime path.
 */
public class ArraysSortStructTest {

    private StructType personType;

    @BeforeEach
    void setUp() {
        personType = new StructType("Person", Arrays.asList(
            new StructFieldDef("name", SystemDataType.STRING),
            new StructFieldDef("age", SystemDataType.INT)
        ));
    }

    private StructData person(String name, int age) {
        StructData p = new StructData(Identifier.fromName("p"), personType);
        p.setField("name", new StringData(name, false));
        p.setField("age", new IntegerData(age, false));
        return p;
    }

    /**
     * Seeds a struct array and a SortKey in the context, then calls Arrays.sort via
     * FunctionCallExpression and returns the sorted array.
     */
    private Object[] sortViaDispatch(StructData[] persons, SortKey... keys) {
        EvalContext ctx = new RootEvalContext();
        DataType sortKeyType = ctx.getDataType("SortKey");

        // Seed array
        ctx.getVariables().put("arr",
            new AnyData<>(Identifier.fromName("arr"), SystemDataType.ANY_ARRAY,
                (Object) persons));

        // Seed each SortKey as a variable with the registered SortKey DataType
        Expression[] args = new Expression[1 + keys.length];
        args[0] = VariableExpression.of(Identifier.fromName("arr"));
        for (int i = 0; i < keys.length; i++) {
            String varName = "k" + i;
            ctx.getVariables().put(varName, new AnyData<>(keys[i], sortKeyType));
            args[1 + i] = VariableExpression.of(Identifier.fromName(varName));
        }

        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", SystemDataType.ANY_ARRAY,
            new FunctionCallExpression(Identifier.fromName("Arrays.sort"), args)
        ));
        new EvalVisitor(ctx).visit(program);
        return (Object[]) ctx.getVariable("result").get();
    }

    // --- 1-key sort ---

    @Test
    void testSort_oneKey_byAge_asc() {
        StructData[] persons = {
            person("Charlie", 30),
            person("Alice", 10),
            person("Bob", 20)
        };
        Object[] sorted = sortViaDispatch(persons, SortKey.by("age"));
        assertEquals(10, ((StructData) sorted[0]).getField("age").get());
        assertEquals(20, ((StructData) sorted[1]).getField("age").get());
        assertEquals(30, ((StructData) sorted[2]).getField("age").get());
    }

    @Test
    void testSort_oneKey_byName_desc() {
        StructData[] persons = {
            person("Alice", 1),
            person("Charlie", 2),
            person("Bob", 3)
        };
        Object[] sorted = sortViaDispatch(persons, SortKey.by("name").desc());
        assertEquals("Charlie", ((StructData) sorted[0]).getField("name").get());
        assertEquals("Bob",     ((StructData) sorted[1]).getField("name").get());
        assertEquals("Alice",   ((StructData) sorted[2]).getField("name").get());
    }

    // --- 2-key sort ---

    @Test
    void testSort_twoKeys_sameAge_thenByName() {
        StructData[] persons = {
            person("Charlie", 20),
            person("Alice", 20),
            person("Bob", 10)
        };
        Object[] sorted = sortViaDispatch(persons, SortKey.by("age"), SortKey.by("name"));
        // Bob (10) first, then Alice (20), then Charlie (20)
        assertEquals("Bob",     ((StructData) sorted[0]).getField("name").get());
        assertEquals("Alice",   ((StructData) sorted[1]).getField("name").get());
        assertEquals("Charlie", ((StructData) sorted[2]).getField("name").get());
    }

    // --- 3-key sort ---

    @Test
    void testSort_threeKeys() {
        StructType t = new StructType("T", Arrays.asList(
            new StructFieldDef("a", SystemDataType.INT),
            new StructFieldDef("b", SystemDataType.INT),
            new StructFieldDef("c", SystemDataType.INT)
        ));
        StructData s1 = new StructData(Identifier.fromName("s1"), t);
        s1.setField("a", new IntegerData(1, false));
        s1.setField("b", new IntegerData(2, false));
        s1.setField("c", new IntegerData(3, false));
        StructData s2 = new StructData(Identifier.fromName("s2"), t);
        s2.setField("a", new IntegerData(1, false));
        s2.setField("b", new IntegerData(2, false));
        s2.setField("c", new IntegerData(1, false));

        EvalContext ctx = new RootEvalContext();
        DataType sortKeyType = ctx.getDataType("SortKey");
        ctx.getVariables().put("arr",
            new AnyData<>(Identifier.fromName("arr"), SystemDataType.ANY_ARRAY,
                new Object[]{s1, s2}));
        ctx.getVariables().put("k0", new AnyData<>(SortKey.by("a"), sortKeyType));
        ctx.getVariables().put("k1", new AnyData<>(SortKey.by("b"), sortKeyType));
        ctx.getVariables().put("k2", new AnyData<>(SortKey.by("c"), sortKeyType));

        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", SystemDataType.ANY_ARRAY,
            new FunctionCallExpression(Identifier.fromName("Arrays.sort"),
                VariableExpression.of(Identifier.fromName("arr")),
                VariableExpression.of(Identifier.fromName("k0")),
                VariableExpression.of(Identifier.fromName("k1")),
                VariableExpression.of(Identifier.fromName("k2")))
        ));
        new EvalVisitor(ctx).visit(program);
        Object[] sorted = (Object[]) ctx.getVariable("result").get();

        // s2 (c=1) before s1 (c=3)
        assertEquals(1, ((StructData) sorted[0]).getField("c").get());
        assertEquals(3, ((StructData) sorted[1]).getField("c").get());
    }

    // --- null-field element sorts last ---

    @Test
    void testSort_missingField_sortsLast() {
        StructData withAge = person("Alice", 5);
        StructData noAge = new StructData(Identifier.fromName("noAge"), personType);
        noAge.setField("name", new StringData("Bob", false));
        // "age" field not set → null

        Object[] sorted = sortViaDispatch(new StructData[]{noAge, withAge}, SortKey.by("age"));
        // withAge (5) first, noAge (null) last
        assertEquals(5, ((StructData) sorted[0]).getField("age").get());
        assertNull(((StructData) sorted[1]).getField("age"));
    }
}
