package com.elminster.jcp.compile.operator.assignment;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.operation.AssignmentExpression;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.expression.operation.operator.AssignmentOperator;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for assignment expression compilation.
 */
public class AssignmentCompilerTest extends AbstractCompileTest {

    /**
     * Tests simple variable assignment.
     * <pre>
     * int x = 10
     * x = 20
     * return x  // => 20
     * </pre>
     */
    @Test
    void testSimpleAssignment() throws Exception {
        Block program = new BlockImpl();

        // Declare variable
        program.addStatement(new VariableDeclarationImpl("x", SystemDataType.INT,
            LiteralExpression.of(IntLiteral.of(10))));

        // Assign new value
        AssignmentExpression assignment = new AssignmentExpression(
            Identifier.fromName("x"),
            AssignmentOperator.ASSIGNMENT,
            LiteralExpression.of(IntLiteral.of(20))
        );
        program.addStatement(ExpressionStatement.of(assignment));

        String className = uniqueClassName("TestSimpleAssign");
        Class<?> clazz = compileAndLoadWithReturn(program,
            new VariableExpression(Identifier.fromName("x")), SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(20, result);
    }

    /**
     * Tests assignment with expression value.
     * <pre>
     * int x = 5
     * x = x + 10
     * return x  // => 15
     * </pre>
     */
    @Test
    void testAssignmentWithExpression() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("x", SystemDataType.INT,
            LiteralExpression.of(IntLiteral.of(5))));

        // x = x + 10
        AssignmentExpression assignment = new AssignmentExpression(
            Identifier.fromName("x"),
            AssignmentOperator.ASSIGNMENT,
            new Plus(
                new VariableExpression(Identifier.fromName("x")),
                LiteralExpression.of(IntLiteral.of(10))
            )
        );
        program.addStatement(ExpressionStatement.of(assignment));

        String className = uniqueClassName("TestAssignExpr");
        Class<?> clazz = compileAndLoadWithReturn(program,
            new VariableExpression(Identifier.fromName("x")), SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(15, result);
    }

    /**
     * Tests double variable assignment.
     * <pre>
     * double d = 3.14
     * d = 2.71
     * return d  // => 2.71
     * </pre>
     */
    @Test
    void testDoubleAssignment() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("d", SystemDataType.DOUBLE,
            LiteralExpression.of(3.14)));

        AssignmentExpression assignment = new AssignmentExpression(
            Identifier.fromName("d"),
            AssignmentOperator.ASSIGNMENT,
            LiteralExpression.of(2.71)
        );
        program.addStatement(ExpressionStatement.of(assignment));

        String className = uniqueClassName("TestDoubleAssign");
        Class<?> clazz = compileAndLoadWithReturn(program,
            new VariableExpression(Identifier.fromName("d")), SystemDataType.DOUBLE, className);

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(2.71, result, 0.001);
    }

    /**
     * Tests boolean variable assignment.
     * <pre>
     * boolean b = true
     * b = false
     * return b  // => false
     * </pre>
     */
    @Test
    void testBooleanAssignment() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("b", SystemDataType.BOOLEAN,
            LiteralExpression.of(true)));

        AssignmentExpression assignment = new AssignmentExpression(
            Identifier.fromName("b"),
            AssignmentOperator.ASSIGNMENT,
            LiteralExpression.of(false)
        );
        program.addStatement(ExpressionStatement.of(assignment));

        String className = uniqueClassName("TestBoolAssign");
        Class<?> clazz = compileAndLoadWithReturn(program,
            new VariableExpression(Identifier.fromName("b")), SystemDataType.BOOLEAN, className);

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertFalse(result);
    }

    /**
     * Tests that assigning to undefined variable throws CompileException.
     */
    @Test
    void testUndefinedVariableThrows() {
        Block program = new BlockImpl();

        // Assignment to undefined variable
        AssignmentExpression assignment = new AssignmentExpression(
            Identifier.fromName("undefined"),
            AssignmentOperator.ASSIGNMENT,
            LiteralExpression.of(IntLiteral.of(10))
        );
        program.addStatement(ExpressionStatement.of(assignment));

        String className = uniqueClassName("TestUndefined");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        assertThrows(CompileException.class, () -> {
            generator.compile(program);
        });
    }

    /**
     * Tests multiple sequential assignments.
     * <pre>
     * int x = 1
     * x = 2
     * x = 3
     * x = 4
     * return x  // => 4
     * </pre>
     */
    @Test
    void testMultipleAssignments() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("x", SystemDataType.INT,
            LiteralExpression.of(IntLiteral.of(1))));

        for (int i = 2; i <= 4; i++) {
            AssignmentExpression assignment = new AssignmentExpression(
                Identifier.fromName("x"),
                AssignmentOperator.ASSIGNMENT,
                LiteralExpression.of(IntLiteral.of(i))
            );
            program.addStatement(ExpressionStatement.of(assignment));
        }

        String className = uniqueClassName("TestMultiAssign");
        Class<?> clazz = compileAndLoadWithReturn(program,
            new VariableExpression(Identifier.fromName("x")), SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(4, result);
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
