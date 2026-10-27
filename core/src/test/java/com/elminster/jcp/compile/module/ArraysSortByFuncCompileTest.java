package com.elminster.jcp.compile.module;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.StringLiteral;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclaration;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.module.base.arrays.SortKey;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compile-mode tests for {@code SortKey.sortByFunc()} — verifies the compiler
 * transforms the one-arg call into the two-arg version with the declaring class,
 * and that the runtime reflection resolution works.
 */
public class ArraysSortByFuncCompileTest extends AbstractCompileTest {

    // --- SortKey.sortByFunc(String, Class) unit tests (reflection resolution) ---

    @Test
    void testSortByFunc_reflectionResolution() {
        SortKey key = SortKey.sortByFunc("compareInts", TestComparator.class);
        Comparator<Object> cmp = key.toComparator();
        assertTrue(cmp.compare(1, 2) < 0);
        assertTrue(cmp.compare(2, 1) > 0);
        assertEquals(0, cmp.compare(5, 5));
    }

    @Test
    void testSortByFunc_unknownFunction_throws() {
        SortKey key = SortKey.sortByFunc("nonExistent", TestComparator.class);
        IllegalStateException ex = assertThrows(IllegalStateException.class, key::toComparator);
        assertTrue(ex.getMessage().contains("nonExistent"));
    }

    @Test
    void testSortByFunc_wrongReturnType_throws() {
        SortKey key = SortKey.sortByFunc("wrongReturnType", TestComparator.class);
        IllegalStateException ex = assertThrows(IllegalStateException.class, key::toComparator);
        assertTrue(ex.getMessage().contains("must return int"));
    }

    @Test
    void testSortByFunc_unresolvedWithoutClass_throws() {
        SortKey key = SortKey.sortByFunc("anyFunc");
        IllegalStateException ex = assertThrows(IllegalStateException.class, key::toComparator);
        assertTrue(ex.getMessage().contains("not resolved"));
    }

    @Test
    void testSortByFunc_withResolvedComparator_immutable() {
        SortKey original = SortKey.sortByFunc("myFunc");
        Comparator<Object> cmp = (a, b) -> 0;
        SortKey resolved = original.withResolvedComparator(cmp);

        assertNotSame(original, resolved);
        assertEquals("myFunc", original.getFunctionName());
        assertEquals("myFunc", resolved.getFunctionName());
        assertThrows(IllegalStateException.class, original::toComparator);
        assertEquals(0, resolved.toComparator().compare("a", "b"));
    }

    @Test
    void testSortByFunc_fieldPathUnaffected() {
        SortKey fieldKey = SortKey.by("age");
        assertNull(fieldKey.getFunctionName());
    }

    // --- Compile-mode integration: sortByFunc one-arg → two-arg transformation ---

    @Test
    void testSortByFunc_compileAndCallReturnsNonNull() throws Exception {
        // Build a program that:
        //   func myCompare(a: Any, b: Any) -> int { return 0 }
        //   let k = SortKey.sortByFunc("myCompare")
        // Verifies the compiler transforms sortByFunc(String) into sortByFunc(String, Class)

        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(LiteralExpression.of(0)));

        FunctionDeclaration compareFunc = new FunctionDeclarationImpl(
            Identifier.fromName("myCompare"),
            SystemDataType.INT,
            new ParameterDef[]{
                ParameterDef.of("a", SystemDataType.ANY),
                ParameterDef.of("b", SystemDataType.ANY)
            },
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(compareFunc);

        // SortKey k = SortKey.sortByFunc("myCompare")
        program.addStatement(new VariableDeclarationImpl(
            "k", SystemDataType.ANY,
            new FunctionCallExpression(Identifier.fromName("SortKey.sortByFunc"),
                LiteralExpression.of(StringLiteral.of("myCompare")))
        ));

        String className = uniqueClassName("TestSortByFuncCompile");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method main = clazz.getMethod("main", String[].class);

        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testSortByFunc_runtimeReflectionResolvesMethod() throws Exception {
        // Compile a function that returns a constant, then verify SortKey
        // can discover it via reflection on the compiled class.

        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(LiteralExpression.of(42)));

        FunctionDeclaration compareFunc = new FunctionDeclarationImpl(
            Identifier.fromName("intCompare"),
            SystemDataType.INT,
            new ParameterDef[]{
                ParameterDef.of("a", SystemDataType.ANY),
                ParameterDef.of("b", SystemDataType.ANY)
            },
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(compareFunc);

        program.addStatement(new VariableDeclarationImpl(
            "k", SystemDataType.ANY,
            new FunctionCallExpression(Identifier.fromName("SortKey.sortByFunc"),
                LiteralExpression.of(StringLiteral.of("intCompare")))
        ));

        String className = uniqueClassName("TestSortByFuncReflect");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method main = clazz.getMethod("main", String[].class);
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));

        // Verify the compiled class has intCompare(Object, Object) accessible via reflection
        Method intCompare = clazz.getMethod("intCompare", Object.class, Object.class);
        assertNotNull(intCompare);

        // Verify SortKey resolves the method and the comparator invokes it
        SortKey key = SortKey.sortByFunc("intCompare", clazz);
        Comparator<Object> cmp = key.toComparator();
        assertEquals(42, cmp.compare("any", "args"));
    }

    @Test
    void testSortByFunc_nonExistentFunction_runtimeError() {
        SortKey key = SortKey.sortByFunc("doesNotExist", TestComparator.class);
        IllegalStateException ex = assertThrows(IllegalStateException.class, key::toComparator);
        assertTrue(ex.getMessage().contains("doesNotExist"));
    }

    // --- Direction tests for function-name mode ---

    @Test
    void testSortByFunc_desc_reversesOrder() {
        SortKey key = SortKey.sortByFunc("compareInts", TestComparator.class).desc();
        Comparator<Object> cmp = key.toComparator();
        assertTrue(cmp.compare(1, 2) > 0, "desc should reverse: 1 vs 2 should be > 0");
        assertTrue(cmp.compare(2, 1) < 0, "desc should reverse: 2 vs 1 should be < 0");
        assertEquals(0, cmp.compare(5, 5));
    }

    @Test
    void testSortByFunc_asc_preservesOrder() {
        SortKey key = SortKey.sortByFunc("compareInts", TestComparator.class).asc();
        Comparator<Object> cmp = key.toComparator();
        assertTrue(cmp.compare(1, 2) < 0);
        assertTrue(cmp.compare(2, 1) > 0);
    }

    @Test
    void testSortByFunc_withResolvedComparator_desc() {
        Comparator<Object> ascending = (a, b) -> Integer.compare((Integer) a, (Integer) b);
        SortKey key = SortKey.sortByFunc("f").withResolvedComparator(ascending);
        SortKey descKey = key.desc();
        Comparator<Object> cmp = descKey.toComparator();
        assertTrue(cmp.compare(1, 2) > 0, "desc on pre-resolved comparator should reverse");
    }

    /**
     * Test helper class with static comparator methods for reflection testing.
     */
    public static class TestComparator {
        public static int compareInts(Object a, Object b) {
            return Integer.compare((Integer) a, (Integer) b);
        }

        public static String wrongReturnType(Object a, Object b) {
            return "wrong";
        }
    }
}
