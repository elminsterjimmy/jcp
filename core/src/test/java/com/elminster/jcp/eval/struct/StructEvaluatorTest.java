package com.elminster.jcp.eval.struct;

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
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import com.elminster.jcp.eval.data.IntegerData;
import com.elminster.jcp.eval.data.StructData;
import com.elminster.jcp.eval.data.StructType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for struct declaration, instantiation, field access, and field assignment.
 */
class StructEvaluatorTest {

  @Test
  void testSimpleStructInstantiation() {
    // Just test that struct instantiation evaluator works
    RootEvalContext context = new RootEvalContext();

    // Register struct type
    StructType pointType = new StructType("Point", Arrays.asList(
        new StructFieldDef("x", DataType.SystemDataType.INT),
        new StructFieldDef("y", DataType.SystemDataType.INT)
    ));
    context.addDataType(pointType);

    // Create struct instance
    StructInstantiation structInst = new StructInstantiation("Point",
        LiteralExpression.of(IntLiteral.of(10)),
        LiteralExpression.of(IntLiteral.of(20))
    );

    StructInstantiationEvaluator evaluator = new StructInstantiationEvaluator(structInst);
    Data result = evaluator.eval(context);

    assertNotNull(result);
    assertTrue(result instanceof StructData);

    StructData structData = (StructData) result;
    assertEquals(10, structData.getField("x").get());
    assertEquals(20, structData.getField("y").get());
  }

  @Test
  void testStructDeclarationAndInstantiation() {
    // Create program:
    // struct Point {
    //   int x;
    //   int y;
    // }
    // Point p = Point(10, 20);

    Block program = new BlockImpl();

    // Struct declaration
    StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
        new StructFieldDef("x", DataType.SystemDataType.INT),
        new StructFieldDef("y", DataType.SystemDataType.INT)
    ));
    program.addStatement(structDecl);

    // Variable declaration with struct instantiation
    StructInstantiation structInst = new StructInstantiation("Point",
        LiteralExpression.of(IntLiteral.of(10)),
        LiteralExpression.of(IntLiteral.of(20))
    );
    VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
        "p",
        new DataTypeImpl("Point"),  // Reference to the struct type
        structInst
    );
    program.addStatement(varDecl);

    // Execute
    RootEvalContext context = new RootEvalContext();
    new EvalVisitor(context).visit(program);

    // Verify struct instance was created
    Data pData = context.getVariable("p");
    assertNotNull(pData);
    assertTrue(pData instanceof StructData);

    StructData structData = (StructData) pData;
    assertEquals("Point", structData.getStructType().getName());

    // Verify field values
    Data xData = structData.getField("x");
    assertNotNull(xData);
    assertTrue(xData instanceof IntegerData);
    assertEquals(10, xData.get());

    Data yData = structData.getField("y");
    assertNotNull(yData);
    assertTrue(yData instanceof IntegerData);
    assertEquals(20, yData.get());
  }

  @Test
  void testFieldAccess() {
    // Create program:
    // struct Point { int x; int y; }
    // Point p = Point(15, 25);
    // int xVal = p.x;

    Block program = new BlockImpl();

    // Struct declaration
    StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
        new StructFieldDef("x", DataType.SystemDataType.INT),
        new StructFieldDef("y", DataType.SystemDataType.INT)
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

    // Field access: int xVal = p.x;
    FieldAccessExpression fieldAccess = new FieldAccessExpression(
        new VariableExpression(Identifier.fromName("p")),
        "x"
    );
    VariableDeclarationImpl xValDecl = new VariableDeclarationImpl(
        "xVal",
        DataType.SystemDataType.INT,
        fieldAccess
    );
    program.addStatement(xValDecl);

    // Execute
    RootEvalContext context = new RootEvalContext();
    new EvalVisitor(context).visit(program);

    // Verify xVal contains the field value
    Data xValData = context.getVariable("xVal");
    assertNotNull(xValData);
    assertEquals(15, xValData.get());
  }

  @Test
  void testFieldAssignment() {
    // Create program:
    // struct Point { int x; int y; }
    // Point p = Point(10, 20);
    // p.y = 30;

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

    // Field assignment: p.y = 30;
    // Note: We need to wrap this in an expression statement
    // For now, let's test by accessing after assignment

    // Execute
    RootEvalContext context = new RootEvalContext();
    new EvalVisitor(context).visit(program);

    // Get the struct instance
    StructData pData = (StructData) context.getVariable("p");
    assertNotNull(pData);

    // Manually test field modification (since we don't have expression statement yet)
    Data newValue = new IntegerData(Identifier.EMPTY_IDENTIFIER, 30);
    pData.setField("y", newValue);

    // Verify the field was updated
    Data yData = pData.getField("y");
    assertEquals(30, yData.get());
  }

  @Test
  void testStructWithWrongFieldCount() {
    // Try to create Point(10) when it expects 2 fields
    Block program = new BlockImpl();

    StructDeclarationImpl structDecl = new StructDeclarationImpl("Point", Arrays.asList(
        new StructFieldDef("x", DataType.SystemDataType.INT),
        new StructFieldDef("y", DataType.SystemDataType.INT)
    ));
    program.addStatement(structDecl);

    // Only provide 1 field value
    StructInstantiation structInst = new StructInstantiation("Point",
        LiteralExpression.of(IntLiteral.of(10))
    );
    VariableDeclarationImpl pDecl = new VariableDeclarationImpl(
        "p",
        new DataTypeImpl("Point"),
        structInst
    );
    program.addStatement(pDecl);

    RootEvalContext context = new RootEvalContext();

    // Should throw exception
    assertThrows(IllegalArgumentException.class, () -> {
      new EvalVisitor(context).visit(program);
    });
  }
}
