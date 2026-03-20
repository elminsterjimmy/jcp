package com.elminster.jcp.compile.module;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.compile.AbstractCompileTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Arrays module in compile mode.
 *
 * <p>All tests use {@code String[]} arrays produced via {@code Strings.split()} since
 * there is no array-literal AST node yet. Verified inline using {@code Assertions}.
 */
public class ArraysCompileTest extends AbstractCompileTest {

    private StaticMethodCallExpression arrays(String method, StaticMethodCallExpression arrayArg,
                                              LiteralExpression... extras) {
        com.elminster.jcp.ast.Expression[] args =
            new com.elminster.jcp.ast.Expression[1 + extras.length];
        args[0] = arrayArg;
        System.arraycopy(extras, 0, args, 1, extras.length);
        return new StaticMethodCallExpression("Arrays", method, args);
    }

    private StaticMethodCallExpression split(String s, String delim) {
        return new StaticMethodCallExpression("Strings", "split",
            LiteralExpression.of(StringLiteral.of(s)),
            LiteralExpression.of(StringLiteral.of(delim)));
    }

    private StaticMethodCallExpression assertTrue(StaticMethodCallExpression actual) {
        return new StaticMethodCallExpression("Assertions", "assertTrue", actual);
    }

    private StaticMethodCallExpression assertFalse(StaticMethodCallExpression actual) {
        return new StaticMethodCallExpression("Assertions", "assertFalse", actual);
    }

    private StaticMethodCallExpression assertEq(LiteralExpression expected,
                                                StaticMethodCallExpression actual) {
        return new StaticMethodCallExpression("Assertions", "assertEquals", expected, actual);
    }

    private LiteralExpression str(String s) {
        return LiteralExpression.of(StringLiteral.of(s));
    }

    private LiteralExpression int_(int n) {
        return LiteralExpression.of(Literal.of(n));
    }

    private Method compileStatements(String baseName,
                                     StaticMethodCallExpression... calls) throws Exception {
        Block program = new BlockImpl();
        for (StaticMethodCallExpression c : calls) {
            program.addStatement(new ExpressionStatement(c));
        }
        String className = uniqueClassName(baseName);
        Class<?> clazz = compiler.compileAndLoad(program, className);
        return clazz.getMethod("main", String[].class);
    }

    @Test
    void testLength() throws Exception {
        Method main = compileStatements("TestArraysLength",
            assertEq(int_(3), arrays("length", split("a,b,c", ","))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testContainsFound() throws Exception {
        Method main = compileStatements("TestArraysContainsFound",
            assertTrue(arrays("contains", split("a,b,c", ","), str("b"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testContainsNotFound() throws Exception {
        Method main = compileStatements("TestArraysContainsNotFound",
            assertFalse(arrays("contains", split("a,b,c", ","), str("x"))));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testSort() throws Exception {
        Method main = compileStatements("TestArraysSort",
            arrays("sort", split("c,a,b", ",")));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testSlice() throws Exception {
        Method main = compileStatements("TestArraysSlice",
            arrays("slice", split("a,b,c", ","), int_(0), int_(2)));
        assertDoesNotThrow(() -> main.invoke(null, (Object) new String[]{}));
    }
}
