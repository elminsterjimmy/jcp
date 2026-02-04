package com.elminster.jcp.compile;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.DoubleLiteral;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.operation.*;
import com.elminster.jcp.ast.expression.operation.operator.AssignmentOperator;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.control.IfElseStatement;
import com.elminster.jcp.ast.statement.control.WhileStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BytecodeGenerator.
 */
public class BytecodeGeneratorTest {

    private final JcpCompiler compiler = new JcpCompiler();

    @Test
    void testSimpleVariableDeclaration() throws Exception {
        // int x = 42;
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(42)
        ));

        // Compile and verify no exceptions
        byte[] bytecode = compiler.compileToBytes(program, "TestSimpleVar");
        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);

        // Print bytecode for debugging
        System.out.println("=== TestSimpleVar bytecode ===");
        compiler.printBytecode(bytecode);
    }

    @Test
    void testArithmeticExpression() throws Exception {
        // int x = 1 + 2;
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                new Plus(
                        LiteralExpression.of(1),
                        LiteralExpression.of(2)
                )
        ));

        byte[] bytecode = compiler.compileToBytes(program, "TestArithmetic");
        assertNotNull(bytecode);

        System.out.println("=== TestArithmetic bytecode ===");
        compiler.printBytecode(bytecode);
    }

    @Test
    void testIfElseStatement() throws Exception {
        // int x = 10;
        // if (x > 5) {
        //     x = 1;
        // } else {
        //     x = 0;
        // }
        Block program = new BlockImpl();

        // int x = 10;
        program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(10)
        ));

        // if block: x = 1
        Block ifBlock = new BlockImpl();
        ifBlock.addStatement(ExpressionStatement.of(new AssignmentExpression(
                Identifier.fromName("x"),
                AssignmentOperator.ASSIGNMENT,
                LiteralExpression.of(1)
        )));

        // else block: x = 0
        Block elseBlock = new BlockImpl();
        elseBlock.addStatement(ExpressionStatement.of(new AssignmentExpression(
                Identifier.fromName("x"),
                AssignmentOperator.ASSIGNMENT,
                LiteralExpression.of(0)
        )));

        // if (x > 5)
        program.addStatement(new IfElseStatement(
                ifBlock,
                elseBlock,
                new GreaterThan(
                        IdentifierExpression.of("x"),
                        LiteralExpression.of(5)
                )
        ));

        byte[] bytecode = compiler.compileToBytes(program, "TestIfElse");
        assertNotNull(bytecode);

        System.out.println("=== TestIfElse bytecode ===");
        compiler.printBytecode(bytecode);
    }

    @Test
    void testWhileLoop() throws Exception {
        // int x = 0;
        // while (x < 10) {
        //     x = x + 1;
        // }
        Block program = new BlockImpl();

        // int x = 0;
        program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(0)
        ));

        // while body: x = x + 1;
        Block whileBody = new BlockImpl();
        whileBody.addStatement(ExpressionStatement.of(new AssignmentExpression(
                Identifier.fromName("x"),
                AssignmentOperator.ASSIGNMENT,
                new Plus(
                        IdentifierExpression.of("x"),
                        LiteralExpression.of(1)
                )
        )));

        // while (x < 10)
        program.addStatement(new WhileStatement(
                new LessThan(
                        IdentifierExpression.of("x"),
                        LiteralExpression.of(10)
                ),
                whileBody
        ));

        byte[] bytecode = compiler.compileToBytes(program, "TestWhile");
        assertNotNull(bytecode);

        System.out.println("=== TestWhile bytecode ===");
        compiler.printBytecode(bytecode);
    }

    @Test
    void testCompileAndLoad() throws Exception {
        // int x = 100;
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(100)
        ));

        // Compile and load class
        Class<?> clazz = compiler.compileAndLoad(program, "TestLoadClass");
        assertNotNull(clazz);
        assertEquals("TestLoadClass", clazz.getName());

        // Verify main method exists
        Method mainMethod = clazz.getMethod("main", String[].class);
        assertNotNull(mainMethod);

        // Run main (should not throw)
        mainMethod.invoke(null, (Object) new String[]{});
    }

    // ========== DOUBLE TYPE TESTS WITH VALUE VERIFICATION ==========

    private static int testCounter = 0;
    private String uniqueClassName(String base) {
        return base + "_" + (++testCounter);
    }

    @Test
    void testDoubleAdditionWithValue() throws Exception {
        // return 3.14 + 2.0  => 5.14
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Plus(
                        LiteralExpression.of(DoubleLiteral.of(3.14)),
                        LiteralExpression.of(DoubleLiteral.of(2.0))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleAdd")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(5.14, result, 0.001);
    }

    @Test
    void testDoubleSubtractionWithValue() throws Exception {
        // return 5.5 - 2.3  => 3.2
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Minus(
                        LiteralExpression.of(DoubleLiteral.of(5.5)),
                        LiteralExpression.of(DoubleLiteral.of(2.3))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleSub")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(3.2, result, 0.001);
    }

    @Test
    void testDoubleMultiplicationWithValue() throws Exception {
        // return 2.5 * 4.0  => 10.0
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Multi(
                        LiteralExpression.of(DoubleLiteral.of(2.5)),
                        LiteralExpression.of(DoubleLiteral.of(4.0))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleMul")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(10.0, result, 0.001);
    }

    @Test
    void testDoubleDivisionWithValue() throws Exception {
        // return 10.0 / 4.0  => 2.5
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Divide(
                        LiteralExpression.of(DoubleLiteral.of(10.0)),
                        LiteralExpression.of(DoubleLiteral.of(4.0))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleDiv")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(2.5, result, 0.001);
    }

    @Test
    void testDoubleModuloWithValue() throws Exception {
        // return 10.5 % 3.0  => 1.5
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Mod(
                        LiteralExpression.of(DoubleLiteral.of(10.5)),
                        LiteralExpression.of(DoubleLiteral.of(3.0))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleMod")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(1.5, result, 0.001);
    }

    @Test
    void testMixedIntDoubleAdditionWithValue() throws Exception {
        // int a = 5; return a + 2.5  => 7.5
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("a", SystemDataType.INT, LiteralExpression.of(5)));

        Class<?> clazz = compiler.compileAndLoadWithReturn(
                program,
                new Plus(
                        IdentifierExpression.of("a"),
                        LiteralExpression.of(DoubleLiteral.of(2.5))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestMixedAdd")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(7.5, result, 0.001);
    }

    @Test
    void testDoubleComparisonLessThanWithValue() throws Exception {
        // return 1.5 < 2.5  => true (1)
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LessThan(
                        LiteralExpression.of(DoubleLiteral.of(1.5)),
                        LiteralExpression.of(DoubleLiteral.of(2.5))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleLT")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }

    @Test
    void testDoubleComparisonGreaterThanWithValue() throws Exception {
        // return 3.14 > 2.0  => true (1)
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new GreaterThan(
                        LiteralExpression.of(DoubleLiteral.of(3.14)),
                        LiteralExpression.of(DoubleLiteral.of(2.0))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleGT")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }

    @Test
    void testDoubleComparisonEqualWithValue() throws Exception {
        // return 3.14 == 3.14  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Equal(
                        LiteralExpression.of(DoubleLiteral.of(3.14)),
                        LiteralExpression.of(DoubleLiteral.of(3.14))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleEQ")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }

    @Test
    void testDoubleComparisonNotEqualWithValue() throws Exception {
        // return 3.14 != 2.0  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new NotEqual(
                        LiteralExpression.of(DoubleLiteral.of(3.14)),
                        LiteralExpression.of(DoubleLiteral.of(2.0))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleNE")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }

    @Test
    void testMixedTypeSlotAllocation() throws Exception {
        // Test: int a=1; double b=2.5; int c=3; double d=4.5; int e=5;
        // return e  => 5 (verifies slots are correctly allocated)
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("a", SystemDataType.INT, LiteralExpression.of(1)));
        program.addStatement(new VariableDeclarationImpl("b", SystemDataType.DOUBLE, LiteralExpression.of(DoubleLiteral.of(2.5))));
        program.addStatement(new VariableDeclarationImpl("c", SystemDataType.INT, LiteralExpression.of(3)));
        program.addStatement(new VariableDeclarationImpl("d", SystemDataType.DOUBLE, LiteralExpression.of(DoubleLiteral.of(4.5))));
        program.addStatement(new VariableDeclarationImpl("e", SystemDataType.INT, LiteralExpression.of(5)));

        Class<?> clazz = compiler.compileAndLoadWithReturn(
                program,
                IdentifierExpression.of("e"),
                SystemDataType.INT,
                uniqueClassName("TestSlotAlloc")
        );

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(5, result);
    }

    @Test
    void testDoubleVariableAfterIntWithValue() throws Exception {
        // Test: int a=1; double b=2.5; return b  => 2.5
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("a", SystemDataType.INT, LiteralExpression.of(1)));
        program.addStatement(new VariableDeclarationImpl("b", SystemDataType.DOUBLE, LiteralExpression.of(DoubleLiteral.of(2.5))));

        Class<?> clazz = compiler.compileAndLoadWithReturn(
                program,
                IdentifierExpression.of("b"),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleAfterInt")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(2.5, result, 0.001);
    }

    @Test
    void testComplexDoubleExpressionWithValue() throws Exception {
        // return (3.0 + 2.0) * 4.0 - 10.0 / 2.0  => 5.0 * 4.0 - 5.0 = 15.0
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Minus(
                        new Multi(
                                new Plus(
                                        LiteralExpression.of(DoubleLiteral.of(3.0)),
                                        LiteralExpression.of(DoubleLiteral.of(2.0))
                                ),
                                LiteralExpression.of(DoubleLiteral.of(4.0))
                        ),
                        new Divide(
                                LiteralExpression.of(DoubleLiteral.of(10.0)),
                                LiteralExpression.of(DoubleLiteral.of(2.0))
                        )
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestComplexDouble")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(15.0, result, 0.001);
    }

    @Test
    void testDoubleDivisionByZero() throws Exception {
        // return 5.0 / 0.0  => Infinity
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Divide(
                        LiteralExpression.of(DoubleLiteral.of(5.0)),
                        LiteralExpression.of(DoubleLiteral.of(0.0))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleDivZero")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertTrue(Double.isInfinite(result));
        assertTrue(result > 0);  // Positive infinity
    }

    @Test
    void testDoubleNaN() throws Exception {
        // return 0.0 / 0.0  => NaN
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Divide(
                        LiteralExpression.of(DoubleLiteral.of(0.0)),
                        LiteralExpression.of(DoubleLiteral.of(0.0))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleNaN")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertTrue(Double.isNaN(result));
    }

    @Test
    void testDoubleSpecialConstants() throws Exception {
        // Test DCONST_0 optimization: return 0.0  => 0.0
        Class<?> clazz0 = compiler.compileAndLoadWithReturn(
                null,
                LiteralExpression.of(DoubleLiteral.of(0.0)),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleConst0")
        );
        assertEquals(0.0, (double) clazz0.getMethod("evaluate").invoke(null), 0.001);

        // Test DCONST_1 optimization: return 1.0  => 1.0
        Class<?> clazz1 = compiler.compileAndLoadWithReturn(
                null,
                LiteralExpression.of(DoubleLiteral.of(1.0)),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleConst1")
        );
        assertEquals(1.0, (double) clazz1.getMethod("evaluate").invoke(null), 0.001);
    }
}
