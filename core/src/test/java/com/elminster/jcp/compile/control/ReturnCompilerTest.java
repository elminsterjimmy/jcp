package com.elminster.jcp.compile.control;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclaration;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReturnCompiler - return statement bytecode generation.
 */
public class ReturnCompilerTest extends AbstractCompileTest {

    /**
     * Tests void function returning a value throws CompileException.
     * <pre>
     * fn voidFunc() -> Void { return 42; }  // ERROR: void cannot return value
     * </pre>
     */
    @Test
    void testVoidFunctionReturningValueThrowsException() {
        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(LiteralExpression.of(42)));

        FunctionDeclaration voidFunc = new FunctionDeclarationImpl(
            Identifier.fromName("voidFunc"),
            SystemDataType.VOID,
            new ParameterDef[]{},
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(voidFunc);

        String className = uniqueClassName("TestVoidReturnsValue");

        assertThrows(CompileException.class, () ->
            compiler.compileToBytes(program, className)
        );
    }


    /**
     * Tests return with double value.
     * <pre>
     * fn getDouble() -> Double { return 3.14; }
     * return getDouble()  // returns 3.14
     * </pre>
     */
    @Test
    void testReturnDoubleValue() throws Exception {
        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(LiteralExpression.of(3.14)));

        FunctionDeclaration getDoubleFunc = new FunctionDeclarationImpl(
            Identifier.fromName("getDouble"),
            SystemDataType.DOUBLE,
            new ParameterDef[]{},
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(getDoubleFunc);

        com.elminster.jcp.ast.expression.base.FunctionCallExpression call =
            new com.elminster.jcp.ast.expression.base.FunctionCallExpression(
                Identifier.fromName("getDouble")
            );

        String className = uniqueClassName("TestReturnDouble");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.DOUBLE);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(3.14, result, 0.001);
    }

    /**
     * Tests return with boolean value.
     * <pre>
     * fn getTrue() -> Boolean { return true; }
     * return getTrue()  // returns true
     * </pre>
     */
    @Test
    void testReturnBooleanValue() throws Exception {
        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(LiteralExpression.of(true)));

        FunctionDeclaration getTrueFunc = new FunctionDeclarationImpl(
            Identifier.fromName("getTrue"),
            SystemDataType.BOOLEAN,
            new ParameterDef[]{},
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(getTrueFunc);

        com.elminster.jcp.ast.expression.base.FunctionCallExpression call =
            new com.elminster.jcp.ast.expression.base.FunctionCallExpression(
                Identifier.fromName("getTrue")
            );

        String className = uniqueClassName("TestReturnBoolean");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.BOOLEAN);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }

    /**
     * Tests return with string value.
     * <pre>
     * fn getMessage() -> String { return "hello"; }
     * return getMessage()  // returns "hello"
     * </pre>
     */
    @Test
    void testReturnStringValue() throws Exception {
        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(LiteralExpression.of("hello")));

        FunctionDeclaration getMessageFunc = new FunctionDeclarationImpl(
            Identifier.fromName("getMessage"),
            SystemDataType.STRING,
            new ParameterDef[]{},
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(getMessageFunc);

        com.elminster.jcp.ast.expression.base.FunctionCallExpression call =
            new com.elminster.jcp.ast.expression.base.FunctionCallExpression(
                Identifier.fromName("getMessage")
            );

        String className = uniqueClassName("TestReturnString");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.STRING);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        String result = (String) evaluate.invoke(null);
        assertEquals("hello", result);
    }


    /**
     * Tests non-void function with no return value throws CompileException.
     * This test exercises the returnType == null branch by calling ReturnCompiler
     * outside a function context (no return type set in context).
     */
    @Test
    void testReturnOutsideFunctionContextThrowsException() {
        // Compile a return statement directly in main (no function context = null returnType)
        // We can't do this via the normal compiler path easily, so we test it via
        // a return statement at the top level where there's no function context
        // Actually, the main method context has null returnType which triggers the exception
        Block program = new BlockImpl();
        program.addStatement(new ReturnStatement(LiteralExpression.of(42)));

        String className = uniqueClassName("TestReturnOutsideFunc");

        assertThrows(CompileException.class, () ->
            compiler.compileToBytes(program, className)
        );
    }

    /**
     * Helper method to load a class from bytecode.
     */
    private Class<?> loadClass(String name, byte[] bytecode) {
        return new ClassLoader() {
            public Class<?> defineClass() {
                return defineClass(name, bytecode, 0, bytecode.length);
            }
        }.defineClass();
    }
}
