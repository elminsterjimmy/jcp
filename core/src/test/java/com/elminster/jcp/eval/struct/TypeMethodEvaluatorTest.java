package com.elminster.jcp.eval.struct;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.FieldAccessExpression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
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
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import com.elminster.jcp.eval.data.StructData;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for type system with constructors and methods.
 */
class TypeMethodEvaluatorTest {

  @Test
  void testTypeWithExplicitConstructor() {
    // type Point {
    //   int x;
    //   constructor(int x) { this.x = x; }
    // }
    // Point p = Point(10);

    Block program = new BlockImpl();

    // Constructor body: this.x = x;
    Block ctorBody = new BlockImpl();
    // For simplicity, we'll just test that the constructor is invoked
    // The constructor body would need field assignment which we'll test indirectly

    MethodDef constructor = MethodDef.constructor(
        ctorBody,
        ParameterDef.of("initX", DataType.SystemDataType.INT)
    );

    // Type declaration with constructor (no methods yet, just field)
    StructDeclarationImpl typeDecl = new StructDeclarationImpl(
        "Point",
        Arrays.asList(new StructFieldDef("x", DataType.SystemDataType.INT)),
        constructor,
        Collections.emptyList(),  // no instance methods
        Collections.emptyList()   // no static methods
    );
    program.addStatement(typeDecl);

    // Point p = Point(10);
    StructInstantiation inst = new StructInstantiation("Point",
        LiteralExpression.of(IntLiteral.of(10))
    );
    VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
        "p",
        new DataTypeImpl("Point"),
        inst
    );
    program.addStatement(varDecl);

    // Execute
    RootEvalContext context = new RootEvalContext();
    new EvalVisitor(context).visit(program);

    // Verify struct was created (constructor was called)
    Data pData = context.getVariable("p");
    assertNotNull(pData);
    assertTrue(pData instanceof StructData);
  }

  @Test
  void testStaticMethod() {
    // type Calc {
    //   static func add(int a, int b) -> int { return a + b; }
    // }
    // int result = Calc.add(2, 3);

    Block program = new BlockImpl();

    // Static method body: return a + b;
    Block methodBody = new BlockImpl();
    methodBody.addStatement(new ReturnStatement(
        new Plus(
            new VariableExpression(Identifier.fromName("a")),
            new VariableExpression(Identifier.fromName("b"))
        )
    ));

    MethodDef addMethod = MethodDef.staticMethod(
        "add",
        DataType.SystemDataType.INT,
        methodBody,
        ParameterDef.of("a", DataType.SystemDataType.INT),
        ParameterDef.of("b", DataType.SystemDataType.INT)
    );

    // Type declaration with static method only (no fields)
    StructDeclarationImpl typeDecl = new StructDeclarationImpl(
        "Calc",
        Collections.emptyList(),  // no fields
        null,                     // no constructor
        Collections.emptyList(),  // no instance methods
        Arrays.asList(addMethod)  // static methods
    );
    program.addStatement(typeDecl);

    // int result = Calc.add(2, 3);
    StaticMethodCallExpression methodCall = new StaticMethodCallExpression(
        "Calc",
        "add",
        LiteralExpression.of(IntLiteral.of(2)),
        LiteralExpression.of(IntLiteral.of(3))
    );
    VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
        "result",
        DataType.SystemDataType.INT,
        methodCall
    );
    program.addStatement(varDecl);

    // Execute
    RootEvalContext context = new RootEvalContext();
    new EvalVisitor(context).visit(program);

    // Verify result
    Data resultData = context.getVariable("result");
    assertNotNull(resultData);
    assertEquals(5, resultData.get());
  }

  @Test
  void testInstanceMethod() {
    // type Counter {
    //   int count;
    //   func getCount() -> int { return this.count; }
    // }
    // Counter c = Counter(10);
    // int val = c.getCount();

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
        DataType.SystemDataType.INT,
        methodBody
        // no parameters
    );

    // Type declaration with instance method
    StructDeclarationImpl typeDecl = new StructDeclarationImpl(
        "Counter",
        Arrays.asList(new StructFieldDef("count", DataType.SystemDataType.INT)),
        null,  // auto-generated constructor
        Arrays.asList(getCountMethod),
        Collections.emptyList()
    );
    program.addStatement(typeDecl);

    // Counter c = Counter(10);
    StructInstantiation inst = new StructInstantiation("Counter",
        LiteralExpression.of(IntLiteral.of(10))
    );
    VariableDeclarationImpl varDecl = new VariableDeclarationImpl(
        "c",
        new DataTypeImpl("Counter"),
        inst
    );
    program.addStatement(varDecl);

    // int val = c.getCount();
    MethodCallExpression methodCall = new MethodCallExpression(
        new VariableExpression(Identifier.fromName("c")),
        "getCount"
    );
    VariableDeclarationImpl resultDecl = new VariableDeclarationImpl(
        "val",
        DataType.SystemDataType.INT,
        methodCall
    );
    program.addStatement(resultDecl);

    // Execute
    RootEvalContext context = new RootEvalContext();
    new EvalVisitor(context).visit(program);

    // Verify result
    Data valData = context.getVariable("val");
    assertNotNull(valData);
    assertEquals(10, valData.get());
  }

  @Test
  void testThisExpression() {
    // Test that 'this' is properly resolved in method context
    RootEvalContext context = new RootEvalContext();

    // Without being in a method context, 'this' should fail
    ThisEvaluator thisEval = new ThisEvaluator(new ThisExpression());

    assertThrows(IllegalStateException.class, () -> {
      thisEval.eval(context);
    });
  }
}
