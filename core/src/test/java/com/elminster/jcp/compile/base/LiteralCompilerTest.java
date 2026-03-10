package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.NullLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LiteralCompiler.
 * Tests compilation of different literal types and value ranges.
 */
public class LiteralCompilerTest extends AbstractCompileTest {

    @Nested
    class IntLiteralTests {

        /**
         * Tests integer literal -1 (uses ICONST_M1).
         * <pre>
         * return -1  // uses ICONST_M1
         * </pre>
         */
        @Test
        void testIntLiteral_NegativeOne() throws Exception {
            int result = compileAndRunIntLiteral(-1);
            assertEquals(-1, result);
        }

        /**
         * Tests integer literal 0 (uses ICONST_0).
         * <pre>
         * return 0  // uses ICONST_0
         * </pre>
         */
        @Test
        void testIntLiteral_Zero() throws Exception {
            int result = compileAndRunIntLiteral(0);
            assertEquals(0, result);
        }

        /**
         * Tests integer literal 5 (uses ICONST_5).
         * <pre>
         * return 5  // uses ICONST_5
         * </pre>
         */
        @Test
        void testIntLiteral_Five() throws Exception {
            int result = compileAndRunIntLiteral(5);
            assertEquals(5, result);
        }

        /**
         * Tests integer literal in byte range (uses BIPUSH).
         * <pre>
         * return 100  // uses BIPUSH
         * </pre>
         */
        @Test
        void testIntLiteral_ByteRange() throws Exception {
            int result = compileAndRunIntLiteral(100);
            assertEquals(100, result);
        }

        /**
         * Tests negative integer in byte range (uses BIPUSH).
         * <pre>
         * return -50  // uses BIPUSH
         * </pre>
         */
        @Test
        void testIntLiteral_NegativeByteRange() throws Exception {
            int result = compileAndRunIntLiteral(-50);
            assertEquals(-50, result);
        }

        /**
         * Tests integer literal in short range (uses SIPUSH).
         * <pre>
         * return 1000  // uses SIPUSH
         * </pre>
         */
        @Test
        void testIntLiteral_ShortRange() throws Exception {
            int result = compileAndRunIntLiteral(1000);
            assertEquals(1000, result);
        }

        /**
         * Tests negative integer in short range (uses SIPUSH).
         * <pre>
         * return -1000  // uses SIPUSH
         * </pre>
         */
        @Test
        void testIntLiteral_NegativeShortRange() throws Exception {
            int result = compileAndRunIntLiteral(-1000);
            assertEquals(-1000, result);
        }

        /**
         * Tests large integer literal (uses LDC).
         * <pre>
         * return 100000  // uses LDC
         * </pre>
         */
        @Test
        void testIntLiteral_LargeValue() throws Exception {
            int result = compileAndRunIntLiteral(100000);
            assertEquals(100000, result);
        }

        /**
         * Tests large negative integer literal (uses LDC).
         * <pre>
         * return -100000  // uses LDC
         * </pre>
         */
        @Test
        void testIntLiteral_LargeNegativeValue() throws Exception {
            int result = compileAndRunIntLiteral(-100000);
            assertEquals(-100000, result);
        }

        /**
         * Tests boundary value Byte.MAX_VALUE (uses BIPUSH).
         * <pre>
         * return 127  // uses BIPUSH
         * </pre>
         */
        @Test
        void testIntLiteral_ByteMaxValue() throws Exception {
            int result = compileAndRunIntLiteral(Byte.MAX_VALUE);
            assertEquals(127, result);
        }

        /**
         * Tests boundary value Byte.MIN_VALUE (uses BIPUSH).
         * <pre>
         * return -128  // uses BIPUSH
         * </pre>
         */
        @Test
        void testIntLiteral_ByteMinValue() throws Exception {
            int result = compileAndRunIntLiteral(Byte.MIN_VALUE);
            assertEquals(-128, result);
        }

        /**
         * Tests boundary value Short.MAX_VALUE (uses SIPUSH).
         * <pre>
         * return 32767  // uses SIPUSH
         * </pre>
         */
        @Test
        void testIntLiteral_ShortMaxValue() throws Exception {
            int result = compileAndRunIntLiteral(Short.MAX_VALUE);
            assertEquals(32767, result);
        }

        /**
         * Tests boundary value Short.MIN_VALUE (uses SIPUSH).
         * <pre>
         * return -32768  // uses SIPUSH
         * </pre>
         */
        @Test
        void testIntLiteral_ShortMinValue() throws Exception {
            int result = compileAndRunIntLiteral(Short.MIN_VALUE);
            assertEquals(-32768, result);
        }

        private int compileAndRunIntLiteral(int value) throws Exception {
            Block program = new BlockImpl();
            String className = uniqueClassName("IntLiteralTest");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            byte[] bytecode = generator.compileWithReturn(program, LiteralExpression.of(value), SystemDataType.INT);

            Class<?> clazz = loadClass(className, bytecode);
            Method evaluate = clazz.getMethod("evaluate");
            return (int) evaluate.invoke(null);
        }
    }

    @Nested
    class DoubleLiteralTests {

        /**
         * Tests double literal 0.0 (uses DCONST_0).
         * <pre>
         * return 0.0  // uses DCONST_0
         * </pre>
         */
        @Test
        void testDoubleLiteral_Zero() throws Exception {
            double result = compileAndRunDoubleLiteral(0.0);
            assertEquals(0.0, result, 0.0001);
        }

