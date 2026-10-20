package com.elminster.jcp.compile.operator.relational;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.LongLiteral;
import com.elminster.jcp.ast.expression.operation.*;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class LongComparisonCompileTest extends AbstractCompileTest {

    @Test
    void testLongLessThan_true() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LessThan(
                        LiteralExpression.of(LongLiteral.of(1L)),
                        LiteralExpression.of(LongLiteral.of(2L))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestLongLT")
        );
        assertTrue((boolean) clazz.getMethod("evaluate").invoke(null));
    }

    @Test
    void testLongLessThan_false() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LessThan(
                        LiteralExpression.of(LongLiteral.of(3L)),
                        LiteralExpression.of(LongLiteral.of(2L))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestLongLTFalse")
        );
        assertFalse((boolean) clazz.getMethod("evaluate").invoke(null));
    }

    @Test
    void testLongGreaterThan_true() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new GreaterThan(
                        LiteralExpression.of(LongLiteral.of(3000000000L)),
                        LiteralExpression.of(LongLiteral.of(2000000000L))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestLongGT")
        );
        assertTrue((boolean) clazz.getMethod("evaluate").invoke(null));
    }

    @Test
    void testLongGreaterThan_false() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new GreaterThan(
                        LiteralExpression.of(LongLiteral.of(1L)),
                        LiteralExpression.of(LongLiteral.of(2L))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestLongGTFalse")
        );
        assertFalse((boolean) clazz.getMethod("evaluate").invoke(null));
    }

    @Test
    void testLongEqual_true() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Equal(
                        LiteralExpression.of(LongLiteral.of(42L)),
                        LiteralExpression.of(LongLiteral.of(42L))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestLongEQ")
        );
        assertTrue((boolean) clazz.getMethod("evaluate").invoke(null));
    }

    @Test
    void testLongEqual_false() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Equal(
                        LiteralExpression.of(LongLiteral.of(42L)),
                        LiteralExpression.of(LongLiteral.of(43L))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestLongEQFalse")
        );
        assertFalse((boolean) clazz.getMethod("evaluate").invoke(null));
    }

    @Test
    void testLongNotEqual_true() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new NotEqual(
                        LiteralExpression.of(LongLiteral.of(1L)),
                        LiteralExpression.of(LongLiteral.of(2L))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestLongNE")
        );
        assertTrue((boolean) clazz.getMethod("evaluate").invoke(null));
    }

    @Test
    void testLongLessOrEqual_true() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LessThanEqual(
                        LiteralExpression.of(LongLiteral.of(2L)),
                        LiteralExpression.of(LongLiteral.of(2L))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestLongLE")
        );
        assertTrue((boolean) clazz.getMethod("evaluate").invoke(null));
    }

    @Test
    void testLongGreaterOrEqual_true() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new GreaterThanEqual(
                        LiteralExpression.of(LongLiteral.of(5L)),
                        LiteralExpression.of(LongLiteral.of(5L))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestLongGE")
        );
        assertTrue((boolean) clazz.getMethod("evaluate").invoke(null));
    }

    @Test
    void testMixedIntLongComparison() throws Exception {
        // int a = 5; return a < 3000000000L (a promoted via I2L)
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("a", SystemDataType.INT, LiteralExpression.of(5)));

        Class<?> clazz = compiler.compileAndLoadWithReturn(
                program,
                new LessThan(
                        IdentifierExpression.of("a"),
                        LiteralExpression.of(LongLiteral.of(3000000000L))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestMixedIntLongCmp")
        );
        assertTrue((boolean) clazz.getMethod("evaluate").invoke(null));
    }
}
