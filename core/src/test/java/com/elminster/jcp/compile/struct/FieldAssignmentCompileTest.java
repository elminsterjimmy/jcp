package com.elminster.jcp.compile.struct;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.FieldAccessExpression;
import com.elminster.jcp.ast.expression.FieldAssignmentExpression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StructInstantiation;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.StructDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.BytecodeGenerator;
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
 * Tests for field assignment compilation.
 */
public class FieldAssignmentCompileTest extends AbstractCompileTest {

    @Test
    void testSimpleFieldAssignment_CompilesSuccessfully() throws Exception {
        // struct Point { int x; int y; }
        // Point p = Point(10, 20);
        // p.x = 100;

        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

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

        // p.x = 100;
        FieldAssignmentExpression fieldAssign = new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("p")),
            "x",
            LiteralExpression.of(IntLiteral.of(100))
        );
        program.addStatement(ExpressionStatement.of(fieldAssign));

        String className = uniqueClassName("TestFieldAssignment");
        Map<String, byte[]> allClasses = compiler.compileToMultipleClasses(program, className);

        assertNotNull(allClasses);
        assertTrue(allClasses.size() >= 2);
    }

    @Test
    void testFieldAssignment_VerifyNewValue() throws Exception {
        // struct Point { int x; int y; }
        // Point p = Point(10, 20);
        // p.x = 100;
        // return p.x;  => 100

        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

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

        // p.x = 100;
        FieldAssignmentExpression fieldAssign = new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("p")),
            "x",
            LiteralExpression.of(IntLiteral.of(100))
        );
        program.addStatement(ExpressionStatement.of(fieldAssign));

        // Return p.x to verify the assignment
        FieldAccessExpression xAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("p")),
            "x"
        );

        String className = uniqueClassName("TestFieldAssignmentVerify");
        Class<?> clazz = compileAndLoadWithReturn(program, xAccess, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(100, result);
    }

    @Test
    void testMultipleFieldAssignments() throws Exception {
        // struct Point { int x; int y; }
        // Point p = Point(0, 0);
        // p.x = 50;
        // p.y = 75;
        // return p.x + p.y;  => 125

        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        StructInstantiation structInst = new StructInstantiation("Point",
            LiteralExpression.of(IntLiteral.of(0)),
            LiteralExpression.of(IntLiteral.of(0))
        );
        VariableDeclarationImpl pDecl = new VariableDeclarationImpl(
            "p",
            new DataTypeImpl("Point"),
            structInst
        );
        program.addStatement(pDecl);

        // p.x = 50;
        program.addStatement(ExpressionStatement.of(new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("p")),
            "x",
            LiteralExpression.of(IntLiteral.of(50))
        )));

        // p.y = 75;
        program.addStatement(ExpressionStatement.of(new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("p")),
            "y",
            LiteralExpression.of(IntLiteral.of(75))
        )));

        // Return p.x + p.y
        FieldAccessExpression xAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("p")),
            "x"
        );
        FieldAccessExpression yAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("p")),
            "y"
        );
        com.elminster.jcp.ast.expression.operation.Plus sum = new com.elminster.jcp.ast.expression.operation.Plus(xAccess, yAccess);

        String className = uniqueClassName("TestMultipleFieldAssign");
        Class<?> clazz = compileAndLoadWithReturn(program, sum, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(125, result);
    }

    @Test
    void testFieldAssignment_OverwriteValue() throws Exception {
        // struct Counter { int value; }
        // Counter c = Counter(0);
        // c.value = 10;
        // c.value = 20;
        // c.value = 30;
        // return c.value;  => 30

        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Counter", Arrays.asList(
            new StructFieldDef("value", SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        StructInstantiation structInst = new StructInstantiation("Counter",
            LiteralExpression.of(IntLiteral.of(0))
        );
        VariableDeclarationImpl cDecl = new VariableDeclarationImpl(
            "c",
            new DataTypeImpl("Counter"),
            structInst
        );
        program.addStatement(cDecl);

        // c.value = 10;
        program.addStatement(ExpressionStatement.of(new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("c")),
            "value",
            LiteralExpression.of(IntLiteral.of(10))
        )));

        // c.value = 20;
        program.addStatement(ExpressionStatement.of(new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("c")),
            "value",
            LiteralExpression.of(IntLiteral.of(20))
        )));

        // c.value = 30;
        program.addStatement(ExpressionStatement.of(new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("c")),
            "value",
            LiteralExpression.of(IntLiteral.of(30))
        )));

        // Return c.value
        FieldAccessExpression valueAccess = new FieldAccessExpression(
            new VariableExpression(Identifier.fromName("c")),
            "value"
        );

        String className = uniqueClassName("TestFieldOverwrite");
        Class<?> clazz = compileAndLoadWithReturn(program, valueAccess, SystemDataType.INT, className);

        Method evaluate = clazz.getMethod("evaluate");
        int result = (int) evaluate.invoke(null);
        assertEquals(30, result);
    }

    /**
     * Helper method to compile with return using MultiClassLoader.
     */
    private Class<?> compileAndLoadWithReturn(Block program, com.elminster.jcp.ast.Expression expression,
                                               DataType returnType, String className) throws Exception {
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
