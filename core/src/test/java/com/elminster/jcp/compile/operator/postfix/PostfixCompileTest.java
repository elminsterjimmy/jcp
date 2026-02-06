package com.elminster.jcp.compile.operator.postfix;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.MinusMinus;
import com.elminster.jcp.ast.expression.operation.PlusPlus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for postfix operators (++, --) compilation.
 */
public class PostfixCompileTest extends AbstractCompileTest {

    /**
     * Tests post-increment (x++) with VariableExpression.
     * <pre>
     * int x = 5
     * x++
     * return x  // => 6
     * </pre>
     */
    @Test
    void testPlusPlusWithVariableExpression() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("x", SystemDataType.INT,
            LiteralExpression.of(IntLiteral.of(5))));

        // x++
        PlusPlus plusPlus = new PlusPlus(new VariableExpression(Identifier.fromName("x")));
        program.addStatement(ExpressionStatement.of(plusPlus));

        // return x
        VariableExpression xExpr = new VariableExpression(Identifier.fromName("x"));

        String className = uniqueClassName("TestPlusPlusVarExpr");
        Class<?> clazz = compileAndLoadWithReturn(program, xExpr, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(6, result);
    }

    /**
     * Tests post-increment (x++) with IdentifierExpression.
     * <pre>
     * int y = 10
     * y++
     * return y  // => 11
     * </pre>
     */
    @Test
    void testPlusPlusWithIdentifierExpression() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("y", SystemDataType.INT,
            LiteralExpression.of(IntLiteral.of(10))));

        // y++ using IdentifierExpression
        PlusPlus plusPlus = new PlusPlus(new IdentifierExpression("y"));
        program.addStatement(ExpressionStatement.of(plusPlus));

        // return y
        VariableExpression yExpr = new VariableExpression(Identifier.fromName("y"));

        String className = uniqueClassName("TestPlusPlusIdExpr");
        Class<?> clazz = compileAndLoadWithReturn(program, yExpr, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(11, result);
    }

    /**
     * Tests post-decrement (x--) with VariableExpression.
     * <pre>
     * int x = 10
     * x--
     * return x  // => 9
     * </pre>
     */
    @Test
    void testMinusMinusWithVariableExpression() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("x", SystemDataType.INT,
            LiteralExpression.of(IntLiteral.of(10))));

        // x--
        MinusMinus minusMinus = new MinusMinus(new VariableExpression(Identifier.fromName("x")));
        program.addStatement(ExpressionStatement.of(minusMinus));

        // return x
        VariableExpression xExpr = new VariableExpression(Identifier.fromName("x"));

        String className = uniqueClassName("TestMinusMinusVarExpr");
        Class<?> clazz = compileAndLoadWithReturn(program, xExpr, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(9, result);
    }

    /**
     * Tests post-decrement (x--) with IdentifierExpression.
     * <pre>
     * int y = 20
     * y--
     * return y  // => 19
     * </pre>
     */
    @Test
    void testMinusMinusWithIdentifierExpression() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("y", SystemDataType.INT,
            LiteralExpression.of(IntLiteral.of(20))));

        // y-- using IdentifierExpression
        MinusMinus minusMinus = new MinusMinus(new IdentifierExpression("y"));
        program.addStatement(ExpressionStatement.of(minusMinus));

        // return y
        VariableExpression yExpr = new VariableExpression(Identifier.fromName("y"));

        String className = uniqueClassName("TestMinusMinusIdExpr");
        Class<?> clazz = compileAndLoadWithReturn(program, yExpr, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(19, result);
    }

    /**
     * Helper method to compile with return.
     */
    private Class<?> compileAndLoadWithReturn(Block program, com.elminster.jcp.ast.Expression expression,
                                               DataType returnType, String className) throws Exception {
        com.elminster.jcp.compile.BytecodeGenerator generator = new com.elminster.jcp.compile.BytecodeGenerator(className);
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
