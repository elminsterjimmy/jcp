package com.elminster.jcp.compile.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.operation.Equal;
import com.elminster.jcp.ast.expression.operation.Minus;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.control.IfElseStatement;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclaration;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for function bytecode compilation.
 * Verifies that JCP functions compile correctly to JVM bytecode.
 */
public class FunctionCompileTest extends AbstractCompileTest {

    @Test
    void testSimpleFunctionWithReturn() throws Exception {
        // func add(int a, int b) -> int { return a + b; }
        // return add(1, 2);  => 3

        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(
            new Plus(
                VariableExpression.of("a"),
                VariableExpression.of("b")
            )
        ));

        FunctionDeclaration addFunc = new FunctionDeclarationImpl(
            Identifier.fromName("add"),
            SystemDataType.INT,
            new ParameterDef[]{
                ParameterDef.of("a", SystemDataType.INT),
                ParameterDef.of("b", SystemDataType.INT)
            },
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(addFunc);

        // Call the function
        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("add"),
            LiteralExpression.of(1),
            LiteralExpression.of(2)
        );

        String className = uniqueClassName("TestAdd");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.INT);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(3, result);
    }

    @Test
    void testRecursiveFibonacci() throws Exception {
        // func fibonacci(int n) -> int {
        //   if (n == 1) return 1;
        //   if (n == 2) return 1;
        //   return fibonacci(n - 1) + fibonacci(n - 2);
        // }
        // return fibonacci(10);  => 55

        String fibonacci = "fibonacci";
        Block functionBody = new BlockImpl();
        functionBody
            .addStatement(
                new IfElseStatement(new ReturnStatement(LiteralExpression.of(1)), Equal.of(
                    VariableExpression.of("n"), LiteralExpression.of(1)
                )))
            .addStatement(
                new IfElseStatement(new ReturnStatement(LiteralExpression.of(1)), Equal.of(
                    VariableExpression.of("n"), LiteralExpression.of(2)
                )))
            .addStatement(
                new ReturnStatement(
                    Plus.of(
                        new FunctionCallExpression(Identifier.fromName(fibonacci),
                            Minus.of(VariableExpression.of("n"), LiteralExpression.of(1))),
                        new FunctionCallExpression(Identifier.fromName(fibonacci),
                            Minus.of(VariableExpression.of("n"), LiteralExpression.of(2)))
                    )
                )
            );

        FunctionDeclaration fibFunc = new FunctionDeclarationImpl(
            Identifier.fromName(fibonacci),
            SystemDataType.INT,
            new ParameterDef[]{ParameterDef.of("n", SystemDataType.INT)},
            functionBody
        );

        Block program = new BlockImpl();
        program.addStatement(fibFunc);

        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName(fibonacci),
            LiteralExpression.of(10)
        );

        String className = uniqueClassName("TestFibonacci");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.INT);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(55, result);
    }

    @Test
    void testVoidFunction() throws Exception {
        // func doNothing() -> void { }
        // doNothing();
        // return 42;

        Block funcBody = new BlockImpl();
        // Empty body - implicit void return

        FunctionDeclaration voidFunc = new FunctionDeclarationImpl(
            Identifier.fromName("doNothing"),
            SystemDataType.VOID,
            new ParameterDef[]{},
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(voidFunc);

        // Call void function (result discarded)
        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("doNothing")
        );
        program.addStatement(new com.elminster.jcp.ast.statement.ExpressionStatement(call));

        String className = uniqueClassName("TestVoid");
        byte[] bytecode = compiler.compileToBytes(program, className);
        assertNotNull(bytecode);

        // Should not throw during execution
        Class<?> clazz = loadClass(className, bytecode);
        Method main = clazz.getMethod("main", String[].class);
        main.invoke(null, (Object) new String[]{});
    }

    @Test
    void testFunctionOverloading() throws Exception {
        // func process(int x) -> int { return x; }
        // func process(int x, int y) -> int { return x + y; }
        // return process(5) + process(10, 20);  => 35

        // First overload: process(int)
        Block body1 = new BlockImpl();
        body1.addStatement(new ReturnStatement(VariableExpression.of("x")));

        FunctionDeclaration func1 = new FunctionDeclarationImpl(
            Identifier.fromName("process"),
            SystemDataType.INT,
            new ParameterDef[]{ParameterDef.of("x", SystemDataType.INT)},
            body1
        );

        // Second overload: process(int, int)
        Block body2 = new BlockImpl();
        body2.addStatement(new ReturnStatement(
            Plus.of(VariableExpression.of("x"), VariableExpression.of("y"))
        ));

        FunctionDeclaration func2 = new FunctionDeclarationImpl(
            Identifier.fromName("process"),
            SystemDataType.INT,
            new ParameterDef[]{
                ParameterDef.of("x", SystemDataType.INT),
                ParameterDef.of("y", SystemDataType.INT)
            },
            body2
        );

        Block program = new BlockImpl();
        program.addStatement(func1);
        program.addStatement(func2);

        // Call both overloads
        Expression expr = Plus.of(
            new FunctionCallExpression(Identifier.fromName("process"), LiteralExpression.of(5)),
            new FunctionCallExpression(Identifier.fromName("process"),
                LiteralExpression.of(10), LiteralExpression.of(20))
        );

        String className = uniqueClassName("TestOverload");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, expr, SystemDataType.INT);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(35, result);  // 5 + 30 = 35
    }

    @Test
    void testForwardReference() throws Exception {
        // int x = foo();  // Called before declaration
        // func foo() -> int { return 42; }
        // return x;

        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(LiteralExpression.of(42)));

        FunctionDeclaration fooFunc = new FunctionDeclarationImpl(
            Identifier.fromName("foo"),
            SystemDataType.INT,
            new ParameterDef[]{},
            funcBody
        );

        Block program = new BlockImpl();
        // Variable declaration that calls foo (before foo is declared in source order)
        program.addStatement(new VariableDeclarationImpl(
            "x",
            SystemDataType.INT,
            new FunctionCallExpression(Identifier.fromName("foo"))
        ));
        // Function declaration comes after the call
        program.addStatement(fooFunc);

        // Return x
        String className = uniqueClassName("TestForward");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, VariableExpression.of("x"), SystemDataType.INT);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(42, result);
    }

    @Test
    void testMultipleParameters() throws Exception {
        // func sum4(int a, int b, int c, int d) -> int { return a + b + c + d; }
        // return sum4(1, 2, 3, 4);  => 10

        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(
            Plus.of(
                Plus.of(VariableExpression.of("a"), VariableExpression.of("b")),
                Plus.of(VariableExpression.of("c"), VariableExpression.of("d"))
            )
        ));

        FunctionDeclaration sumFunc = new FunctionDeclarationImpl(
            Identifier.fromName("sum4"),
            SystemDataType.INT,
            new ParameterDef[]{
                ParameterDef.of("a", SystemDataType.INT),
                ParameterDef.of("b", SystemDataType.INT),
                ParameterDef.of("c", SystemDataType.INT),
                ParameterDef.of("d", SystemDataType.INT)
            },
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(sumFunc);

        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("sum4"),
            LiteralExpression.of(1),
            LiteralExpression.of(2),
            LiteralExpression.of(3),
            LiteralExpression.of(4)
        );

        String className = uniqueClassName("TestMultiParams");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.INT);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(10, result);
    }

    @Test
    void testFunctionWithLocalVariable() throws Exception {
        // func compute(int x) -> int {
        //   int y = x + 10;
        //   return y * 2;
        // }
        // return compute(5);  => 30

        Block funcBody = new BlockImpl();
        funcBody.addStatement(new VariableDeclarationImpl(
            "y",
            SystemDataType.INT,
            Plus.of(VariableExpression.of("x"), LiteralExpression.of(10))
        ));
        funcBody.addStatement(new ReturnStatement(
            new com.elminster.jcp.ast.expression.operation.Multi(
                VariableExpression.of("y"),
                LiteralExpression.of(2)
            )
        ));

        FunctionDeclaration computeFunc = new FunctionDeclarationImpl(
            Identifier.fromName("compute"),
            SystemDataType.INT,
            new ParameterDef[]{ParameterDef.of("x", SystemDataType.INT)},
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(computeFunc);

        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("compute"),
            LiteralExpression.of(5)
        );

        String className = uniqueClassName("TestLocalVar");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.INT);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(30, result);  // (5 + 10) * 2 = 30
    }

    @Test
    void testNestedFunctionCalls() throws Exception {
        // func double(int x) -> int { return x * 2; }
        // func triple(int x) -> int { return x * 3; }
        // return triple(double(5));  => 30

        Block doubleBody = new BlockImpl();
        doubleBody.addStatement(new ReturnStatement(
            new com.elminster.jcp.ast.expression.operation.Multi(
                VariableExpression.of("x"),
                LiteralExpression.of(2)
            )
        ));

        Block tripleBody = new BlockImpl();
        tripleBody.addStatement(new ReturnStatement(
            new com.elminster.jcp.ast.expression.operation.Multi(
                VariableExpression.of("x"),
                LiteralExpression.of(3)
            )
        ));

        FunctionDeclaration doubleFunc = new FunctionDeclarationImpl(
            Identifier.fromName("double_it"),
            SystemDataType.INT,
            new ParameterDef[]{ParameterDef.of("x", SystemDataType.INT)},
            doubleBody
        );

        FunctionDeclaration tripleFunc = new FunctionDeclarationImpl(
            Identifier.fromName("triple_it"),
            SystemDataType.INT,
            new ParameterDef[]{ParameterDef.of("x", SystemDataType.INT)},
            tripleBody
        );

        Block program = new BlockImpl();
        program.addStatement(doubleFunc);
        program.addStatement(tripleFunc);

        // triple(double(5))
        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("triple_it"),
            new FunctionCallExpression(
                Identifier.fromName("double_it"),
                LiteralExpression.of(5)
            )
        );

        String className = uniqueClassName("TestNested");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.INT);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(30, result);  // triple(double(5)) = triple(10) = 30
    }

    /**
     * Tests function with Double parameter and return type.
     * <pre>
     * fn doubleIt(x: Double) -> Double { return x * 2.0 }
     * return doubleIt(3.14)  // returns 6.28
     * </pre>
     */
    @Test
    void testFunctionWithDouble() throws Exception {
        Block funcBody = new BlockImpl();
        funcBody.addStatement(new ReturnStatement(
            new com.elminster.jcp.ast.expression.operation.Multi(
                VariableExpression.of("x"),
                LiteralExpression.of(2.0)
            )
        ));

        FunctionDeclaration doubleFunc = new FunctionDeclarationImpl(
            Identifier.fromName("doubleIt"),
            SystemDataType.DOUBLE,
            new ParameterDef[]{ParameterDef.of("x", SystemDataType.DOUBLE)},
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(doubleFunc);

        FunctionCallExpression call = new FunctionCallExpression(
            Identifier.fromName("doubleIt"),
            LiteralExpression.of(3.0)
        );

        String className = uniqueClassName("TestDouble");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.DOUBLE);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(6.0, result, 0.001);
    }

    /**
     * Tests function with boolean parameter that returns the same value.
     * <pre>
     * fn identity(b: Boolean) -> Boolean { return b }
     * return identity(true)  // returns true
     * </pre>
     */
    @Test
    void testFunctionWithBoolean() throws Exception {
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

        String className = uniqueClassName("TestBoolean");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] bytecode = generator.compileWithReturn(program, call, SystemDataType.BOOLEAN);

        Class<?> clazz = loadClass(className, bytecode);
        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }

    /**
     * Helper method to load a class from bytecode using a custom class loader.
     */
    private Class<?> loadClass(String name, byte[] bytecode) {
        return new ClassLoader() {
            public Class<?> defineClass() {
                return defineClass(name, bytecode, 0, bytecode.length);
            }
        }.defineClass();
    }
}
