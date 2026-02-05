package com.elminster.jcp.compile.module;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValueBuffer module type in compile mode.
 * Verifies that ValueBuffer can be constructed and its instance methods called.
 */
public class ValueBufferCompileTest extends AbstractCompileTest {

    @Test
    void testValueBufferNew() throws Exception {
        // ValueBuffer vb = ValueBuffer.new();
        Block program = new BlockImpl();

        FunctionCallExpression ctorCall = new FunctionCallExpression(
            Identifier.fromName("ValueBuffer.new")
        );
        VariableDeclarationImpl vbDecl = new VariableDeclarationImpl(
            "vb",
            new DataTypeImpl("ValueBuffer"),
            ctorCall
        );
        program.addStatement(vbDecl);

        // Compile and run
        String className = uniqueClassName("TestValueBufferNew");
        Class<?> clazz = compiler.compileAndLoad(program, className);
        Method mainMethod = clazz.getMethod("main", String[].class);

        // Should execute without throwing
        assertDoesNotThrow(() -> mainMethod.invoke(null, (Object) new String[]{}));
    }

    @Test
    void testValueBufferLength() throws Exception {
        // ValueBuffer vb = ValueBuffer.new();
        // int len = vb.length();  // Should be 0 for empty buffer
        Block program = new BlockImpl();

        // Create ValueBuffer
        FunctionCallExpression ctorCall = new FunctionCallExpression(
            Identifier.fromName("ValueBuffer.new")
        );
        VariableDeclarationImpl vbDecl = new VariableDeclarationImpl(
            "vb",
            new DataTypeImpl("ValueBuffer"),
            ctorCall
        );
        program.addStatement(vbDecl);

        // Call vb.length()
        MethodCallExpression lengthCall = new MethodCallExpression(
            VariableExpression.of("vb"),
            "length"
        );

        // Compile with return to get the length value
        String className = uniqueClassName("TestValueBufferLength");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, lengthCall, SystemDataType.INT);

        Map<String, byte[]> genClasses = generator.getGeneratedClasses();
        MultiClassLoader loader = new MultiClassLoader();
        for (Map.Entry<String, byte[]> entry : genClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }
        loader.defineClass(className, bytecode);
        Class<?> clazz = loader.loadClass(className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(0, result);  // Empty buffer has length 0
    }
}
