package com.elminster.jcp.eval;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import com.elminster.jcp.eval.excpetion.EvaluationException;
import com.elminster.jcp.util.ClassConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for module:: prefix type disambiguation in eval mode (issue #73).
 */
class ModulePrefixTypeDisambiguationEvalTest {

    private RootEvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
    }

    /**
     * guava::Strings.repeat("ab", 3) in eval mode.
     * Registers Guava Strings under module "guava", verifying the function lookup
     * resolves through module-prefixed type resolution.
     */
    @Test
    void testGuavaStringsRepeatWithModulePrefix() {
        ClassConverter.registerClass(com.google.common.base.Strings.class, context, "guava");

        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
            new StaticMethodCallExpression(
                "guava::Strings",
                "repeat",
                LiteralExpression.of("ab"),
                LiteralExpression.of(IntLiteral.of(3))
            )
        ));

        new EvalVisitor(context).visit(program);
        // If no exception, the call resolved correctly.
        // The eval visitor doesn't capture return values from expression statements,
        // but successful execution proves the module:: prefix resolved.
    }

    /**
     * Strings.length("hello") works when only base module Strings is registered.
     * Simple name resolution unchanged (no collision).
     */
    @Test
    void testSimpleNameResolutionNoCollision() {
        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
            new StaticMethodCallExpression(
                "Strings",
                "length",
                LiteralExpression.of("hello")
            )
        ));

        assertDoesNotThrow(() -> new EvalVisitor(context).visit(program));
    }

    /**
     * base::Strings.length("hello") with explicit base module prefix.
     */
    @Test
    void testExplicitBaseModulePrefix() {
        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
            new StaticMethodCallExpression(
                "base::Strings",
                "length",
                LiteralExpression.of("hello")
            )
        ));

        assertDoesNotThrow(() -> new EvalVisitor(context).visit(program));
    }

    /**
     * unknownmod::Strings should fail with a clear error.
     * Since getDataType returns null for unknown module, StaticMethodCallEvaluator
     * will throw because the type resolution returns null.
     */
    @Test
    void testUnknownModulePrefixFails() {
        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
            new StaticMethodCallExpression(
                "unknownmod::Strings",
                "length",
                LiteralExpression.of("test")
            )
        ));

        assertThrows(Exception.class, () -> new EvalVisitor(context).visit(program));
    }

    /**
     * When both Guava and base Strings are registered, bare "Strings" is ambiguous.
     */
    @Test
    void testAmbiguousSimpleNameWithBothRegistered() {
        ClassConverter.registerClass(com.google.common.base.Strings.class, context, "guava");

        Block program = new BlockImpl();
        program.addStatement(new ExpressionStatement(
            new StaticMethodCallExpression(
                "Strings",
                "length",
                LiteralExpression.of("test")
            )
        ));

        EvaluationException ex = assertThrows(EvaluationException.class,
            () -> new EvalVisitor(context).visit(program));
        assertTrue(ex.getMessage().contains("ambiguous"),
            "Error should mention ambiguity");
        assertTrue(ex.getMessage().contains("module::SimpleName"),
            "Error should suggest module:: syntax");
    }

    @Nested
    class GuavaIntMathNonCollision {
        /**
         * guava::IntMath.factorial(5) — explicit module prefix on non-colliding type.
         */
        @Test
        void testGuavaIntMathWithModulePrefix() {
            ClassConverter.registerClass(com.google.common.math.IntMath.class, context, "guava");

            Block program = new BlockImpl();
            program.addStatement(new ExpressionStatement(
                new StaticMethodCallExpression(
                    "guava::IntMath",
                    "factorial",
                    LiteralExpression.of(IntLiteral.of(5))
                )
            ));

            assertDoesNotThrow(() -> new EvalVisitor(context).visit(program));
        }
    }

    @Nested
    class ModulePrefixedGetDataType {

        @Test
        void testGetDataTypeWithModulePrefix() {
            ClassConverter.registerClass(com.google.common.base.Strings.class, context, "guava");

            DataType resolved = context.getDataType("guava::Strings");
            assertNotNull(resolved, "guava::Strings should resolve");
            assertEquals("Strings", resolved.getName());
            assertEquals("com.google.common.base.Strings", resolved.getFqn());
        }

        @Test
        void testGetDataTypeWithBasePrefixForBaseModule() {
            DataType resolved = context.getDataType("base::Strings");
            assertNotNull(resolved, "base::Strings should resolve");
            assertEquals("Strings", resolved.getName());
        }

        @Test
        void testGetDataTypeReturnsNullForUnknownModule() {
            DataType resolved = context.getDataType("unknownmod::Strings");
            assertNull(resolved, "unknownmod::Strings should return null");
        }

        @Test
        void testGetDataTypeReturnsNullForUnknownSimpleName() {
            DataType resolved = context.getDataType("guava::NonExistent");
            assertNull(resolved, "guava::NonExistent should return null");
        }
    }
}
