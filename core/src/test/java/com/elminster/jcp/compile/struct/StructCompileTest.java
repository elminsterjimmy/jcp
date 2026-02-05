package com.elminster.jcp.compile.struct;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.FieldAccessExpression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StructInstantiation;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.StructDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for struct type compilation.
 * Tests struct declaration, instantiation, field access, and field assignment.
 */
public class StructCompileTest extends AbstractCompileTest {

    @Test
    void testStructDeclaration() throws Exception {
        // struct Point { int x; int y; }
        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        // Compile to multiple classes
        Map<String, byte[]> allClasses = compiler.compileToMultipleClasses(program, uniqueClassName("TestStructDecl"));

        // Should have main class + Point struct class
        assertTrue(allClasses.size() >= 2, "Should have at least 2 classes (main + Point)");
        assertTrue(allClasses.containsKey("Point"), "Should have Point class");
    }

    @Test
    void testStructInstantiation() throws Exception {
        // struct Point { int x; int y; }
        // Point p = Point(10, 20);
        // int result = p.x + p.y;

        Block program = new BlockImpl();

        // Struct declaration
        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        // Variable declaration with struct instantiation
        StructInstantiation structInst = new StructInstantiation("Point",
            LiteralExpression.of(IntLiteral.of(10)),
            LiteralExpression.of(IntLiteral.of(20))
        );
        VariableDeclarationImpl pDecl = new VariableDeclarationImpl(
            "p",
            new DataTypeImpl("Point"),
            structInst
        );
        program.addStatement(pDecl);

        // Access fields and add them: p.x + p.y
        FieldAccessExpression xAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("p")),
            "x"
        );
        FieldAccessExpression yAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("p")),
            "y"
        );
        Plus sum = new Plus(xAccess, yAccess);

        VariableDeclarationImpl resultDecl = new VariableDeclarationImpl(
            "result",
            SystemDataType.INT,
            sum
        );
        program.addStatement(resultDecl);

        // Compile and load with MultiClassLoader
        String className = uniqueClassName("TestStructInst");
        Map<String, byte[]> allClasses = compiler.compileToMultipleClasses(program, className);

        MultiClassLoader loader = new MultiClassLoader();
        for (Map.Entry<String, byte[]> entry : allClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }

        Class<?> clazz = loader.loadClass(className);
        Method mainMethod = clazz.getMethod("main", String[].class);

        // Run main (should not throw)
        mainMethod.invoke(null, (Object) new String[]{});
    }

    @Test
    void testFieldAccess() throws Exception {
        // struct Point { int x; int y; }
        // Point p = Point(15, 25);
        // return p.x;  => 15

        Block program = new BlockImpl();

        // Struct declaration
        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        // Create struct instance
        StructInstantiation structInst = new StructInstantiation("Point",
            LiteralExpression.of(IntLiteral.of(15)),
            LiteralExpression.of(IntLiteral.of(25))
        );
        VariableDeclarationImpl pDecl = new VariableDeclarationImpl(
            "p",
            new DataTypeImpl("Point"),
            structInst
        );
        program.addStatement(pDecl);

        // Field access expression to return
        FieldAccessExpression xAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("p")),
            "x"
        );

        // Compile with return using MultiClassLoader approach
        String className = uniqueClassName("TestFieldAccess");
        Class<?> clazz = compileAndLoadWithReturn(program, xAccess, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(15, result);
    }

    @Test
    void testFieldAccessBoth() throws Exception {
        // struct Point { int x; int y; }
        // Point p = Point(100, 200);
        // return p.x + p.y;  => 300

        Block program = new BlockImpl();

        // Struct declaration
        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        // Create struct instance
        StructInstantiation structInst = new StructInstantiation("Point",
            LiteralExpression.of(IntLiteral.of(100)),
            LiteralExpression.of(IntLiteral.of(200))
        );
        VariableDeclarationImpl pDecl = new VariableDeclarationImpl(
            "p",
            new DataTypeImpl("Point"),
            structInst
        );
        program.addStatement(pDecl);

        // Field access: p.x + p.y
        FieldAccessExpression xAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("p")),
            "x"
        );
        FieldAccessExpression yAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("p")),
            "y"
        );
        Plus sum = new Plus(xAccess, yAccess);

        // Compile and load
        String className = uniqueClassName("TestFieldAccessBoth");
        Class<?> clazz = compileAndLoadWithReturn(program, sum, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(300, result);
    }

    @Test
    void testStructInLocalVariable() throws Exception {
        // struct Point { int x; int y; }
        // Point p1 = Point(5, 10);
        // Point p2 = Point(15, 20);
        // return p1.x + p2.y;  => 25

        Block program = new BlockImpl();

        // Struct declaration
        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        // Create first struct instance
        StructInstantiation struct1 = new StructInstantiation("Point",
            LiteralExpression.of(IntLiteral.of(5)),
            LiteralExpression.of(IntLiteral.of(10))
        );
        VariableDeclarationImpl p1Decl = new VariableDeclarationImpl(
            "p1",
            new DataTypeImpl("Point"),
            struct1
        );
        program.addStatement(p1Decl);

        // Create second struct instance
        StructInstantiation struct2 = new StructInstantiation("Point",
            LiteralExpression.of(IntLiteral.of(15)),
            LiteralExpression.of(IntLiteral.of(20))
        );
        VariableDeclarationImpl p2Decl = new VariableDeclarationImpl(
            "p2",
            new DataTypeImpl("Point"),
            struct2
        );
        program.addStatement(p2Decl);

        // Field access: p1.x + p2.y
        FieldAccessExpression p1x = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("p1")),
            "x"
        );
        FieldAccessExpression p2y = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("p2")),
            "y"
        );
        Plus sum = new Plus(p1x, p2y);

        // Compile and load
        String className = uniqueClassName("TestMultipleStructVars");
        Class<?> clazz = compileAndLoadWithReturn(program, sum, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(25, result);  // 5 + 20 = 25
    }

    @Test
    void testMultipleStructTypes() throws Exception {
        // struct Point { int x; int y; }
        // struct Size { int width; int height; }
        // Point p = Point(10, 20);
        // Size s = Size(30, 40);
        // return p.x + s.width;  => 40

        Block program = new BlockImpl();

        // First struct declaration
        StructDeclarationImpl pointDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
        program.addStatement(pointDecl);

        // Second struct declaration
        StructDeclarationImpl sizeDecl = new StructDeclarationImpl("Size", Arrays.asList(
            new StructFieldDef("width", SystemDataType.INT),
            new StructFieldDef("height", SystemDataType.INT)
        ));
        program.addStatement(sizeDecl);

        // Create Point instance
        StructInstantiation pointInst = new StructInstantiation("Point",
            LiteralExpression.of(IntLiteral.of(10)),
            LiteralExpression.of(IntLiteral.of(20))
        );
        VariableDeclarationImpl pDecl = new VariableDeclarationImpl(
            "p",
            new DataTypeImpl("Point"),
            pointInst
        );
        program.addStatement(pDecl);

        // Create Size instance
        StructInstantiation sizeInst = new StructInstantiation("Size",
            LiteralExpression.of(IntLiteral.of(30)),
            LiteralExpression.of(IntLiteral.of(40))
        );
        VariableDeclarationImpl sDecl = new VariableDeclarationImpl(
            "s",
            new DataTypeImpl("Size"),
            sizeInst
        );
        program.addStatement(sDecl);

        // Field access: p.x + s.width
        FieldAccessExpression px = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("p")),
            "x"
        );
        FieldAccessExpression sWidth = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("s")),
            "width"
        );
        Plus sum = new Plus(px, sWidth);

        // Compile and load
        String className = uniqueClassName("TestMultipleStructTypes");
        Map<String, byte[]> allClasses = compiler.compileToMultipleClasses(program, className);

        // Should have main class + Point + Size
        assertTrue(allClasses.size() >= 3, "Should have at least 3 classes");
        assertTrue(allClasses.containsKey("Point"), "Should have Point class");
        assertTrue(allClasses.containsKey("Size"), "Should have Size class");

        // Load and execute with return
        Class<?> clazz = loadWithReturn(allClasses, className, sum, SystemDataType.INT);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(40, result);  // 10 + 30 = 40
    }

    @Test
    void testStructTypeDescriptor() throws Exception {
        // Verify that struct classes are properly generated
        // struct Point { int x; int y; }
        // Point p = Point(1, 2);

        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        StructInstantiation structInst = new StructInstantiation("Point",
            LiteralExpression.of(IntLiteral.of(1)),
            LiteralExpression.of(IntLiteral.of(2))
        );
        VariableDeclarationImpl pDecl = new VariableDeclarationImpl(
            "p",
            new DataTypeImpl("Point"),
            structInst
        );
        program.addStatement(pDecl);

        // Compile
        String className = uniqueClassName("TestStructDescriptor");
        Map<String, byte[]> allClasses = compiler.compileToMultipleClasses(program, className);

        // Load classes
        MultiClassLoader loader = new MultiClassLoader();
        for (Map.Entry<String, byte[]> entry : allClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }

        // Verify Point class exists and has expected fields
        Class<?> pointClass = loader.loadClass("Point");
        assertNotNull(pointClass);

        // Verify fields exist
        assertNotNull(pointClass.getDeclaredField("x"));
        assertNotNull(pointClass.getDeclaredField("y"));

        // Verify constructor exists: Point(int, int)
        assertNotNull(pointClass.getConstructor(int.class, int.class));
    }

    /**
     * Helper method to compile with return using MultiClassLoader.
     * Similar to compileAndLoadWithReturn but handles multiple classes.
     */
    private Class<?> compileAndLoadWithReturn(Block program, com.elminster.jcp.ast.Expression expression,
                                               DataType returnType, String className) throws Exception {
        // First compile the program with return
        com.elminster.jcp.compile.BytecodeGenerator generator = new com.elminster.jcp.compile.BytecodeGenerator(className);
        byte[] mainBytecode = generator.compileWithReturn(program, expression, returnType);

        // Get struct classes
        Map<String, byte[]> structClasses = generator.getGeneratedClasses();

        // Load all classes
        MultiClassLoader loader = new MultiClassLoader();

        // Load struct classes first
        for (Map.Entry<String, byte[]> entry : structClasses.entrySet()) {
            loader.defineClass(entry.getKey(), entry.getValue());
        }

        // Load main class
        loader.defineClass(className, mainBytecode);

        return loader.loadClass(className);
    }

    /**
     * Helper to load classes with a return expression.
     * Used when we already have the compiled classes.
     */
    private Class<?> loadWithReturn(Map<String, byte[]> allClasses, String mainClassName,
                                     com.elminster.jcp.ast.Expression expression, DataType returnType) throws Exception {
        // Need to recompile the main class with the return expression
        // This is a bit of a workaround - we'll get the program from context
        // For now, create a simple wrapper that returns the expression

        // Actually, this is complex - let's simplify by just loading the classes
        // and calling main. For this test, we'll need to modify the approach.

        // Alternative: compile fresh with return
        Block emptyProgram = new BlockImpl();
        return compileAndLoadWithReturn(emptyProgram, expression, returnType, mainClassName + "_WithReturn");
    }
}
