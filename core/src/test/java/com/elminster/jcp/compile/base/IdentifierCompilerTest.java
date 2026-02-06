package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
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
 * Tests for IdentifierCompiler.
 */
public class IdentifierCompilerTest extends AbstractCompileTest {

    /**
     * Tests compiling IdentifierExpression.
     * <pre>
     * int x = 42
     * return x  // IdentifierExpression
     * </pre>
     */
    @Test
    void testIdentifierExpression() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("x", SystemDataType.INT,
            LiteralExpression.of(IntLiteral.of(42))));

        // Return x using IdentifierExpression
        IdentifierExpression idExpr = new IdentifierExpression("x");

        String className = uniqueClassName("TestIdentifierExpr");
        Class<?> clazz = compileAndLoadWithReturn(program, idExpr, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(42, result);
    }

    /**
     * Tests compiling String variable read.
     * <pre>
     * String s = "hello"
     * return s
     * </pre>
     */
    @Test
    void testIdentifierStringType() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("s", SystemDataType.STRING,
            LiteralExpression.of("hello")));

        // Return s
        IdentifierExpression idExpr = new IdentifierExpression("s");

        String className = uniqueClassName("TestIdentifierString");
        Class<?> clazz = compileAndLoadWithReturn(program, idExpr, SystemDataType.STRING, className);

        Method evaluate = clazz.getMethod("evaluate");
        String result = (String) evaluate.invoke(null);
        assertEquals("hello", result);
    }

    /**
     * Tests compiling double variable read.
     * <pre>
     * double d = 3.14
     * return d
     * </pre>
     */
    @Test
    void testIdentifierDoubleType() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("d", SystemDataType.DOUBLE,
            LiteralExpression.of(3.14)));

        // Return d
        IdentifierExpression idExpr = new IdentifierExpression("d");

        String className = uniqueClassName("TestIdentifierDouble");
        Class<?> clazz = compileAndLoadWithReturn(program, idExpr, SystemDataType.DOUBLE, className);

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(3.14, result, 0.001);
    }

    /**
     * Tests compiling boolean variable read.
     * <pre>
     * boolean b = true
     * return b
     * </pre>
     */
    @Test
    void testIdentifierBooleanType() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("b", SystemDataType.BOOLEAN,
            LiteralExpression.of(true)));

        // Return b
        IdentifierExpression idExpr = new IdentifierExpression("b");

        String className = uniqueClassName("TestIdentifierBoolean");
        Class<?> clazz = compileAndLoadWithReturn(program, idExpr, SystemDataType.BOOLEAN, className);

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
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
