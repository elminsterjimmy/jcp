package com.elminster.jcp.compile;

import com.elminster.jcp.ast.Identifier;
import static com.elminster.jcp.ast.Identifier.fromName;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.operation.GreaterThan;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.expression.operation.AssignmentExpression;
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
                fromName("x"),
                SystemDataType.INT,
                new LiteralExpression(new IntLiteral(42))
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
        // int x = 1 + 2 * 3;
        // This will be: int x = 1 + 6 = 7 (if we had proper precedence)
        // For now: int x = (1 + 2) since we're testing just addition
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
                fromName("x"),
                SystemDataType.INT,
                new Plus(
                        new LiteralExpression(new IntLiteral(1)),
                        new LiteralExpression(new IntLiteral(2))
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
                fromName("x"),
                SystemDataType.INT,
                new LiteralExpression(new IntLiteral(10))
        ));

        // if (x > 5)
        Block thenBlock = new BlockImpl();
        thenBlock.addStatement(new ExpressionStatement(
                new AssignmentExpression(
                        new IdentifierExpression(fromName("x")),
                        new LiteralExpression(new IntLiteral(1)),
                        AssignmentOperator.ASSIGN
                )
        ));

        Block elseBlock = new BlockImpl();
        elseBlock.addStatement(new ExpressionStatement(
                new AssignmentExpression(
                        new IdentifierExpression(fromName("x")),
                        new LiteralExpression(new IntLiteral(0)),
                        AssignmentOperator.ASSIGN
                )
        ));

        program.addStatement(new IfElseStatement(
                new GreaterThan(
                        new IdentifierExpression(fromName("x")),
                        new LiteralExpression(new IntLiteral(5))
                ),
                thenBlock,
                elseBlock
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
                fromName("x"),
                SystemDataType.INT,
                new LiteralExpression(new IntLiteral(0))
        ));

        // while body: x = x + 1;
        Block whileBody = new BlockImpl();
        whileBody.addStatement(new ExpressionStatement(
                new AssignmentExpression(
                        new IdentifierExpression(fromName("x")),
                        new Plus(
                                new IdentifierExpression(fromName("x")),
                                new LiteralExpression(new IntLiteral(1))
                        ),
                        AssignmentOperator.ASSIGN
                )
        ));

        // while (x < 10)
        program.addStatement(new WhileStatement(
                new com.elminster.jcp.ast.expression.operation.LessThan(
                        new IdentifierExpression(fromName("x")),
                        new LiteralExpression(new IntLiteral(10))
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
                fromName("x"),
                SystemDataType.INT,
                new LiteralExpression(new IntLiteral(100))
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
