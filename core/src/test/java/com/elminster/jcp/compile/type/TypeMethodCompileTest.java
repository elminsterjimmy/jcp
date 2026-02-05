package com.elminster.jcp.compile.type;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.FieldAccessExpression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.StructInstantiation;
import com.elminster.jcp.ast.expression.ThisExpression;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.ast.statement.declaration.MethodDef;
import com.elminster.jcp.ast.statement.declaration.StructDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for type method bytecode compilation.
 * Verifies that instance methods and static methods compile correctly to JVM bytecode.
 */
public class TypeMethodCompileTest extends AbstractCompileTest {

    @Test
    void testStaticMethodCompilation() throws Exception {
        // type Math {
        //   static func add(int a, int b) -> int { return a + b; }
        // }
        // return Math.add(2, 3);  => 5

        Block program = new BlockImpl();

        // Static method body: return a + b;
        Block methodBody = new BlockImpl();
        methodBody.addStatement(new ReturnStatement(
            new Plus(
                VariableExpression.of("a"),
                VariableExpression.of("b")
            )
        ));

        MethodDef addMethod = MethodDef.staticMethod(
            "add",
            SystemDataType.INT,
            methodBody,
            ParameterDef.of("a", SystemDataType.INT),
            ParameterDef.of("b", SystemDataType.INT)
        );

        // Type declaration with static method only
        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Math",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Arrays.asList(addMethod)
        );
        program.addStatement(typeDecl);

        // Call static method: Math.add(2, 3)
        StaticMethodCallExpression methodCall = new StaticMethodCallExpression(
            "Math",
            "add",
            LiteralExpression.of(2),
            LiteralExpression.of(3)
        );

        // Compile and execute
        String className = uniqueClassName("TestStaticMethod");
        Class<?> clazz = compileAndLoadWithReturn(program, methodCall, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(5, result);
    }

    @Test
    void testInstanceMethodCompilation() throws Exception {
        // type Counter {
        //   int count;
        //   func getCount() -> int { return this.count; }
        // }
        // Counter c = Counter(42);
        // return c.getCount();  => 42

        Block program = new BlockImpl();

        // Instance method body: return this.count;
        Block methodBody = new BlockImpl();
        methodBody.addStatement(new ReturnStatement(
            new FieldAccessExpression(
                new ThisExpression(),
                "count"
            )
        ));

        MethodDef getCountMethod = new MethodDef(
            "getCount",
            SystemDataType.INT,
            methodBody
        );

        // Type declaration with instance method
        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Counter",
            Arrays.asList(new StructFieldDef("count", SystemDataType.INT)),
            null,
            Arrays.asList(getCountMethod),
            Collections.emptyList()
        );
        program.addStatement(typeDecl);

