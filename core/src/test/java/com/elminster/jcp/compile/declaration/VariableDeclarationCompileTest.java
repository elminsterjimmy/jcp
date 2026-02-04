package com.elminster.jcp.compile.declaration;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.DoubleLiteral;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for variable declaration compilation.
 */
public class VariableDeclarationCompileTest extends AbstractCompileTest {

    @Test
    void testSimpleVariableDeclaration() throws Exception {
        // int x = 42;
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(42)
        ));

        byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestSimpleVar"));
        assertNotNull(bytecode);
        assertTrue(bytecode.length > 0);
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

        Class<?> clazz = compiler.compileAndLoad(program, uniqueClassName("TestLoadClass"));
        assertNotNull(clazz);

        Method mainMethod = clazz.getMethod("main", String[].class);
        assertNotNull(mainMethod);

        // Run main (should not throw)
        mainMethod.invoke(null, (Object) new String[]{});
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
    void testDoubleVariableAfterInt() throws Exception {
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
}
