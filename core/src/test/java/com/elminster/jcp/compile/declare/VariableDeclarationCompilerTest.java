package com.elminster.jcp.compile.declare;

import com.elminster.jcp.ast.expression.base.VariableExpression;
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
 * Tests for VariableDeclarationCompiler.
 */
public class VariableDeclarationCompilerTest extends AbstractCompileTest {

    @Nested
    class DefaultValueTests {

        /**
         * Tests default value initialization for Int variable.
         * <pre>
         * var x: Int
         * return x  // returns 0
         * </pre>
         */
        @Test
        void testDefaultValue_Int() throws Exception {
            Block program = new BlockImpl();
            // Declare without initializer
            program.addStatement(new VariableDeclarationImpl("x", SystemDataType.INT, null));

            String className = uniqueClassName("DefaultInt");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            byte[] bytecode = generator.compileWithReturn(program, VariableExpression.of("x"), SystemDataType.INT);

            Class<?> clazz = loadClass(className, bytecode);
            Method evaluate = clazz.getMethod("evaluate");
            int result = (int) evaluate.invoke(null);
            assertEquals(0, result);
        }

        /**
         * Tests default value initialization for Boolean variable.
         * <pre>
         * var b: Boolean
         * return b  // returns false
         * </pre>
         */
        @Test
        void testDefaultValue_Boolean() throws Exception {
            Block program = new BlockImpl();
            // Declare without initializer
            program.addStatement(new VariableDeclarationImpl("b", SystemDataType.BOOLEAN, null));

            String className = uniqueClassName("DefaultBool");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            byte[] bytecode = generator.compileWithReturn(program, VariableExpression.of("b"), SystemDataType.BOOLEAN);

            Class<?> clazz = loadClass(className, bytecode);
            Method evaluate = clazz.getMethod("evaluate");
            boolean result = (boolean) evaluate.invoke(null);
            assertFalse(result);
        }

        /**
         * Tests default value initialization for String variable.
         * <pre>
         * var s: String
         * return s  // returns ""
         * </pre>
         */
        @Test
        void testDefaultValue_String() throws Exception {
            Block program = new BlockImpl();
            // Declare without initializer
            program.addStatement(new VariableDeclarationImpl("s", SystemDataType.STRING, null));

            String className = uniqueClassName("DefaultString");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            byte[] bytecode = generator.compileWithReturn(program, VariableExpression.of("s"), SystemDataType.STRING);

            Class<?> clazz = loadClass(className, bytecode);
            Method evaluate = clazz.getMethod("evaluate");
            String result = (String) evaluate.invoke(null);
            assertEquals("", result);
        }

        /**
         * Tests default value initialization for Any/Object variable.
         * <pre>
         * var o: Any
         * return o  // returns null
         * </pre>
         */
        @Test
        void testDefaultValue_Any() throws Exception {
            Block program = new BlockImpl();
            // Declare without initializer
            program.addStatement(new VariableDeclarationImpl("o", SystemDataType.ANY, null));

            String className = uniqueClassName("DefaultAny");
            BytecodeGenerator generator = new BytecodeGenerator(className);
            byte[] bytecode = generator.compileWithReturn(program, VariableExpression.of("o"), SystemDataType.ANY);

            Class<?> clazz = loadClass(className, bytecode);
            Method evaluate = clazz.getMethod("evaluate");
            Object result = evaluate.invoke(null);
            assertNull(result);
        }
    }

    private Class<?> loadClass(String name, byte[] bytecode) {
        return new ClassLoader() {
            public Class<?> defineClass() {
                return defineClass(name, bytecode, 0, bytecode.length);
            }
        }.defineClass();
    }
}