        // Counter c = Counter(42);
        StructInstantiation inst = new StructInstantiation("Counter",
            LiteralExpression.of(42)
        );
        VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
            "c",
            new DataTypeImpl("Counter"),
            inst
        );
        program.addStatement(varDecl);

        // Call instance method: c.getCount()
        MethodCallExpression methodCall = new MethodCallExpression(
            VariableExpression.of("c"),
            "getCount"
        );

        // Compile and execute
        String className = uniqueClassName("TestInstanceMethod");
        Class<?> clazz = compileAndLoadWithReturn(program, methodCall, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(42, result);
    }

    @Test
    void testInstanceMethodWithParameters() throws Exception {
        // type Calculator {
        //   int base;
        //   func add(int x) -> int { return this.base + x; }
        // }
        // Calculator c = Calculator(10);
        // return c.add(5);  => 15

        Block program = new BlockImpl();

        // Instance method body: return this.base + x;
        Block methodBody = new BlockImpl();
        methodBody.addStatement(new ReturnStatement(
            new Plus(
                new FieldAccessExpression(new ThisExpression(), "base"),
                VariableExpression.of("x")
            )
        ));

        MethodDef addMethod = new MethodDef(
            "add",
            SystemDataType.INT,
            methodBody,
            ParameterDef.of("x", SystemDataType.INT)
        );

        // Type declaration
        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Calculator",
            Arrays.asList(new StructFieldDef("base", SystemDataType.INT)),
            null,
            Arrays.asList(addMethod),
            Collections.emptyList()
        );
        program.addStatement(typeDecl);

        // Calculator c = Calculator(10);
        StructInstantiation inst = new StructInstantiation("Calculator",
            LiteralExpression.of(10)
        );
        VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
            "c",
            new DataTypeImpl("Calculator"),
            inst
        );
        program.addStatement(varDecl);

        // c.add(5)
        MethodCallExpression methodCall = new MethodCallExpression(
            VariableExpression.of("c"),
            "add",
            LiteralExpression.of(5)
        );

        // Compile and execute
        String className = uniqueClassName("TestInstanceMethodParams");
        Class<?> clazz = compileAndLoadWithReturn(program, methodCall, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(15, result);
    }

    @Test
    void testTypeClassWithMethodsGenerated() throws Exception {
        // Verify that type classes with methods are properly generated
        // type Point {
        //   int x;
        //   func getX() -> int { return this.x; }
        //   static func zero() -> int { return 0; }
        // }

        Block program = new BlockImpl();

        // Instance method
        Block getXBody = new BlockImpl();
        getXBody.addStatement(new ReturnStatement(
            new FieldAccessExpression(new ThisExpression(), "x")
        ));
        MethodDef getXMethod = new MethodDef("getX", SystemDataType.INT, getXBody);

        // Static method
        Block zeroBody = new BlockImpl();
        zeroBody.addStatement(new ReturnStatement(LiteralExpression.of(0)));
        MethodDef zeroMethod = MethodDef.staticMethod("zero", SystemDataType.INT, zeroBody);

        // Type declaration
        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Point",
            Arrays.asList(new StructFieldDef("x", SystemDataType.INT)),
            null,
            Arrays.asList(getXMethod),
            Arrays.asList(zeroMethod)
        );
        program.addStatement(typeDecl);

        // Compile
        String className = uniqueClassName("TestTypeClassGeneration");
        Map<String, byte[]> allClasses = compiler.compileToMultipleClasses(program, className);

        // Verify Point class was generated
        assertTrue(allClasses.containsKey("Point"), "Should have Point class");

        // Load and verify class structure
        MultiClassLoader loader = new MultiClassLoader();
        for (Map.Entry<String, byte[]> entry : allClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }

        Class<?> pointClass = loader.loadClass("Point");

        // Verify field exists
        assertNotNull(pointClass.getDeclaredField("x"));

        // Verify instance method exists
        assertNotNull(pointClass.getMethod("getX"));

        // Verify static method exists
        Method zeroMethodRef = pointClass.getMethod("zero");
        assertTrue(java.lang.reflect.Modifier.isStatic(zeroMethodRef.getModifiers()));
    }

    @Test
    void testStaticMethodMultipleParameters() throws Exception {
        // type Ops {
        //   static func sum3(int a, int b, int c) -> int { return a + b + c; }
        // }
        // return Ops.sum3(1, 2, 3);  => 6

        Block program = new BlockImpl();

        // Static method body: return a + b + c;
        Block methodBody = new BlockImpl();
        methodBody.addStatement(new ReturnStatement(
            new Plus(
                new Plus(VariableExpression.of("a"), VariableExpression.of("b")),
                VariableExpression.of("c")
            )
        ));

        MethodDef sum3Method = MethodDef.staticMethod(
            "sum3",
            SystemDataType.INT,
            methodBody,
            ParameterDef.of("a", SystemDataType.INT),
            ParameterDef.of("b", SystemDataType.INT),
            ParameterDef.of("c", SystemDataType.INT)
        );

        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Ops",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Arrays.asList(sum3Method)
        );
        program.addStatement(typeDecl);

        // Ops.sum3(1, 2, 3)
        StaticMethodCallExpression methodCall = new StaticMethodCallExpression(
            "Ops",
            "sum3",
            LiteralExpression.of(1),
            LiteralExpression.of(2),
            LiteralExpression.of(3)
        );

        String className = uniqueClassName("TestStaticMultiParams");
        Class<?> clazz = compileAndLoadWithReturn(program, methodCall, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(6, result);
    }

    @Test
    void testCombinedInstanceAndStaticMethods() throws Exception {
        // type Value {
        //   int n;
        //   func get() -> int { return this.n; }
        //   static func make(int v) -> int { return v; }
        // }
        // Value val = Value(100);
        // return val.get() + Value.make(50);  => 150

        Block program = new BlockImpl();

        // Instance method: get()
        Block getBody = new BlockImpl();
        getBody.addStatement(new ReturnStatement(
            new FieldAccessExpression(new ThisExpression(), "n")
        ));
        MethodDef getMethod = new MethodDef("get", SystemDataType.INT, getBody);

        // Static method: make(int v)
        Block makeBody = new BlockImpl();
        makeBody.addStatement(new ReturnStatement(VariableExpression.of("v")));
        MethodDef makeMethod = MethodDef.staticMethod(
            "make",
            SystemDataType.INT,
            makeBody,
            ParameterDef.of("v", SystemDataType.INT)
        );

        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Value",
            Arrays.asList(new StructFieldDef("n", SystemDataType.INT)),
            null,
            Arrays.asList(getMethod),
            Arrays.asList(makeMethod)
        );
        program.addStatement(typeDecl);

        // Value val = Value(100);
        VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
            "val",
            new DataTypeImpl("Value"),
            new StructInstantiation("Value", LiteralExpression.of(100))
        );
        program.addStatement(varDecl);

        // val.get() + Value.make(50)
        Plus expr = new Plus(
            new MethodCallExpression(VariableExpression.of("val"), "get"),
            new StaticMethodCallExpression("Value", "make", LiteralExpression.of(50))
        );

        String className = uniqueClassName("TestCombinedMethods");
        Class<?> clazz = compileAndLoadWithReturn(program, expr, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(150, result);
    }

    /**
     * Helper method to compile with return using MultiClassLoader.
     * Ensures type classes are loaded before the main class.
     */
    private Class<?> compileAndLoadWithReturn(Block program, com.elminster.jcp.ast.Expression expression,
                                               DataType returnType, String className) throws Exception {
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] mainBytecode = generator.compileWithReturn(program, expression, returnType);
        Map<String, byte[]> typeClasses = generator.getGeneratedClasses();

        MultiClassLoader loader = new MultiClassLoader();

        // Register all type classes
        for (Map.Entry<String, byte[]> entry : typeClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }

        // Register main class
        loader.defineClass(className, mainBytecode);

        // Load type classes first
        for (String typeClassName : typeClasses.keySet()) {
            loader.loadClass(typeClassName);
        }

        // Load main class
        return loader.loadClass(className);
    }
}
