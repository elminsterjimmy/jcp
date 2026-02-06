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
import com.elminster.jcp.ast.statement.declaration.MethodDef;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import com.elminster.jcp.eval.data.IntegerData;
import com.elminster.jcp.eval.data.StructData;
import com.elminster.jcp.eval.data.StructType;
import org.junit.jupiter.api.Nested;
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

  /**
   * Tests that creating struct with wrong field count throws IllegalArgumentException.
   * <pre>
   * struct Point { x: Int, y: Int }
   * Point p = Point(10)  // throws: expects 2 fields, got 1
   * </pre>
   */
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

  /**
   * Tests that instantiating an unknown struct type throws IllegalArgumentException.
   * <pre>
   * UnknownType x = UnknownType()  // throws: Unknown struct type
   * </pre>
   */
  @Test
  void testUnknownStructType() {
    RootEvalContext context = new RootEvalContext();

    // Try to instantiate a type that doesn't exist
    StructInstantiation structInst = new StructInstantiation("UnknownType");

    StructInstantiationEvaluator evaluator = new StructInstantiationEvaluator(structInst);

    assertThrows(IllegalArgumentException.class, () -> evaluator.eval(context));
  }

  /**
   * Tests struct with type incompatible field values throws CannotCastException.
   * <pre>
   * struct Point { x: Int, y: Int }
   * Point p = Point("hello", 20)  // throws: String not castable to Int
   * </pre>
   */
  @Test
  void testStructWithIncompatibleFieldType() {
    RootEvalContext context = new RootEvalContext();

    // Register struct type
    StructType pointType = new StructType("Point", Arrays.asList(
        new StructFieldDef("x", DataType.SystemDataType.INT),
        new StructFieldDef("y", DataType.SystemDataType.INT)
    ));
    context.addDataType(pointType);

    // Create struct instance with string where int expected
    StructInstantiation structInst = new StructInstantiation("Point",
        LiteralExpression.of(com.elminster.jcp.ast.expression.literal.StringLiteral.of("hello")),
        LiteralExpression.of(IntLiteral.of(20))
    );

    StructInstantiationEvaluator evaluator = new StructInstantiationEvaluator(structInst);

    assertThrows(com.elminster.jcp.eval.excpetion.CannotCastException.class, () ->
        evaluator.eval(context));
  }

  /**
   * Tests struct with boolean field and default value.
   * <pre>
   * struct Flags { enabled: Boolean, active: Boolean }
   * Flags f = Flags(true, false)
   * // f.enabled = true, f.active = false
   * </pre>
   */
  @Test
  void testStructWithBooleanFields() {
    RootEvalContext context = new RootEvalContext();

    // Register struct type with boolean fields
    StructType flagsType = new StructType("Flags", Arrays.asList(
        new StructFieldDef("enabled", DataType.SystemDataType.BOOLEAN),
        new StructFieldDef("active", DataType.SystemDataType.BOOLEAN)
    ));
    context.addDataType(flagsType);

    // Create struct instance
    StructInstantiation structInst = new StructInstantiation("Flags",
        LiteralExpression.of(com.elminster.jcp.ast.expression.literal.BooleanLiteral.of(true)),
        LiteralExpression.of(com.elminster.jcp.ast.expression.literal.BooleanLiteral.of(false))
    );

    StructInstantiationEvaluator evaluator = new StructInstantiationEvaluator(structInst);
    Data result = evaluator.eval(context);

    assertNotNull(result);
    assertTrue(result instanceof StructData);

    StructData structData = (StructData) result;
    assertEquals(true, structData.getField("enabled").get());
    assertEquals(false, structData.getField("active").get());
  }

  /**
   * Tests struct with string field.
   * <pre>
   * struct Person { name: String, age: Int }
   * Person p = Person("Alice", 30)
   * // p.name = "Alice", p.age = 30
   * </pre>
   */
  @Test
  void testStructWithStringField() {
    RootEvalContext context = new RootEvalContext();

    // Register struct type with string field
    StructType personType = new StructType("Person", Arrays.asList(
        new StructFieldDef("name", DataType.SystemDataType.STRING),
        new StructFieldDef("age", DataType.SystemDataType.INT)
    ));
    context.addDataType(personType);

    // Create struct instance
    StructInstantiation structInst = new StructInstantiation("Person",
        LiteralExpression.of(com.elminster.jcp.ast.expression.literal.StringLiteral.of("Alice")),
        LiteralExpression.of(IntLiteral.of(30))
    );

    StructInstantiationEvaluator evaluator = new StructInstantiationEvaluator(structInst);
    Data result = evaluator.eval(context);

    assertNotNull(result);
    assertTrue(result instanceof StructData);

    StructData structData = (StructData) result;
    assertEquals("Alice", structData.getField("name").get());
    assertEquals(30, structData.getField("age").get());
  }

  @Nested
  class ExplicitConstructorTests {

    /**
     * Tests explicit constructor with wrong argument count throws exception.
     * <pre>
     * type Counter {
     *   count: Int
     *   constructor(start: Int) { }
     * }
     * Counter c = Counter()  // throws: expects 1 argument
     * </pre>
     */
    @Test
    void testExplicitConstructor_WrongArgCount() {
      RootEvalContext context = new RootEvalContext();

      Block constructorBody = new BlockImpl();
      MethodDef constructor = MethodDef.constructor(
          constructorBody,
          ParameterDef.of("start", DataType.SystemDataType.INT)
      );

      StructType counterType = new StructType("Counter", Arrays.asList(
          new StructFieldDef("count", DataType.SystemDataType.INT)
      ), constructor);
      context.addDataType(counterType);

      // Try to instantiate with no arguments (constructor expects 1)
      StructInstantiation structInst = new StructInstantiation("Counter");

      StructInstantiationEvaluator evaluator = new StructInstantiationEvaluator(structInst);

      assertThrows(IllegalArgumentException.class, () -> evaluator.eval(context));
    }

    /**
     * Tests explicit constructor with wrong argument type throws CannotCastException.
     * <pre>
     * type Counter {
     *   count: Int
     *   constructor(start: Int) { }
     * }
     * Counter c = Counter("hello")  // throws: String not castable to Int
     * </pre>
     */
    @Test
    void testExplicitConstructor_WrongArgType() {
      RootEvalContext context = new RootEvalContext();

      Block constructorBody = new BlockImpl();
      MethodDef constructor = MethodDef.constructor(
          constructorBody,
          ParameterDef.of("start", DataType.SystemDataType.INT)
      );

      StructType counterType = new StructType("Counter", Arrays.asList(
          new StructFieldDef("count", DataType.SystemDataType.INT)
      ), constructor);
      context.addDataType(counterType);

      // Try to instantiate with wrong type
      StructInstantiation structInst = new StructInstantiation("Counter",
          LiteralExpression.of(com.elminster.jcp.ast.expression.literal.StringLiteral.of("hello"))
      );

      StructInstantiationEvaluator evaluator = new StructInstantiationEvaluator(structInst);

      assertThrows(com.elminster.jcp.eval.excpetion.CannotCastException.class, () -> evaluator.eval(context));
    }

    /**
     * Tests explicit constructor with String field getting default value.
     * <pre>
     * type Named {
     *   name: String
     *   constructor() { }
     * }
     * Named n = Named()  // name gets default ""
     * </pre>
     */
    @Test
    void testExplicitConstructor_StringFieldDefault() {
      RootEvalContext context = new RootEvalContext();

      Block constructorBody = new BlockImpl();  // Empty constructor body
      MethodDef constructor = MethodDef.constructor(constructorBody);  // No parameters

      StructType namedType = new StructType("Named", Arrays.asList(
          new StructFieldDef("name", DataType.SystemDataType.STRING)
      ), constructor);
      context.addDataType(namedType);

      // Instantiate with no arguments
      StructInstantiation structInst = new StructInstantiation("Named");

      StructInstantiationEvaluator evaluator = new StructInstantiationEvaluator(structInst);
      Data result = evaluator.eval(context);

      assertNotNull(result);
      assertTrue(result instanceof StructData);

      StructData structData = (StructData) result;
      assertEquals("", structData.getField("name").get());  // Default string value
    }

    /**
     * Tests explicit constructor with Boolean field getting default value.
     * <pre>
     * type Flags {
     *   enabled: Boolean
     *   constructor() { }
     * }
     * Flags f = Flags()  // enabled gets default false
     * </pre>
     */
    @Test
    void testExplicitConstructor_BooleanFieldDefault() {
      RootEvalContext context = new RootEvalContext();

      Block constructorBody = new BlockImpl();  // Empty constructor body
      MethodDef constructor = MethodDef.constructor(constructorBody);  // No parameters

      StructType flagsType = new StructType("Flags", Arrays.asList(
          new StructFieldDef("enabled", DataType.SystemDataType.BOOLEAN)
      ), constructor);
      context.addDataType(flagsType);

      // Instantiate with no arguments
      StructInstantiation structInst = new StructInstantiation("Flags");

      StructInstantiationEvaluator evaluator = new StructInstantiationEvaluator(structInst);
      Data result = evaluator.eval(context);

      assertNotNull(result);
      assertTrue(result instanceof StructData);

      StructData structData = (StructData) result;
      assertEquals(false, structData.getField("enabled").get());  // Default boolean value
    }

    /**
     * Tests explicit constructor with custom type field getting null default.
     * <pre>
     * type Container {
     *   data: AnotherType
     *   constructor() { }
     * }
     * Container c = Container()  // data gets null
     * </pre>
     */
    @Test
    void testExplicitConstructor_CustomTypeFieldDefault() {
      RootEvalContext context = new RootEvalContext();

      // First register a custom type
      StructType innerType = new StructType("Inner", Arrays.asList(
          new StructFieldDef("val", DataType.SystemDataType.INT)
      ));
      context.addDataType(innerType);

      // Now create a type with custom type field
      Block constructorBody = new BlockImpl();  // Empty constructor body
      MethodDef constructor = MethodDef.constructor(constructorBody);  // No parameters

      StructType containerType = new StructType("Container", Arrays.asList(
          new StructFieldDef("data", innerType)
      ), constructor);
      context.addDataType(containerType);

      // Instantiate with no arguments
      StructInstantiation structInst = new StructInstantiation("Container");

      StructInstantiationEvaluator evaluator = new StructInstantiationEvaluator(structInst);
      Data result = evaluator.eval(context);

      assertNotNull(result);
      assertTrue(result instanceof StructData);

      StructData structData = (StructData) result;
      // Custom type defaults to null
      assertNull(structData.getField("data").get());
    }
  }
}
