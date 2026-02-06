package com.elminster.jcp.eval.operator.arithmetic;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.operation.Divide;
import com.elminster.jcp.ast.expression.operation.Minus;
import com.elminster.jcp.ast.expression.operation.Mod;
import com.elminster.jcp.ast.expression.operation.Multi;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for arithmetic evaluators with various type combinations.
 */
class ArithmeticEvaluatorTest {

    private EvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
    }

    @Nested
    class DivideEvaluatorTests {

        /**
         * Tests integer division truncates to integer result.
         * <pre>
         * var result: Int = 10 / 3  // result = 3
         * </pre>
         */
        @Test
        void testDivide_IntByInt() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new Divide(LiteralExpression.of(10), LiteralExpression.of(3))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(3, context.getVariable("result").get());
        }

        /**
         * Tests double division preserves decimal precision.
         * <pre>
         * var result: Double = 10.0 / 4.0  // result = 2.5
         * </pre>
         */
        @Test
        void testDivide_DoubleByDouble() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Divide(LiteralExpression.of(10.0), LiteralExpression.of(4.0))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(2.5, (Double) context.getVariable("result").get(), 0.0001);
        }

        /**
         * Tests mixed type division (Int / Double) promotes to Double.
         * <pre>
         * var result: Double = 10 / 4.0  // result = 2.5
         * </pre>
         */
        @Test
        void testDivide_IntByDouble() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Divide(LiteralExpression.of(10), LiteralExpression.of(4.0))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(2.5, (Double) context.getVariable("result").get(), 0.0001);
        }

        /**
         * Tests mixed type division (Double / Int) promotes to Double.
         * <pre>
         * var result: Double = 10.0 / 4  // result = 2.5
         * </pre>
         */
        @Test
        void testDivide_DoubleByInt() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Divide(LiteralExpression.of(10.0), LiteralExpression.of(4))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(2.5, (Double) context.getVariable("result").get(), 0.0001);
        }
    }

    @Nested
    class MinusEvaluatorTests {

        /**
         * Tests integer subtraction.
         * <pre>
         * var result: Int = 10 - 3  // result = 7
         * </pre>
         */
        @Test
        void testMinus_IntMinusInt() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new Minus(LiteralExpression.of(10), LiteralExpression.of(3))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(7, context.getVariable("result").get());
        }

        /**
         * Tests double subtraction preserves decimal precision.
         * <pre>
         * var result: Double = 10.5 - 3.2  // result = 7.3
         * </pre>
         */
        @Test
        void testMinus_DoubleMinusDouble() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Minus(LiteralExpression.of(10.5), LiteralExpression.of(3.2))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(7.3, (Double) context.getVariable("result").get(), 0.0001);
        }

        /**
         * Tests mixed type subtraction (Int - Double) promotes to Double.
         * <pre>
         * var result: Double = 10 - 3.5  // result = 6.5
         * </pre>
         */
        @Test
        void testMinus_IntMinusDouble() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Minus(LiteralExpression.of(10), LiteralExpression.of(3.5))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(6.5, (Double) context.getVariable("result").get(), 0.0001);
        }

        /**
         * Tests mixed type subtraction (Double - Int) promotes to Double.
         * <pre>
         * var result: Double = 10.5 - 3  // result = 7.5
         * </pre>
         */
        @Test
        void testMinus_DoubleMinusInt() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Minus(LiteralExpression.of(10.5), LiteralExpression.of(3))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(7.5, (Double) context.getVariable("result").get(), 0.0001);
        }
    }

    @Nested
    class MultiEvaluatorTests {

        /**
         * Tests integer multiplication.
         * <pre>
         * var result: Int = 5 * 3  // result = 15
         * </pre>
         */
        @Test
        void testMulti_IntTimesInt() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new Multi(LiteralExpression.of(5), LiteralExpression.of(3))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(15, context.getVariable("result").get());
        }

        /**
         * Tests double multiplication.
         * <pre>
         * var result: Double = 2.5 * 4.0  // result = 10.0
         * </pre>
         */
        @Test
        void testMulti_DoubleTimesDouble() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Multi(LiteralExpression.of(2.5), LiteralExpression.of(4.0))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(10.0, (Double) context.getVariable("result").get(), 0.0001);
        }

        /**
         * Tests mixed type multiplication (Int * Double) promotes to Double.
         * <pre>
         * var result: Double = 5 * 2.5  // result = 12.5
         * </pre>
         */
        @Test
        void testMulti_IntTimesDouble() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Multi(LiteralExpression.of(5), LiteralExpression.of(2.5))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(12.5, (Double) context.getVariable("result").get(), 0.0001);
        }

        /**
         * Tests mixed type multiplication (Double * Int) promotes to Double.
         * <pre>
         * var result: Double = 2.5 * 4  // result = 10.0
         * </pre>
         */
        @Test
        void testMulti_DoubleTimesInt() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Multi(LiteralExpression.of(2.5), LiteralExpression.of(4))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(10.0, (Double) context.getVariable("result").get(), 0.0001);
        }
    }

    @Nested
    class ModEvaluatorTests {

        /**
         * Tests integer modulo operation.
         * <pre>
         * var result: Int = 10 % 3  // result = 1
         * </pre>
         */
        @Test
        void testMod_IntModInt() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new Mod(LiteralExpression.of(10), LiteralExpression.of(3))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(1, context.getVariable("result").get());
        }

        /**
         * Tests double modulo preserves decimal precision.
         * <pre>
         * var result: Double = 10.5 % 3.0  // result = 1.5
         * </pre>
         */
        @Test
        void testMod_DoubleModDouble() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Mod(LiteralExpression.of(10.5), LiteralExpression.of(3.0))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(1.5, (Double) context.getVariable("result").get(), 0.0001);
        }

        /**
         * Tests mixed type modulo (Int % Double) promotes to Double.
         * <pre>
         * var result: Double = 10 % 3.0  // result = 1.0
         * </pre>
         */
        @Test
        void testMod_IntModDouble() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Mod(LiteralExpression.of(10), LiteralExpression.of(3.0))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(1.0, (Double) context.getVariable("result").get(), 0.0001);
        }

        /**
         * Tests mixed type modulo (Double % Int) promotes to Double.
         * <pre>
         * var result: Double = 10.5 % 3  // result = 1.5
         * </pre>
         */
        @Test
        void testMod_DoubleModInt() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Mod(LiteralExpression.of(10.5), LiteralExpression.of(3))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(1.5, (Double) context.getVariable("result").get(), 0.0001);
        }
    }

    @Nested
    class PlusEvaluatorTests {

        /**
         * Tests integer addition.
         * <pre>
         * var result: Int = 5 + 3  // result = 8
         * </pre>
         */
        @Test
        void testPlus_IntPlusInt() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new Plus(LiteralExpression.of(5), LiteralExpression.of(3))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(8, context.getVariable("result").get());
        }

        /**
         * Tests double addition.
         * <pre>
         * var result: Double = 2.5 + 3.5  // result = 6.0
         * </pre>
         */
        @Test
        void testPlus_DoublePlusDouble() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Plus(LiteralExpression.of(2.5), LiteralExpression.of(3.5))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(6.0, (Double) context.getVariable("result").get(), 0.0001);
        }

        /**
         * Tests mixed type addition (Int + Double) promotes to Double.
         * <pre>
         * var result: Double = 5 + 2.5  // result = 7.5
         * </pre>
         */
        @Test
        void testPlus_IntPlusDouble() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Plus(LiteralExpression.of(5), LiteralExpression.of(2.5))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(7.5, (Double) context.getVariable("result").get(), 0.0001);
        }

        /**
         * Tests string concatenation using plus operator.
         * <pre>
         * var result: String = "Hello, " + "World!"  // result = "Hello, World!"
         * </pre>
         */
        @Test
        void testPlus_StringConcat() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.STRING,
                new Plus(LiteralExpression.of("Hello, "), LiteralExpression.of("World!"))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals("Hello, World!", context.getVariable("result").get());
        }

        /**
         * Tests mixed type addition (Double + Int) promotes to Double.
         * <pre>
         * var result: Double = 2.5 + 3  // result = 5.5
         * </pre>
         */
        @Test
        void testPlus_DoublePlusInt() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new Plus(LiteralExpression.of(2.5), LiteralExpression.of(3))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(5.5, (Double) context.getVariable("result").get(), 0.0001);
        }

        /**
         * Tests string concatenation with integer converts int to string.
         * <pre>
         * var result: String = "Value: " + 42  // result = "Value: 42"
         * </pre>
         */
        @Test
        void testPlus_StringPlusInt() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.STRING,
                new Plus(LiteralExpression.of("Value: "), LiteralExpression.of(42))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals("Value: 42", context.getVariable("result").get());
        }
    }

    @Nested
    class UnsupportedOperationTests {

        /**
         * Tests that dividing a boolean throws UnsupportedOperationException.
         * <pre>
         * var result = true / 2  // throws UnsupportedOperationException
         * </pre>
         */
        @Test
        void testDivide_BooleanThrowsException() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.ANY,
                new Divide(LiteralExpression.of(true), LiteralExpression.of(2))
            ));

            assertThrows(UnsupportedOperationException.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }

        /**
         * Tests that subtracting from a string throws an exception.
         * <pre>
         * var result = "hello" - 2  // throws Exception
         * </pre>
         */
        @Test
        void testMinus_StringThrowsException() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.ANY,
                new Minus(LiteralExpression.of("hello"), LiteralExpression.of(2))
            ));

            assertThrows(Exception.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }

        /**
         * Tests that multiplying a boolean throws UnsupportedOperationException.
         * <pre>
         * var result = true * 2  // throws UnsupportedOperationException
         * </pre>
         */
        @Test
        void testMulti_BooleanThrowsException() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.ANY,
                new Multi(LiteralExpression.of(true), LiteralExpression.of(2))
            ));

            assertThrows(UnsupportedOperationException.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }

        /**
         * Tests that modulo on a boolean throws UnsupportedOperationException.
         * <pre>
         * var result = true % 2  // throws UnsupportedOperationException
         * </pre>
         */
        @Test
        void testMod_BooleanThrowsException() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.ANY,
                new Mod(LiteralExpression.of(true), LiteralExpression.of(2))
            ));

            assertThrows(UnsupportedOperationException.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }

        /**
         * Tests that adding boolean to boolean throws UnsupportedOperationException.
         * <pre>
         * var result = true + false  // throws UnsupportedOperationException
         * </pre>
         */
        @Test
        void testPlus_BooleanPlusBooleanThrowsException() {
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.ANY,
                new Plus(LiteralExpression.of(true), LiteralExpression.of(false))
            ));

            assertThrows(UnsupportedOperationException.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }
    }
}
