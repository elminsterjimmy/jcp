package com.elminster.jcp.compile.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.compile.util.CompileModeClassConverter;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for type promotion and boxing in compiled code.
 */
public class ExternalClassCompileTest extends AbstractCompileTest {

    /**
     * Tests CompileModeClassConverter type mapping.
     * <pre>
     * int.class -> INT
     * double.class -> DOUBLE
     * boolean.class -> BOOLEAN
     * String.class -> STRING
     * </pre>
     */
    @Test
    void testMapJavaTypeToDataType() {
        assertEquals(SystemDataType.INT, CompileModeClassConverter.mapJavaTypeToDataType(int.class));
        assertEquals(SystemDataType.INT, CompileModeClassConverter.mapJavaTypeToDataType(Integer.class));
        assertEquals(SystemDataType.DOUBLE, CompileModeClassConverter.mapJavaTypeToDataType(double.class));
        assertEquals(SystemDataType.DOUBLE, CompileModeClassConverter.mapJavaTypeToDataType(Double.class));
        assertEquals(SystemDataType.BOOLEAN, CompileModeClassConverter.mapJavaTypeToDataType(boolean.class));
        assertEquals(SystemDataType.BOOLEAN, CompileModeClassConverter.mapJavaTypeToDataType(Boolean.class));
        assertEquals(SystemDataType.STRING, CompileModeClassConverter.mapJavaTypeToDataType(String.class));
        assertEquals(SystemDataType.VOID, CompileModeClassConverter.mapJavaTypeToDataType(void.class));
        assertEquals(SystemDataType.VOID, CompileModeClassConverter.mapJavaTypeToDataType(Void.class));
        assertEquals(SystemDataType.ANY, CompileModeClassConverter.mapJavaTypeToDataType(Object.class));
    }

    /**
     * Tests array type mapping.
     * <pre>
     * int[].class -> INT_ARRAY
     * double[].class -> DOUBLE_ARRAY
     * boolean[].class -> BOOLEAN_ARRAY
     * String[].class -> STRING_ARRAY
     * </pre>
     */
    @Test
    void testMapJavaArrayTypeToDataType() {
        assertEquals(SystemDataType.INT_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(int[].class));
        assertEquals(SystemDataType.DOUBLE_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(double[].class));
        assertEquals(SystemDataType.BOOLEAN_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(boolean[].class));
        assertEquals(SystemDataType.STRING_ARRAY, CompileModeClassConverter.mapJavaTypeToDataType(String[].class));
    }

    /**
     * Tests int + int returns int type.
     * <pre>
     * int a = 5
     * int b = 10
     * return a + b  // => 15
     * </pre>
     */
    @Test
    void testIntPlusInt() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("a", SystemDataType.INT, LiteralExpression.of(IntLiteral.of(5))));
        program.addStatement(new VariableDeclarationImpl("b", SystemDataType.INT, LiteralExpression.of(IntLiteral.of(10))));

        Plus sum = new Plus(
            new VariableExpression(Identifier.fromName("a")),
            new VariableExpression(Identifier.fromName("b"))
        );

        String className = uniqueClassName("TestIntPlusInt");
        Class<?> clazz = compileAndLoadWithReturn(program, sum, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(15, result);
    }

    /**
     * Tests double + double returns double.
     * <pre>
     * double a = 5.5
     * double b = 10.3
     * return a + b  // => 15.8
     * </pre>
     */
    @Test
    void testDoublePlusDouble() throws Exception {
        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl("a", SystemDataType.DOUBLE, LiteralExpression.of(5.5)));
        program.addStatement(new VariableDeclarationImpl("b", SystemDataType.DOUBLE, LiteralExpression.of(10.3)));

        Plus sum = new Plus(
            new VariableExpression(Identifier.fromName("a")),
            new VariableExpression(Identifier.fromName("b"))
        );

        String className = uniqueClassName("TestDoublePlusDouble");
        Class<?> clazz = compileAndLoadWithReturn(program, sum, SystemDataType.DOUBLE, className);

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(15.8, result, 0.001);
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
