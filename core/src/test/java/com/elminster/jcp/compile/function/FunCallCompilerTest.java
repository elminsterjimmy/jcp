package com.elminster.jcp.compile.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
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
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FunCallCompiler - function call bytecode generation.
 */
public class FunCallCompilerTest extends AbstractCompileTest {

    /**
     * Tests calling undefined function throws CompileException.
     */
    @Test
    void testUndefinedFunctionThrowsException() {
        Block program = new BlockImpl();

        // Call an undefined function
        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("undefinedFunc"),
            LiteralExpression.of(42)
        );

        String className = uniqueClassName("TestUndefinedFunc");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        assertThrows(CompileException.class, () ->
            generator.compileWithReturn(program, call, SystemDataType.INT)
        );
    }

    /**
     * Tests function call with boolean argument.
     * <pre>
     * fn identity(b: Boolean) -> Boolean { return b }
     * return identity(true)  // returns true
     * </pre>
     */
    @Test
    void testFunctionWithBooleanArgument() throws Exception {
        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(
            VariableExpression.of("b")
        ));

        FunctionDeclaration identityFunc = new FunctionDeclarationImpl(
            Identifier.fromName("identity"),
            SystemDataType.BOOLEAN,
            new ParameterDef[]{ParameterDef.of("b", SystemDataType.BOOLEAN)},
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(identityFunc);

        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("identity"),
            LiteralExpression.of(true)
        );

        String className = uniqueClassName("TestBooleanArg");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.BOOLEAN);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }

    /**
     * Tests function call with string argument and return.
     * <pre>
     * fn echo(s: String) -> String { return s }
     * return echo("hello")  // returns "hello"
     * </pre>
     */
    @Test
    void testFunctionWithStringArgument() throws Exception {
        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(
            VariableExpression.of("s")
        ));

        FunctionDeclaration echoFunc = new FunctionDeclarationImpl(
            Identifier.fromName("echo"),
            SystemDataType.STRING,
            new ParameterDef[]{ParameterDef.of("s", SystemDataType.STRING)},
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(echoFunc);

        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("echo"),
            LiteralExpression.of("hello")
        );

        String className = uniqueClassName("TestStringArg");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.STRING);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        String result = (String) evaluate.invoke(null);
        assertEquals("hello", result);
    }

    /**
     * Tests mixed parameter types with type promotion.
     * <pre>
     * fn mixedSum(d: Double, i: Int) -> Double { return d + i }
     * return mixedSum(1.5, 2)  // returns 3.5
     * </pre>
     */
    @Test
    void testMixedParameterTypes() throws Exception {
        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(
            Plus.of(
                VariableExpression.of("d"),
                VariableExpression.of("i")
            )
        ));

        FunctionDeclaration mixedFunc = new FunctionDeclarationImpl(
            Identifier.fromName("mixedSum"),
            SystemDataType.DOUBLE,
            new ParameterDef[]{
                ParameterDef.of("d", SystemDataType.DOUBLE),
                ParameterDef.of("i", SystemDataType.INT)
            },
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(mixedFunc);

        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("mixedSum"),
            LiteralExpression.of(1.5),
            LiteralExpression.of(2)
        );

        String className = uniqueClassName("TestMixedParams");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.DOUBLE);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(3.5, result, 0.001);
    }

    /**
     * Tests function call with no arguments.
     * <pre>
     * fn getAnswer() -> Int { return 42 }
     * return getAnswer()  // returns 42
     * </pre>
     */
    @Test
    void testNoArgumentFunction() throws Exception {
        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(LiteralExpression.of(42)));

        FunctionDeclaration getAnswerFunc = new FunctionDeclarationImpl(
            Identifier.fromName("getAnswer"),
            SystemDataType.INT,
            new ParameterDef[]{},
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(getAnswerFunc);

        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("getAnswer")
        );

        String className = uniqueClassName("TestNoArgs");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.INT);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(42, result);
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
