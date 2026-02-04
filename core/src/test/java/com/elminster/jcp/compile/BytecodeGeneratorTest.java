package com.elminster.jcp.compile;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.operation.AssignmentExpression;
import com.elminster.jcp.ast.expression.operation.GreaterThan;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.LessThan;
import com.elminster.jcp.ast.expression.operation.Plus;
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
}
