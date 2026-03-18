package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArraysTest {

    private EvalContext newContext() {
        return new RootEvalContext();
    }

    private VariableExpression var(String name) {
        return VariableExpression.of(Identifier.fromName(name));
    }

    private LiteralExpression int_(int n) {
        return LiteralExpression.of(Literal.of(n));
    }

    private LiteralExpression str(String s) {
        return LiteralExpression.of(StringLiteral.of(s));
    }

    private LiteralExpression bool_(boolean b) {
        return LiteralExpression.of(BooleanLiteral.of(b));
    }

    /**
     * Runs a method call on a pre-seeded array variable and returns the result.
     *
     * <p>We store the array in the context as {@code AnyData} with the correct array data type
     * (e.g. {@code INT_ARRAY}) so that type-based overload resolution in {@code FunCallEvaluator}
     * can correctly match the parameter. {@code ArrayData.getDataType()} returns {@code ANY}, which
     * is too permissive for dispatch.
     */
    private Object evalOnArray(String varName, Object arrayValue, SystemDataType arrayType,
                               String method, SystemDataType returnType,
                               Expression... extraArgs) {
        EvalContext ctx = newContext();
        // Use AnyData with explicit arrayType so FunCallEvaluator resolves the correct overload
        ctx.getVariables().put(varName,
            new com.elminster.jcp.eval.data.AnyData<>(
                Identifier.fromName(varName), arrayType, arrayValue));

        Expression[] allArgs = new Expression[1 + extraArgs.length];
        allArgs[0] = var(varName);
        System.arraycopy(extraArgs, 0, allArgs, 1, extraArgs.length);

        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result", returnType,
            new FunctionCallExpression(Identifier.fromName("Arrays." + method), allArgs)
        ));
        new EvalVisitor(ctx).visit(program);
        return ctx.getVariable("result").get();
    }

    // --- length ---

    @Test
    void testLength_intArray() {
        assertEquals(3, evalOnArray("a", new int[]{1, 2, 3}, SystemDataType.INT_ARRAY,
            "length", SystemDataType.INT));
    }

    @Test
    void testLength_emptyArray() {
        assertEquals(0, evalOnArray("a", new int[]{}, SystemDataType.INT_ARRAY,
            "length", SystemDataType.INT));
    }

    // --- slice ---

    @Test
    void testSlice_intArray() {
        assertArrayEquals(new int[]{2, 3},
            (int[]) evalOnArray("a", new int[]{1, 2, 3, 4}, SystemDataType.INT_ARRAY,
                "slice", SystemDataType.INT_ARRAY, int_(1), int_(3)));
    }

    @Test
    void testSlice_stringArray() {
        assertArrayEquals(new String[]{"x", "y"},
            (String[]) evalOnArray("a", new String[]{"x", "y", "z"}, SystemDataType.STRING_ARRAY,
                "slice", SystemDataType.STRING_ARRAY, int_(0), int_(2)));
    }

    @Test
    void testSlice_emptyResult() {
        assertArrayEquals(new int[]{},
            (int[]) evalOnArray("a", new int[]{1, 2}, SystemDataType.INT_ARRAY,
                "slice", SystemDataType.INT_ARRAY, int_(0), int_(0)));
    }

    // --- contains ---

    @Test
    void testContains_intFound() {
        assertEquals(true, evalOnArray("a", new int[]{1, 2, 3}, SystemDataType.INT_ARRAY,
            "contains", SystemDataType.BOOLEAN, int_(2)));
    }

    @Test
    void testContains_intNotFound() {
        assertEquals(false, evalOnArray("a", new int[]{1, 2, 3}, SystemDataType.INT_ARRAY,
            "contains", SystemDataType.BOOLEAN, int_(9)));
    }

    @Test
    void testContains_stringFound() {
        assertEquals(true, evalOnArray("a", new String[]{"apple", "banana"}, SystemDataType.STRING_ARRAY,
            "contains", SystemDataType.BOOLEAN, str("banana")));
    }

    @Test
    void testContains_stringNotFound() {
        assertEquals(false, evalOnArray("a", new String[]{"apple", "banana"}, SystemDataType.STRING_ARRAY,
            "contains", SystemDataType.BOOLEAN, str("cherry")));
    }

    @Test
    void testContains_booleanFound() {
        assertEquals(true, evalOnArray("a", new boolean[]{true, false}, SystemDataType.BOOLEAN_ARRAY,
            "contains", SystemDataType.BOOLEAN, bool_(false)));
    }

    // --- sort ---

    @Test
    void testSort_intArray() {
        assertArrayEquals(new int[]{1, 2, 3},
            (int[]) evalOnArray("a", new int[]{3, 1, 2}, SystemDataType.INT_ARRAY,
                "sort", SystemDataType.INT_ARRAY));
    }

    @Test
    void testSort_stringArray() {
        assertArrayEquals(new String[]{"a", "b", "c"},
            (String[]) evalOnArray("a", new String[]{"c", "a", "b"}, SystemDataType.STRING_ARRAY,
                "sort", SystemDataType.STRING_ARRAY));
    }

    @Test
    void testSort_booleanMixed() {
        assertArrayEquals(new boolean[]{false, true, true},
            (boolean[]) evalOnArray("a", new boolean[]{true, false, true}, SystemDataType.BOOLEAN_ARRAY,
                "sort", SystemDataType.BOOLEAN_ARRAY));
    }

    @Test
    void testSort_booleanAllTrue() {
        assertArrayEquals(new boolean[]{true, true},
            (boolean[]) evalOnArray("a", new boolean[]{true, true}, SystemDataType.BOOLEAN_ARRAY,
                "sort", SystemDataType.BOOLEAN_ARRAY));
    }

    @Test
    void testSort_booleanAllFalse() {
        assertArrayEquals(new boolean[]{false, false},
            (boolean[]) evalOnArray("a", new boolean[]{false, false}, SystemDataType.BOOLEAN_ARRAY,
                "sort", SystemDataType.BOOLEAN_ARRAY));
    }

    @Test
    void testSort_doubleArray() {
        assertArrayEquals(new double[]{1.0, 2.0, 3.0},
            (double[]) evalOnArray("a", new double[]{3.0, 1.0, 2.0}, SystemDataType.DOUBLE_ARRAY,
                "sort", SystemDataType.DOUBLE_ARRAY));
    }
}
