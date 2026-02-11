package com.elminster.jcp.compile.struct;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.FieldAccessExpression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StructInstantiation;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.StructDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
import com.elminster.jcp.compile.MultiClassLoader;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for edge cases in struct compilation - FieldAccessCompiler, StructInstantiationCompiler.
 */
public class StructCompilerEdgeCaseTest extends AbstractCompileTest {

    /**
     * Tests struct instantiation with wrong field count throws exception.
     */
    @Test
    void testStructInstantiationWrongFieldCount() {
        Block program = new BlockImpl();

        // Struct with 2 fields
        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        // Try to instantiate with 3 values (wrong)
        StructInstantiation structInst = new StructInstantiation("Point",
            LiteralExpression.of(IntLiteral.of(1)),
            LiteralExpression.of(IntLiteral.of(2)),
            LiteralExpression.of(IntLiteral.of(3))  // Extra field!
        );

        String className = uniqueClassName("TestWrongFieldCount");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        assertThrows(IllegalArgumentException.class, () ->
            generator.compileWithReturn(program, structInst, new DataTypeImpl("Point"))
        );
    }

    /**
     * Tests struct instantiation with unknown type throws exception.
     */
    @Test
    void testStructInstantiationUnknownType() {
        Block program = new BlockImpl();
        // No struct declared

        // Try to instantiate non-existent struct
        StructInstantiation structInst = new StructInstantiation("UnknownStruct",
            LiteralExpression.of(IntLiteral.of(1))
        );

        String className = uniqueClassName("TestUnknownType");
        BytecodeGenerator generator = new BytecodeGenerator(className);

        assertThrows(IllegalArgumentException.class, () ->
            generator.compileWithReturn(program, structInst, new DataTypeImpl("UnknownStruct"))
        );
    }

    /**
     * Tests struct with double field access.
     * <pre>
     * struct Circle { radius: Double }
     * Circle c = Circle(3.14)
     * return c.radius  // returns 3.14
     * </pre>
     */
    @Test
    void testStructWithDoubleFieldAccess() throws Exception {
        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Circle", Arrays.asList(
            new StructFieldDef("radius", SystemDataType.DOUBLE)
        ));
        program.addStatement(structDecl);

        StructInstantiation structInst = new StructInstantiation("Circle",
            LiteralExpression.of(3.14)
        );
        VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
            "c",
            new DataTypeImpl("Circle"),
            structInst
        );
        program.addStatement(varDecl);

        FieldAccessExpression fieldAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("c")),
            "radius"
        );

        String className = uniqueClassName("TestDoubleFieldAccess");
        Class<?> clazz = compileAndLoadWithReturn(program, fieldAccess, SystemDataType.DOUBLE, className);

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(3.14, result, 0.001);
    }

    /**
     * Tests struct with boolean field access.
     * <pre>
     * struct Flag { enabled: Boolean }
     * Flag f = Flag(true)
     * return f.enabled  // returns true
     * </pre>
     */
    @Test
    void testStructWithBooleanFieldAccess() throws Exception {
        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Flag", Arrays.asList(
            new StructFieldDef("enabled", SystemDataType.BOOLEAN)
        ));
        program.addStatement(structDecl);

        StructInstantiation structInst = new StructInstantiation("Flag",
            LiteralExpression.of(true)
        );
        VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
            "f",
            new DataTypeImpl("Flag"),
            structInst
        );
        program.addStatement(varDecl);

        FieldAccessExpression fieldAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("f")),
            "enabled"
        );

        String className = uniqueClassName("TestBooleanFieldAccess");
        Class<?> clazz = compileAndLoadWithReturn(program, fieldAccess, SystemDataType.BOOLEAN, className);

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }

    /**
     * Tests struct with string field access.
     * <pre>
     * struct Person { name: String }
     * Person p = Person("Alice")
     * return p.name  // returns "Alice"
     * </pre>
     */
    @Test
    void testStructWithStringFieldAccess() throws Exception {
        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Person", Arrays.asList(
            new StructFieldDef("name", SystemDataType.STRING)
        ));
        program.addStatement(structDecl);

        StructInstantiation structInst = new StructInstantiation("Person",
            LiteralExpression.of("Alice")
        );
        VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
            "p",
            new DataTypeImpl("Person"),
            structInst
        );
        program.addStatement(varDecl);

        FieldAccessExpression fieldAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("p")),
            "name"
        );

        String className = uniqueClassName("TestStringFieldAccess");
        Class<?> clazz = compileAndLoadWithReturn(program, fieldAccess, SystemDataType.STRING, className);

        Method evaluate = clazz.getMethod("evaluate");
        String result = (String) evaluate.invoke(null);
        assertEquals("Alice", result);
    }

    /**
     * Tests struct with single int field.
     * <pre>
     * struct Single { value: Int }
     * Single s = Single(99)
     * return s.value  // returns 99
     * </pre>
     */
    @Test
    void testStructWithSingleField() throws Exception {
        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Single", Arrays.asList(
            new StructFieldDef("value", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        StructInstantiation structInst = new StructInstantiation("Single",
            LiteralExpression.of(IntLiteral.of(99))
        );
        VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
            "s",
            new DataTypeImpl("Single"),
            structInst
        );
        program.addStatement(varDecl);

        FieldAccessExpression fieldAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("s")),
            "value"
        );

        String className = uniqueClassName("TestSingleField");
        Class<?> clazz = compileAndLoadWithReturn(program, fieldAccess, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(99, result);
    }

    /**
     * Helper method to compile with return using MultiClassLoader.
     */
    private Class<?> compileAndLoadWithReturn(Block program, com.elminster.jcp.ast.Expression expression,
                                               com.elminster.jcp.eval.data.DataType returnType, String className) throws Exception {
        BytecodeGenerator generator = new BytecodeGenerator(className);
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
