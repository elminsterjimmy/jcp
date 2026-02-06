package com.elminster.jcp.eval.struct;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.FieldAssignmentExpression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StructInstantiation;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.StructDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import com.elminster.jcp.eval.data.StructData;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FieldAssignmentEvaluator.
 */
class FieldAssignmentEvaluatorTest {

    private EvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
    }

    @Test
    void testSimpleFieldAssignment() {
        // Create program:
        // struct Point { int x; int y; }
        // Point p = Point(10, 20);
        // p.x = 100;

        Block program = new BlockImpl();

        // Struct declaration
        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", DataType.SystemDataType.INT),
            new StructFieldDef("y", DataType.SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        // Create struct instance
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

        // Field assignment: p.x = 100;
        FieldAssignmentExpression fieldAssign = new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("p")),
            "x",
            LiteralExpression.of(IntLiteral.of(100))
        );
        program.addStatement(ExpressionStatement.of(fieldAssign));

        // Execute
        new EvalVisitor(context).visit(program);

        // Verify field was updated
        StructData pData = (StructData) context.getVariable("p");
        assertEquals(100, pData.getField("x").get());
        assertEquals(20, pData.getField("y").get());  // Unchanged
    }

    @Test
    void testMultipleFieldAssignments() {
        // struct Point { int x; int y; }
        // Point p = Point(0, 0);
        // p.x = 50;
        // p.y = 75;

        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", DataType.SystemDataType.INT),
            new StructFieldDef("y", DataType.SystemDataType.INT)
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

        new EvalVisitor(context).visit(program);

        StructData pData = (StructData) context.getVariable("p");
        assertEquals(50, pData.getField("x").get());
        assertEquals(75, pData.getField("y").get());
    }

    @Test
    void testFieldAssignment_ReturnsAssignedValue() {
        // Field assignment should return the assigned value
        // Use full program approach
        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", DataType.SystemDataType.INT),
            new StructFieldDef("y", DataType.SystemDataType.INT)
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

        // Assign to p.x and capture result in another variable
        // int result = (p.x = 999);
        FieldAssignmentExpression fieldAssign = new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("p")),
            "x",
            LiteralExpression.of(IntLiteral.of(999))
        );
        VariableDeclarationImpl resultDecl = new VariableDeclarationImpl(
            "result",
            DataType.SystemDataType.INT,
            fieldAssign
        );
        program.addStatement(resultDecl);

        new EvalVisitor(context).visit(program);

        // The assigned value should be returned and stored in result
        assertEquals(999, context.getVariable("result").get());
        // And the struct field should be updated
        StructData pData = (StructData) context.getVariable("p");
        assertEquals(999, pData.getField("x").get());
    }

    @Test
    void testFieldAssignment_InvalidField_ThrowsException() {
        // Try to assign to a non-existent field
        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
            new StructFieldDef("x", DataType.SystemDataType.INT),
            new StructFieldDef("y", DataType.SystemDataType.INT)
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

        // Try to assign to non-existent field "z"
        FieldAssignmentExpression fieldAssign = new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("p")),
            "z",  // Invalid field
            LiteralExpression.of(IntLiteral.of(100))
        );
        program.addStatement(ExpressionStatement.of(fieldAssign));

        assertThrows(IllegalArgumentException.class, () -> {
            new EvalVisitor(context).visit(program);
        });
    }

    @Test
    void testFieldAssignment_TypeMismatch_ThrowsException() {
        // Try to assign wrong type to a field
        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Person", Arrays.asList(
            new StructFieldDef("name", DataType.SystemDataType.STRING),
            new StructFieldDef("age", DataType.SystemDataType.INT)
        ));
        program.addStatement(structDecl);

        StructInstantiation structInst = new StructInstantiation("Person",
            LiteralExpression.of(StringLiteral.of("John")),
            LiteralExpression.of(IntLiteral.of(30))
        );
        VariableDeclarationImpl personDecl = new VariableDeclarationImpl(
            "person",
            new DataTypeImpl("Person"),
            structInst
        );
        program.addStatement(personDecl);

        // Try to assign string to int field
        FieldAssignmentExpression fieldAssign = new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("person")),
            "age",
            LiteralExpression.of(StringLiteral.of("not a number"))
        );
        program.addStatement(ExpressionStatement.of(fieldAssign));

        assertThrows(CannotCastException.class, () -> {
            new EvalVisitor(context).visit(program);
        });
    }

    @Test
    void testFieldAssignment_OverwriteMultipleTimes() {
        // Assign to the same field multiple times
        Block program = new BlockImpl();

        StructDeclarationImpl structDecl = new StructDeclarationImpl("Counter", Arrays.asList(
            new StructFieldDef("value", DataType.SystemDataType.INT)
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

        // Assign value = 1
        program.addStatement(ExpressionStatement.of(new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("c")),
            "value",
            LiteralExpression.of(IntLiteral.of(1))
        )));

        // Assign value = 2
        program.addStatement(ExpressionStatement.of(new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("c")),
            "value",
            LiteralExpression.of(IntLiteral.of(2))
        )));

        // Assign value = 3
        program.addStatement(ExpressionStatement.of(new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("c")),
            "value",
            LiteralExpression.of(IntLiteral.of(3))
        )));

        new EvalVisitor(context).visit(program);

        StructData cData = (StructData) context.getVariable("c");
        assertEquals(3, cData.getField("value").get());
    }

    @Test
    void testFieldAssignment_NonStruct_ThrowsException() {
        // Try to assign a field on a non-struct value (e.g., int)
        Block program = new BlockImpl();

        // int x = 42;
        program.addStatement(new VariableDeclarationImpl(
            "x",
            DataType.SystemDataType.INT,
            LiteralExpression.of(IntLiteral.of(42))
        ));

        // x.field = 100;  // x is not a struct
        FieldAssignmentExpression fieldAssign = new FieldAssignmentExpression(
            new VariableExpression(Identifier.fromName("x")),
            "field",
            LiteralExpression.of(IntLiteral.of(100))
        );
        program.addStatement(ExpressionStatement.of(fieldAssign));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            new EvalVisitor(context).visit(program);
        });

        assertTrue(ex.getMessage().contains("Field assignment requires a struct instance"));
    }
}
