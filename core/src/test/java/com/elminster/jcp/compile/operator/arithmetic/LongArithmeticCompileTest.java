package com.elminster.jcp.compile.operator.arithmetic;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.LongLiteral;
import com.elminster.jcp.ast.expression.operation.*;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LongArithmeticCompileTest extends AbstractCompileTest {

    @Test
    void testLongAddition() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Plus(
                        LiteralExpression.of(LongLiteral.of(3000000000L)),
                        LiteralExpression.of(LongLiteral.of(2000000000L))
                ),
                SystemDataType.LONG,
                uniqueClassName("TestLongAdd")
        );

        Method evaluate = clazz.getMethod("evaluate");
        long result = (long) evaluate.invoke(null);
        assertEquals(5000000000L, result);
    }

    @Test
    void testLongSubtraction() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Minus(
                        LiteralExpression.of(LongLiteral.of(5000000000L)),
                        LiteralExpression.of(LongLiteral.of(2000000000L))
                ),
                SystemDataType.LONG,
                uniqueClassName("TestLongSub")
        );

        Method evaluate = clazz.getMethod("evaluate");
        long result = (long) evaluate.invoke(null);
        assertEquals(3000000000L, result);
    }

    @Test
    void testLongMultiplication() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Multi(
                        LiteralExpression.of(LongLiteral.of(100000L)),
                        LiteralExpression.of(LongLiteral.of(50000L))
                ),
                SystemDataType.LONG,
                uniqueClassName("TestLongMul")
        );

        Method evaluate = clazz.getMethod("evaluate");
        long result = (long) evaluate.invoke(null);
        assertEquals(5000000000L, result);
    }

    @Test
    void testLongDivision() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Divide(
                        LiteralExpression.of(LongLiteral.of(10000000000L)),
                        LiteralExpression.of(LongLiteral.of(4L))
                ),
                SystemDataType.LONG,
                uniqueClassName("TestLongDiv")
        );

        Method evaluate = clazz.getMethod("evaluate");
        long result = (long) evaluate.invoke(null);
        assertEquals(2500000000L, result);
    }

    @Test
    void testLongModulo() throws Exception {
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Mod(
                        LiteralExpression.of(LongLiteral.of(10L)),
                        LiteralExpression.of(LongLiteral.of(3L))
                ),
                SystemDataType.LONG,
                uniqueClassName("TestLongMod")
        );

        Method evaluate = clazz.getMethod("evaluate");
        long result = (long) evaluate.invoke(null);
        assertEquals(1L, result);
    }

    @Test
    void testLongSpecialConstants() throws Exception {
        // LCONST_0
        Class<?> clazz0 = compiler.compileAndLoadWithReturn(
                null,
                LiteralExpression.of(LongLiteral.of(0L)),
                SystemDataType.LONG,
                uniqueClassName("TestLongConst0")
        );
        assertEquals(0L, (long) clazz0.getMethod("evaluate").invoke(null));

        // LCONST_1
        Class<?> clazz1 = compiler.compileAndLoadWithReturn(
                null,
                LiteralExpression.of(LongLiteral.of(1L)),
                SystemDataType.LONG,
                uniqueClassName("TestLongConst1")
        );
        assertEquals(1L, (long) clazz1.getMethod("evaluate").invoke(null));
    }

    @Test
    void testMixedIntLongAddition() throws Exception {
        // int a = 5; return a + 3000000000L (a promoted via I2L)
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("a", SystemDataType.INT, LiteralExpression.of(5)));

        Class<?> clazz = compiler.compileAndLoadWithReturn(
                program,
                new Plus(
                        IdentifierExpression.of("a"),
                        LiteralExpression.of(LongLiteral.of(3000000000L))
                ),
                SystemDataType.LONG,
                uniqueClassName("TestMixedIntLong")
        );

        Method evaluate = clazz.getMethod("evaluate");
        long result = (long) evaluate.invoke(null);
        assertEquals(3000000005L, result);
    }

    @Test
    void testMixedLongDoubleAddition() throws Exception {
        // long a = 100L; return a + 0.5 (a promoted via L2D)
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("a", SystemDataType.LONG,
                LiteralExpression.of(LongLiteral.of(100L))));

        Class<?> clazz = compiler.compileAndLoadWithReturn(
                program,
                new Plus(
                        IdentifierExpression.of("a"),
                        LiteralExpression.of(com.elminster.jcp.ast.expression.literal.DoubleLiteral.of(0.5))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestMixedLongDouble")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(100.5, result, 0.001);
    }

    @Test
    void testLongDefaultValue() throws Exception {
        // long x; return x => 0L
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("x", SystemDataType.LONG, null));

        Class<?> clazz = compiler.compileAndLoadWithReturn(
                program,
                IdentifierExpression.of("x"),
                SystemDataType.LONG,
                uniqueClassName("TestLongDefault")
        );

        Method evaluate = clazz.getMethod("evaluate");
        long result = (long) evaluate.invoke(null);
        assertEquals(0L, result);
    }

    @Test
    void testLongPlusPlus() throws Exception {
        // long x = 5L; x++; return x => 6L
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("x", SystemDataType.LONG,
                LiteralExpression.of(LongLiteral.of(5L))));
        program.addStatement(ExpressionStatement.of(
                new PlusPlus(new VariableExpression(Identifier.fromName("x")))));

        String className = uniqueClassName("TestLongPP");
        Class<?> clazz = compileAndLoadWithReturn(program,
                new VariableExpression(Identifier.fromName("x")),
                SystemDataType.LONG, className);

        Method evaluate = clazz.getMethod("evaluate");
        long result = (long) evaluate.invoke(null);
        assertEquals(6L, result);
    }

    @Test
    void testLongMinusMinus() throws Exception {
        // long x = 10L; x--; return x => 9L
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("x", SystemDataType.LONG,
                LiteralExpression.of(LongLiteral.of(10L))));
        program.addStatement(ExpressionStatement.of(
                new MinusMinus(new VariableExpression(Identifier.fromName("x")))));

        String className = uniqueClassName("TestLongMM");
        Class<?> clazz = compileAndLoadWithReturn(program,
                new VariableExpression(Identifier.fromName("x")),
                SystemDataType.LONG, className);

        Method evaluate = clazz.getMethod("evaluate");
        long result = (long) evaluate.invoke(null);
        assertEquals(9L, result);
    }

    private Class<?> compileAndLoadWithReturn(Block program, com.elminster.jcp.ast.Expression expression,
                                               DataType returnType, String className) throws Exception {
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] mainBytecode = generator.compileWithReturn(program, expression, returnType);
        Map<String, byte[]> structClasses = generator.getGeneratedClasses();

        MultiClassLoader loader = new MultiClassLoader();
        for (Map.Entry<String, byte[]> entry : structClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }
        loader.defineClass(className, mainBytecode);

        for (String structClassName : structClasses.keySet()) {
            loader.loadClass(structClassName);
        }

        return loader.loadClass(className);
    }
}
