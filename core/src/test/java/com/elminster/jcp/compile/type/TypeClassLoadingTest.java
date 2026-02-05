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
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for type class loading via both compile() and compileWithReturn() entry points.
 *
 * CRITICAL: This test class ensures that type classes are properly loaded via BOTH
 * entry points. The compileWithReturn() entry point has historically had issues with
 * type classes not being tracked in getGeneratedClasses().
 *
 * See: docs/solutions/runtime-errors/noclassdeffounderror-struct-classes-not-loaded.md
 */
public class TypeClassLoadingTest extends AbstractCompileTest {

    @Test
    void testTypeClassLoadedViaCompile() throws Exception {
        // Use compile() entry point
        // type Point { int x; int y; }
        // Point p = Point(1, 2);

        Block program = new BlockImpl();

        // Type with instance method
        Block getXBody = new BlockImpl();
        getXBody.addStatement(new ReturnStatement(
            new FieldAccessExpression(new ThisExpression(), "x")
        ));
        MethodDef getXMethod = new MethodDef("getX", SystemDataType.INT, getXBody);

        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Point",
            Arrays.asList(
                new StructFieldDef("x", SystemDataType.INT),
                new StructFieldDef("y", SystemDataType.INT)
            ),
            null,
            Arrays.asList(getXMethod),
            Collections.emptyList()
        );
        program.addStatement(typeDecl);