        /**
         * Tests double literal 1.0 (uses DCONST_1).
         * <pre>
         * return 1.0  // uses DCONST_1
         * </pre>
         */
        @Test
        void testDoubleLiteral_One() throws Exception {
            double result = compileAndRunDoubleLiteral(1.0);
            assertEquals(1.0, result, 0.0001);
        }

        /**
         * Tests double literal 3.14 (uses LDC2_W).
         * <pre>
         * return 3.14  // uses LDC2_W
         * </pre>
         */
        @Test
        void testDoubleLiteral_Pi() throws Exception {
            double result = compileAndRunDoubleLiteral(3.14);
            assertEquals(3.14, result, 0.0001);
        }

        /**
         * Tests negative double literal.
         * <pre>
         * return -2.5  // uses LDC2_W
         * </pre>
         */
        @Test
        void testDoubleLiteral_Negative() throws Exception {
            double result = compileAndRunDoubleLiteral(-2.5);
            assertEquals(-2.5, result, 0.0001);
        }

        private double compileAndRunDoubleLiteral(double value) throws Exception {
            Block program = new BlockImpl();
            String className = uniqueClassName("DoubleLiteralTest");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            byte[] bytecode = generator.compileWithReturn(program, LiteralExpression.of(value), SystemDataType.DOUBLE);

            Class<?> clazz = loadClass(className, bytecode);
            Method evaluate = clazz.getMethod("evaluate");
            return (double) evaluate.invoke(null);
        }
    }

    @Nested
    class BooleanLiteralTests {

        /**
         * Tests boolean literal true.
         * <pre>
         * return true  // uses ICONST_1
         * </pre>
         */
        @Test
        void testBooleanLiteral_True() throws Exception {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl("b", SystemDataType.BOOLEAN, LiteralExpression.of(true)));

            String className = uniqueClassName("BoolTrueTest");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            byte[] bytecode = generator.compileWithReturn(program, VariableExpression.of("b"), SystemDataType.BOOLEAN);

            Class<?> clazz = loadClass(className, bytecode);
            Method evaluate = clazz.getMethod("evaluate");
            boolean result = (boolean) evaluate.invoke(null);
            assertTrue(result);
        }

        /**
         * Tests boolean literal false.
         * <pre>
         * return false  // uses ICONST_0
         * </pre>
         */
        @Test
        void testBooleanLiteral_False() throws Exception {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl("b", SystemDataType.BOOLEAN, LiteralExpression.of(false)));

            String className = uniqueClassName("BoolFalseTest");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            byte[] bytecode = generator.compileWithReturn(program, VariableExpression.of("b"), SystemDataType.BOOLEAN);

            Class<?> clazz = loadClass(className, bytecode);
            Method evaluate = clazz.getMethod("evaluate");
            boolean result = (boolean) evaluate.invoke(null);
            assertFalse(result);
        }
    }

    @Nested
    class StringLiteralTests {

        /**
         * Tests string literal.
         * <pre>
         * return "hello"  // uses LDC
         * </pre>
         */
        @Test
        void testStringLiteral() throws Exception {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl("s", SystemDataType.STRING, LiteralExpression.of("hello")));

            String className = uniqueClassName("StringLiteralTest");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            byte[] bytecode = generator.compileWithReturn(program, VariableExpression.of("s"), SystemDataType.STRING);

            Class<?> clazz = loadClass(className, bytecode);
            Method evaluate = clazz.getMethod("evaluate");
            String result = (String) evaluate.invoke(null);
            assertEquals("hello", result);
        }

        /**
         * Tests empty string literal.
         * <pre>
         * return ""  // uses LDC
         * </pre>
         */
        @Test
        void testStringLiteral_Empty() throws Exception {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl("s", SystemDataType.STRING, LiteralExpression.of("")));

            String className = uniqueClassName("EmptyStringTest");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            byte[] bytecode = generator.compileWithReturn(program, VariableExpression.of("s"), SystemDataType.STRING);

            Class<?> clazz = loadClass(className, bytecode);
            Method evaluate = clazz.getMethod("evaluate");
            String result = (String) evaluate.invoke(null);
            assertEquals("", result);
        }
    }

    private Class<?> loadClass(String name, byte[] bytecode) {
        return new ClassLoader() {
            public Class<?> defineClass() {
                return defineClass(name, bytecode, 0, bytecode.length);
            }
        }.defineClass();
    }

    @Nested
    class NullLiteralTests {

        /**
         * Tests null literal compiles to ACONST_NULL and evaluates to null.
         * <pre>
         * return null  // uses ACONST_NULL + ARETURN
         * </pre>
         */
        @Test
        void testNullLiteral_compilesToNull() throws Exception {
            Block program = new BlockImpl();
            String className = uniqueClassName("NullLiteralTest");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            byte[] bytecode = generator.compileWithReturn(
                    program,
                    LiteralExpression.of(NullLiteral.INSTANCE),
                    SystemDataType.ANY);

            Class<?> clazz = loadClass(className, bytecode);
            Method evaluate = clazz.getMethod("evaluate");
            Object result = evaluate.invoke(null);
            assertNull(result);
        }

        /**
         * Tests that LiteralCompiler.resolveType() returns ANY for a NullLiteral.
         */
        @Test
        void testNullLiteral_resolveTypeIsAny() throws Exception {
            LiteralCompiler compiler = new LiteralCompiler(
                    LiteralExpression.of(NullLiteral.INSTANCE));
            assertEquals(SystemDataType.ANY, compiler.resolveType(null));
        }
    }
}
