package com.elminster.jcp.eval.declare;

import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.MethodDef;
import com.elminster.jcp.ast.statement.declaration.StructDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.StructType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TypeDeclarationEvaluator.
 */
class TypeDeclarationEvaluatorTest {

    /**
     * Tests type declaration with instance method (no parameters).
     * <pre>
     * type Counter {
     *   count: Int
     *   fun getValue() -> Int { }
     * }
     * </pre>
     */
    @Test
    void testTypeWithInstanceMethod_NoParams() {
        RootEvalContext context = new RootEvalContext();

        Block methodBody = new BlockImpl();
        MethodDef getValueMethod = new MethodDef("getValue", SystemDataType.INT, methodBody);

        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Counter",
            Arrays.asList(new StructFieldDef("count", SystemDataType.INT)),
            null,  // no constructor
            Arrays.asList(getValueMethod),  // instance method
            Collections.emptyList()  // no static methods
        );

        new EvalVisitor(context).visit(new BlockImpl(typeDecl));

        // Type should be registered
        StructType type = (StructType) context.getDataType("Counter");
        assertNotNull(type);
        assertEquals("Counter", type.getName());
    }

    /**
     * Tests type declaration with static method (no parameters).
     * <pre>
     * type Math {
     *   static fun zero() -> Int { }
     * }
     * </pre>
     */
    @Test
    void testTypeWithStaticMethod_NoParams() {
        RootEvalContext context = new RootEvalContext();

        Block methodBody = new BlockImpl();
        MethodDef zeroMethod = MethodDef.staticMethod("zero", SystemDataType.INT, methodBody);

        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Math",
            Collections.emptyList(),  // no fields
            null,  // no constructor
            Collections.emptyList(),  // no instance methods
            Arrays.asList(zeroMethod)  // static method
        );

        new EvalVisitor(context).visit(new BlockImpl(typeDecl));

        // Type should be registered
        StructType type = (StructType) context.getDataType("Math");
        assertNotNull(type);
        assertEquals("Math", type.getName());
    }

    /**
     * Tests type declaration with instance method with parameters.
     * <pre>
     * type Adder {
     *   base: Int
     *   fun add(x: Int) -> Int { }
     * }
     * </pre>
     */
    @Test
    void testTypeWithInstanceMethod_WithParams() {
        RootEvalContext context = new RootEvalContext();

        Block methodBody = new BlockImpl();
        MethodDef addMethod = new MethodDef("add", SystemDataType.INT, methodBody,
            ParameterDef.of("x", SystemDataType.INT));

        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Adder",
            Arrays.asList(new StructFieldDef("base", SystemDataType.INT)),
            null,  // no constructor
            Arrays.asList(addMethod),  // instance method
            Collections.emptyList()  // no static methods
        );

        new EvalVisitor(context).visit(new BlockImpl(typeDecl));

        // Type should be registered
        StructType type = (StructType) context.getDataType("Adder");
        assertNotNull(type);
        assertEquals("Adder", type.getName());
    }

    /**
     * Tests type declaration with static method with parameters.
     * <pre>
     * type Math {
     *   static fun add(a: Int, b: Int) -> Int { }
     * }
     * </pre>
     */
    @Test
    void testTypeWithStaticMethod_WithParams() {
        RootEvalContext context = new RootEvalContext();

        Block methodBody = new BlockImpl();
        MethodDef addMethod = MethodDef.staticMethod("add", SystemDataType.INT, methodBody,
            ParameterDef.of("a", SystemDataType.INT),
            ParameterDef.of("b", SystemDataType.INT));

        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Math",
            Collections.emptyList(),  // no fields
            null,  // no constructor
            Collections.emptyList(),  // no instance methods
            Arrays.asList(addMethod)  // static method
        );

        new EvalVisitor(context).visit(new BlockImpl(typeDecl));

        // Type should be registered
        StructType type = (StructType) context.getDataType("Math");
        assertNotNull(type);
        assertEquals("Math", type.getName());
    }

    /**
     * Tests type declaration with instance method with null parameters array.
     * This covers the branch where getParameters() returns null.
     */
    @Test
    void testTypeWithInstanceMethod_NullParams() {
        RootEvalContext context = new RootEvalContext();

        Block methodBody = new BlockImpl();
        // Create method with null parameters using direct constructor
        MethodDef getValueMethod = new MethodDef("getValue", SystemDataType.INT, methodBody, (ParameterDef[]) null);

        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Counter",
            Arrays.asList(new StructFieldDef("count", SystemDataType.INT)),
            null,
            Arrays.asList(getValueMethod),
            Collections.emptyList()
        );

        new EvalVisitor(context).visit(new BlockImpl(typeDecl));

        StructType type = (StructType) context.getDataType("Counter");
        assertNotNull(type);
    }

    /**
     * Tests type declaration with static method with null parameters array.
     * This covers the branch where getParameters() returns null.
     */
    @Test
    void testTypeWithStaticMethod_NullParams() {
        RootEvalContext context = new RootEvalContext();

        Block methodBody = new BlockImpl();
        // Create static method with null parameters
        MethodDef zeroMethod = MethodDef.staticMethod("zero", SystemDataType.INT, methodBody, (ParameterDef[]) null);

        StructDeclarationImpl typeDecl = new StructDeclarationImpl(
            "Math",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            Arrays.asList(zeroMethod)
        );

        new EvalVisitor(context).visit(new BlockImpl(typeDecl));

        StructType type = (StructType) context.getDataType("Math");
        assertNotNull(type);
    }
}