        // Instantiate the type
        VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
            "p",
            new DataTypeImpl("Point"),
            new StructInstantiation("Point",
                LiteralExpression.of(1),
                LiteralExpression.of(2))
        );
        program.addStatement(varDecl);

        // Compile via compile() entry point
        String className = uniqueClassName("TestCompileEntryPoint");
        Map<String, byte[]> allClasses = compiler.compileToMultipleClasses(program, className);

        // CRITICAL: Verify type class was tracked
        assertTrue(allClasses.containsKey("Point"),
            "BUG: Type class 'Point' missing from generated classes via compile() entry point!");

        // Verify the type class can be loaded and has expected methods
        MultiClassLoader loader = new MultiClassLoader();
        for (Map.Entry<String, byte[]> entry : allClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }

        Class<?> pointClass = loader.loadClass("Point");
        assertNotNull(pointClass);

        // Verify instance method exists
        Method getXMethodRef = pointClass.getMethod("getX");
        assertNotNull(getXMethodRef);
    }

    @Test
    void testTypeClassLoadedViaCompileWithReturn() throws Exception {
        // Use compileWithReturn() entry point - THE ONE THAT HISTORICALLY FAILED
        // type Counter { int count; func getCount() -> int { return this.count; } }
        // Counter c = Counter(42);
        // return c.getCount();

        Block program = new BlockImpl();

        // Instance method
        Block getCountBody = new BlockImpl();
        getCountBody.addStatement(new ReturnStatement(
            new FieldAccessExpression(new ThisExpression(), "count")
        ));
        MethodDef getCountMethod = new MethodDef("getCount", SystemDataType.INT, getCountBody);

        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Counter",
            Arrays.asList(new StructFieldDef("count", SystemDataType.INT)),
            null,
            Arrays.asList(getCountMethod),
            Collections.emptyList()
        );
        program.addStatement(typeDecl);

        // Instantiate
        VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
            "c",
            new DataTypeImpl("Counter"),
            new StructInstantiation("Counter", LiteralExpression.of(42))
        );
        program.addStatement(varDecl);

        // Return expression
        MethodCallExpression methodCall = new MethodCallExpression(
            VariableExpression.of("c"),
            "getCount"
        );

        // Compile via compileWithReturn() entry point
        String className = uniqueClassName("TestCompileWithReturnEntryPoint");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] mainBytecode = generator.compileWithReturn(program, methodCall, SystemDataType.INT);

        // CRITICAL: Verify type class was tracked
        Map<String, byte[]> typeClasses = generator.getGeneratedClasses();
        assertTrue(typeClasses.containsKey("Counter"),
            "BUG: Type class 'Counter' missing from generated classes via compileWithReturn() entry point! " +
            "This indicates rootContext is not set correctly.");

        // Load and execute to verify it works at runtime
        MultiClassLoader loader = new MultiClassLoader();
        for (Map.Entry<String, byte[]> entry : typeClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }
        loader.defineClass(className, mainBytecode);

        // Load type classes first
        for (String typeClassName : typeClasses.keySet()) {
            loader.loadClass(typeClassName);
        }

        // Load and execute main class
        Class<?> clazz = loader.loadClass(className);
        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(42, result);
    }

    @Test
    void testMultipleTypeClassesLoadedViaCompileWithReturn() throws Exception {
        // Test that multiple type classes are all tracked via compileWithReturn()
        // type Point { int x; }
        // type Size { int w; }

        Block program = new BlockImpl();

        // First type
        StructDeclarationImpl pointDecl = new StructDeclarationImpl(
            "Point",
            Arrays.asList(new StructFieldDef("x", SystemDataType.INT)),
            null,
            Collections.emptyList(),
            Collections.emptyList()
        );
        program.addStatement(pointDecl);

        // Second type
        StructDeclarationImpl sizeDecl = new StructDeclarationImpl(
            "Size",
            Arrays.asList(new StructFieldDef("w", SystemDataType.INT)),
            null,
            Collections.emptyList(),
            Collections.emptyList()
        );
        program.addStatement(sizeDecl);

        // Instantiate both
        VariableDeclarationImpl pDecl = new VariableDeclarationImpl(
            "p",
            new DataTypeImpl("Point"),
            new StructInstantiation("Point", LiteralExpression.of(10))
        );
        program.addStatement(pDecl);

        VariableDeclarationImpl sDecl = new VariableDeclarationImpl(
            "s",
            new DataTypeImpl("Size"),
            new StructInstantiation("Size", LiteralExpression.of(20))
        );
        program.addStatement(sDecl);

        // return p.x + s.w
        Plus expr = new Plus(
            new FieldAccessExpression(VariableExpression.of("p"), "x"),
            new FieldAccessExpression(VariableExpression.of("s"), "w")
        );

        // Compile
        String className = uniqueClassName("TestMultipleTypes");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] mainBytecode = generator.compileWithReturn(program, expr, SystemDataType.INT);
        Map<String, byte[]> typeClasses = generator.getGeneratedClasses();

        // CRITICAL: Both type classes must be present
        assertTrue(typeClasses.containsKey("Point"),
            "BUG: Point class missing from generated classes!");
        assertTrue(typeClasses.containsKey("Size"),
            "BUG: Size class missing from generated classes!");

        // Load and verify execution
        MultiClassLoader loader = new MultiClassLoader();
        for (Map.Entry<String, byte[]> entry : typeClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }
        loader.defineClass(className, mainBytecode);

        for (String typeClassName : typeClasses.keySet()) {
            loader.loadClass(typeClassName);
        }

        Class<?> clazz = loader.loadClass(className);
        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(30, result);  // 10 + 20
    }

    @Test
    void testTypeWithStaticMethodLoadedCorrectly() throws Exception {
        // type Math { static func add(int a, int b) -> int { return a + b; } }
        // return Math.add(3, 7);

        Block program = new BlockImpl();

        // Static method
        Block addBody = new BlockImpl();
        addBody.addStatement(new ReturnStatement(
            new Plus(VariableExpression.of("a"), VariableExpression.of("b"))
        ));
        MethodDef addMethod = MethodDef.staticMethod(
            "add",
            SystemDataType.INT,
            addBody,
            ParameterDef.of("a", SystemDataType.INT),
            ParameterDef.of("b", SystemDataType.INT)
        );

        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Math",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Arrays.asList(addMethod)
        );
        program.addStatement(typeDecl);

        // Static method call
        StaticMethodCallExpression methodCall = new StaticMethodCallExpression(
            "Math",
            "add",
            LiteralExpression.of(3),
            LiteralExpression.of(7)
        );

        // Compile
        String className = uniqueClassName("TestStaticMethodLoading");
        BytecodeGenerator generator = new BytecodeGenerator(className);
        byte[] mainBytecode = generator.compileWithReturn(program, methodCall, SystemDataType.INT);
        Map<String, byte[]> typeClasses = generator.getGeneratedClasses();

        // Verify Math class is present
        assertTrue(typeClasses.containsKey("Math"),
            "BUG: Math class missing from generated classes!");

        // Load and verify static method can be called
        MultiClassLoader loader = new MultiClassLoader();
        for (Map.Entry<String, byte[]> entry : typeClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }
        loader.defineClass(className, mainBytecode);

        for (String typeClassName : typeClasses.keySet()) {
            loader.loadClass(typeClassName);
        }

        Class<?> clazz = loader.loadClass(className);
        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(10, result);

        // Verify the Math class has a static method
        Class<?> mathClass = loader.loadClass("Math");
        Method addMethodRef = mathClass.getMethod("add", int.class, int.class);
        assertTrue(java.lang.reflect.Modifier.isStatic(addMethodRef.getModifiers()));
    }

    @Test
    void testTypeClassNotDuplicatedOnMultipleCalls() throws Exception {
        // Verify that multiple compilations don't duplicate type classes

        Block program = new BlockImpl();

        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Simple",
            Arrays.asList(new StructFieldDef("val", SystemDataType.INT)),
            null,
            Collections.emptyList(),
            Collections.emptyList()
        );
        program.addStatement(typeDecl);

        VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
            "s",
            new DataTypeImpl("Simple"),
            new StructInstantiation("Simple", LiteralExpression.of(1))
        );
        program.addStatement(varDecl);

        FieldAccessExpression expr = new FieldAccessExpression(
            VariableExpression.of("s"),
            "val"
        );

        // First compilation
        String className1 = uniqueClassName("TestNoDupe1");
        BytecodeGenerator generator1 = new BytecodeGenerator(className1);
        generator1.compileWithReturn(program, expr, SystemDataType.INT);
        Map<String, byte[]> classes1 = generator1.getGeneratedClasses();

        // Second compilation with a new generator (fresh state)
        String className2 = uniqueClassName("TestNoDupe2");
        BytecodeGenerator generator2 = new BytecodeGenerator(className2);
        generator2.compileWithReturn(program, expr, SystemDataType.INT);
        Map<String, byte[]> classes2 = generator2.getGeneratedClasses();

        // Each generator should have exactly one copy of Simple
        assertEquals(1, classes1.entrySet().stream()
            .filter(e -> e.getKey().equals("Simple")).count());
        assertEquals(1, classes2.entrySet().stream()
            .filter(e -> e.getKey().equals("Simple")).count());
    }
}
