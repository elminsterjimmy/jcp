package com.elminster.jcp.compile.struct;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.FieldAccessExpression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StructInstantiation;
import com.elminster.jcp.ast.expression.ThisExpression;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
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
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for type methods compilation.
 * Tests instance methods, static methods, and constructors.
 */
public class TypeMethodCompileTest extends AbstractCompileTest {

    /**
     * Tests a type with an instance method.
     * <pre>
     * type Counter {
     *   count: Int
     *   fun getValue() -> Int { return this.count }
     * }
     * Counter c = Counter(42)
     * return c.getValue()  // => 42
     * </pre>
     */
    @Test
    void testInstanceMethod() throws Exception {
        Block program = new BlockImpl();

        // Method body: return this.count
        Block methodBody = new BlockImpl();
        FieldAccessExpression thisCount = new FieldAccessExpression(
            new ThisExpression(),
            "count"
        );
        methodBody.addStatement(new ReturnStatement(thisCount));

        // Instance method: getValue() -> Int
        MethodDef getValueMethod = new MethodDef("getValue", SystemDataType.INT, methodBody);

        // Type declaration with instance method
        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Counter",
            Arrays.asList(new StructFieldDef("count", SystemDataType.INT)),
            null,
            Arrays.asList(getValueMethod),
            Collections.emptyList()
        );
        program.addStatement(typeDecl);

        // Create instance: Counter(42)
        StructInstantiation structInst = new StructInstantiation("Counter",
            LiteralExpression.of(IntLiteral.of(42))
        );
        VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
            "c",
            new DataTypeImpl("Counter"),
            structInst
        );
        program.addStatement(varDecl);

        // Method call: c.getValue()
        MethodCallExpression methodCall = new MethodCallExpression(
            new VariableExpression(Identifier.fromName("c")),
            "getValue"
        );

        // Compile and load
        String className = uniqueClassName("TestInstanceMethod");
        Class<?> clazz = compileAndLoadWithReturn(program, methodCall, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(42, result);
    }

    /**
     * Tests a type with a static method.
     * <pre>
     * type Math {
     *   static fun add(a: Int, b: Int) -> Int { return a + b }
     * }
     * return Math.add(10, 20)  // => 30
     * </pre>
     */
    @Test
    void testStaticMethod() throws Exception {
        Block program = new BlockImpl();

        // Static method body: return a + b
        Block methodBody = new BlockImpl();
        Plus addition = new Plus(
            new VariableExpression(Identifier.fromName("a")),
            new VariableExpression(Identifier.fromName("b"))
        );
        methodBody.addStatement(new ReturnStatement(addition));

        // Static method: add(a: Int, b: Int) -> Int
        MethodDef addMethod = MethodDef.staticMethod("add", SystemDataType.INT, methodBody,
            new ParameterDef("a", SystemDataType.INT),
            new ParameterDef("b", SystemDataType.INT)
        );

        // Type declaration with static method
        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Math",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Arrays.asList(addMethod)
        );
        program.addStatement(typeDecl);

        // Static method call: Math.add(10, 20)
        com.elminster.jcp.ast.expression.StaticMethodCallExpression staticCall =
            new com.elminster.jcp.ast.expression.StaticMethodCallExpression(
                Identifier.fromName("Math"),
                "add",
                LiteralExpression.of(IntLiteral.of(10)),
                LiteralExpression.of(IntLiteral.of(20))
            );

        // Compile and load
        String className = uniqueClassName("TestStaticMethod");
        Class<?> clazz = compileAndLoadWithReturn(program, staticCall, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(30, result);
    }

    /**
     * Tests instance method with parameter.
     * <pre>
     * type Adder {
     *   base: Int
     *   fun addTo(x: Int) -> Int { return this.base + x }
     * }
     * Adder a = Adder(100)
     * return a.addTo(23)  // => 123
     * </pre>
     */
    @Test
    void testInstanceMethodWithParameter() throws Exception {
        Block program = new BlockImpl();

        // Method body: return this.base + x
        Block methodBody = new BlockImpl();
        Plus addition = new Plus(
            new FieldAccessExpression(new ThisExpression(), "base"),
            new VariableExpression(Identifier.fromName("x"))
        );
        methodBody.addStatement(new ReturnStatement(addition));

        // Instance method: addTo(x: Int) -> Int
        MethodDef addToMethod = new MethodDef("addTo", SystemDataType.INT, methodBody,
            new ParameterDef("x", SystemDataType.INT)
        );

        // Type declaration
        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Adder",
            Arrays.asList(new StructFieldDef("base", SystemDataType.INT)),
            null,
            Arrays.asList(addToMethod),
            Collections.emptyList()
        );
        program.addStatement(typeDecl);

        // Create instance: Adder(100)
        StructInstantiation structInst = new StructInstantiation("Adder",
            LiteralExpression.of(IntLiteral.of(100))
        );
        VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
            "a",
            new DataTypeImpl("Adder"),
            structInst
        );
        program.addStatement(varDecl);

        // Method call: a.addTo(23)
        MethodCallExpression methodCall = new MethodCallExpression(
            new VariableExpression(Identifier.fromName("a")),
            "addTo",
            LiteralExpression.of(IntLiteral.of(23))
        );

        // Compile and load
        String className = uniqueClassName("TestInstanceMethodWithParam");
        Class<?> clazz = compileAndLoadWithReturn(program, methodCall, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(123, result);
    }

    /**
     * Helper method to compile with return using MultiClassLoader.
     */
    private Class<?> compileAndLoadWithReturn(Block program, com.elminster.jcp.ast.Expression expression,
                                               com.elminster.jcp.eval.data.DataType returnType,
                                               String className) throws Exception {
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
