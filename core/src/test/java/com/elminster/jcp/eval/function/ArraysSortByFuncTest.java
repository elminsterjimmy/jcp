package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.FieldAccessExpression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.Minus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.ast.statement.function.AbstractFunction;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.ast.statement.function.ParameterDef;
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
import com.elminster.jcp.eval.excpetion.EvaluationException;
import com.elminster.jcp.module.base.arrays.SortKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Eval-mode integration tests for {@code Arrays.sort(Object[], SortKey.sortByFunc(...))}.
 */
public class ArraysSortByFuncTest {

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
     * Registers a comparator function and runs Arrays.sort via function dispatch.
     */
    private Object[] sortByFunc(StructData[] persons, String funcName, Function comparator) {
        EvalContext ctx = new RootEvalContext();
        DataType sortKeyType = ctx.getDataType("SortKey");

        ctx.addFunction(comparator);

        ctx.getVariables().put("arr",
            new AnyData<>(Identifier.fromName("arr"), SystemDataType.ANY_ARRAY,
                (Object) persons));

        SortKey key = SortKey.sortByFunc(funcName);
        ctx.getVariables().put("k0", new AnyData<>(key, sortKeyType));

        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", SystemDataType.ANY_ARRAY,
            new FunctionCallExpression(Identifier.fromName("Arrays.sort"),
                VariableExpression.of(Identifier.fromName("arr")),
                VariableExpression.of(Identifier.fromName("k0")))
        ));
        new EvalVisitor(ctx).visit(program);
        return (Object[]) ctx.getVariable("result").get();
    }

    private Function ageComparator(String funcName) {
        // func funcName(a: Any, b: Any) -> int { return a.age - b.age }
        return new AbstractFunction(
            IdentifierExpression.of(funcName),
            new ParameterDef[]{
                ParameterDef.of("a", SystemDataType.ANY),
                ParameterDef.of("b", SystemDataType.ANY)
            },
            SystemDataType.INT,
            new ReturnStatement(
                new Minus(
                    new FieldAccessExpression(VariableExpression.of("a"), "age"),
                    new FieldAccessExpression(VariableExpression.of("b"), "age")
                )
            )
        );
    }

    // --- Happy-path tests ---

    @Test
    void testSortByAge_ascending() {
        StructData[] persons = {
            person("Charlie", 30),
            person("Alice", 10),
            person("Bob", 20)
        };
        Object[] sorted = sortByFunc(persons, "ageCompare", ageComparator("ageCompare"));
        assertEquals(10, ((StructData) sorted[0]).getField("age").get());
        assertEquals(20, ((StructData) sorted[1]).getField("age").get());
        assertEquals(30, ((StructData) sorted[2]).getField("age").get());
    }

    @Test
    void testSortByAge_descending() {
        // func descAge(a, b) -> int { return b.age - a.age }
        Function descAgeComparator = new AbstractFunction(
            IdentifierExpression.of("descAge"),
            new ParameterDef[]{
                ParameterDef.of("a", SystemDataType.ANY),
                ParameterDef.of("b", SystemDataType.ANY)
            },
            SystemDataType.INT,
            new ReturnStatement(
                new Minus(
                    new FieldAccessExpression(VariableExpression.of("b"), "age"),
                    new FieldAccessExpression(VariableExpression.of("a"), "age")
                )
            )
        );
        StructData[] persons = {
            person("Alice", 10),
            person("Charlie", 30),
            person("Bob", 20)
        };
        Object[] sorted = sortByFunc(persons, "descAge", descAgeComparator);
        assertEquals(30, ((StructData) sorted[0]).getField("age").get());
        assertEquals(20, ((StructData) sorted[1]).getField("age").get());
        assertEquals(10, ((StructData) sorted[2]).getField("age").get());
    }

    @Test
    void testMixedSortKeys_functionAndFieldPath() {
        EvalContext ctx = new RootEvalContext();
        DataType sortKeyType = ctx.getDataType("SortKey");

        Function ageCompare = ageComparator("ageCompare");
        ctx.addFunction(ageCompare);

        StructData[] persons = {
            person("Charlie", 20),
            person("Alice", 20),
            person("Bob", 10)
        };
        ctx.getVariables().put("arr",
            new AnyData<>(Identifier.fromName("arr"), SystemDataType.ANY_ARRAY,
                (Object) persons));

        SortKey funcKey = SortKey.sortByFunc("ageCompare");
        SortKey fieldKey = SortKey.by("name");
        ctx.getVariables().put("k0", new AnyData<>(funcKey, sortKeyType));
        ctx.getVariables().put("k1", new AnyData<>(fieldKey, sortKeyType));

        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", SystemDataType.ANY_ARRAY,
            new FunctionCallExpression(Identifier.fromName("Arrays.sort"),
                VariableExpression.of(Identifier.fromName("arr")),
                VariableExpression.of(Identifier.fromName("k0")),
                VariableExpression.of(Identifier.fromName("k1")))
        ));
        new EvalVisitor(ctx).visit(program);
        Object[] sorted = (Object[]) ctx.getVariable("result").get();

        // Bob (10), Alice (20), Charlie (20) — age ascending, then name ascending
        assertEquals("Bob",     ((StructData) sorted[0]).getField("name").get());
        assertEquals("Alice",   ((StructData) sorted[1]).getField("name").get());
        assertEquals("Charlie", ((StructData) sorted[2]).getField("name").get());
    }

    // --- Error-path tests ---

    @Test
    void testUnknownFunctionName_throwsException() {
        StructData[] persons = { person("Alice", 10) };
        EvalContext ctx = new RootEvalContext();
        DataType sortKeyType = ctx.getDataType("SortKey");

        ctx.getVariables().put("arr",
            new AnyData<>(Identifier.fromName("arr"), SystemDataType.ANY_ARRAY,
                (Object) persons));

        SortKey key = SortKey.sortByFunc("nonExistentFunc");
        ctx.getVariables().put("k0", new AnyData<>(key, sortKeyType));

        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", SystemDataType.ANY_ARRAY,
            new FunctionCallExpression(Identifier.fromName("Arrays.sort"),
                VariableExpression.of(Identifier.fromName("arr")),
                VariableExpression.of(Identifier.fromName("k0")))
        ));

        EvaluationException ex = assertThrows(EvaluationException.class,
            () -> new EvalVisitor(ctx).visit(program));
        assertTrue(ex.getMessage().contains("nonExistentFunc"));
    }

    @Test
    void testWrongArity_oneParam_throwsException() {
        Function oneParam = new AbstractFunction(
            IdentifierExpression.of("oneParam"),
            new ParameterDef[]{
                ParameterDef.of("a", SystemDataType.ANY)
            },
            SystemDataType.INT,
            new ReturnStatement(LiteralExpression.of(0))
        );

        StructData[] persons = { person("Alice", 10) };
        EvalContext ctx = new RootEvalContext();
        DataType sortKeyType = ctx.getDataType("SortKey");
        ctx.addFunction(oneParam);

        ctx.getVariables().put("arr",
            new AnyData<>(Identifier.fromName("arr"), SystemDataType.ANY_ARRAY,
                (Object) persons));

        SortKey key = SortKey.sortByFunc("oneParam");
        ctx.getVariables().put("k0", new AnyData<>(key, sortKeyType));

        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", SystemDataType.ANY_ARRAY,
            new FunctionCallExpression(Identifier.fromName("Arrays.sort"),
                VariableExpression.of(Identifier.fromName("arr")),
                VariableExpression.of(Identifier.fromName("k0")))
        ));

        EvaluationException ex = assertThrows(EvaluationException.class,
            () -> new EvalVisitor(ctx).visit(program));
        assertTrue(ex.getMessage().contains("2 parameters"));
    }

    @Test
    void testWrongArity_threeParams_throwsException() {
        Function threeParam = new AbstractFunction(
            IdentifierExpression.of("threeParam"),
            new ParameterDef[]{
                ParameterDef.of("a", SystemDataType.ANY),
                ParameterDef.of("b", SystemDataType.ANY),
                ParameterDef.of("c", SystemDataType.ANY)
            },
            SystemDataType.INT,
            new ReturnStatement(LiteralExpression.of(0))
        );

        StructData[] persons = { person("Alice", 10) };
        EvalContext ctx = new RootEvalContext();
        DataType sortKeyType = ctx.getDataType("SortKey");
        ctx.addFunction(threeParam);

        ctx.getVariables().put("arr",
            new AnyData<>(Identifier.fromName("arr"), SystemDataType.ANY_ARRAY,
                (Object) persons));

        SortKey key = SortKey.sortByFunc("threeParam");
        ctx.getVariables().put("k0", new AnyData<>(key, sortKeyType));

        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", SystemDataType.ANY_ARRAY,
            new FunctionCallExpression(Identifier.fromName("Arrays.sort"),
                VariableExpression.of(Identifier.fromName("arr")),
                VariableExpression.of(Identifier.fromName("k0")))
        ));

        EvaluationException ex = assertThrows(EvaluationException.class,
            () -> new EvalVisitor(ctx).visit(program));
        assertTrue(ex.getMessage().contains("2 parameters"));
    }

    @Test
    void testNonIntReturn_throwsException() {
        // func badReturn(a, b) -> string { return "hello" }
        Function badReturn = new AbstractFunction(
            IdentifierExpression.of("badReturn"),
            new ParameterDef[]{
                ParameterDef.of("a", SystemDataType.ANY),
                ParameterDef.of("b", SystemDataType.ANY)
            },
            SystemDataType.STRING,
            new ReturnStatement(LiteralExpression.of("hello"))
        );

        StructData[] persons = {
            person("Alice", 10),
            person("Bob", 20)
        };
        EvalContext ctx = new RootEvalContext();
        DataType sortKeyType = ctx.getDataType("SortKey");
        ctx.addFunction(badReturn);

        ctx.getVariables().put("arr",
            new AnyData<>(Identifier.fromName("arr"), SystemDataType.ANY_ARRAY,
                (Object) persons));

        SortKey key = SortKey.sortByFunc("badReturn");
        ctx.getVariables().put("k0", new AnyData<>(key, sortKeyType));

        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", SystemDataType.ANY_ARRAY,
            new FunctionCallExpression(Identifier.fromName("Arrays.sort"),
                VariableExpression.of(Identifier.fromName("arr")),
                VariableExpression.of(Identifier.fromName("k0")))
        ));

        assertThrows(EvaluationException.class,
            () -> new EvalVisitor(ctx).visit(program));
    }
}
