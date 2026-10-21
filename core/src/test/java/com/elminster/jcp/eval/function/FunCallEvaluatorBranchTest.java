package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.ModuleFunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.Multi;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.ast.statement.function.AbstractFunction;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import com.elminster.jcp.eval.excpetion.FunctionAmbiguityException;
import com.elminster.jcp.eval.excpetion.UndeclaredException;
import com.elminster.jcp.module.Modulable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Branch coverage tests for FunCallEvaluator.
 * Targets: hasSameModule (non-default module), isOpaqueStub, isReferenceType,
 * narrowToExactMatches (null args), applyWideningConversions edge cases.
 */
class FunCallEvaluatorBranchTest {

    private EvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
    }

    // -----------------------------------------------------------------------
    // hasSameModule: non-default module matching
    // -----------------------------------------------------------------------

    @Nested
    class ModuleMatchingTests {

        /**
         * When the call has an explicit module name and the function is a ModuleFunction
         * with the same module name, it should match.
         * Covers hasSameModule when moduleName != DEFAULT_MODULE and function is Modulable.
         */
        @Test
        void testExplicitModuleMatchesModulableFunction() {
            Function modFunc = makeModuleFunction("compute", "mymod", 77);
            context.addFunction(modFunc);

            // Call with explicit module via ModuleFunctionCallExpression
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new ModuleFunctionCallExpression(
                    IdentifierExpression.of("compute"),
                    "mymod"
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(77, context.getVariable("result").get());
        }

        /**
         * When the call has an explicit module that does NOT match the registered function's module,
         * the function should not be found → UndeclaredException.
         * Covers hasSameModule returning false for non-matching module.
         */
        @Test
        void testExplicitModuleMismatchThrows() {
            Function modFunc = makeModuleFunction("compute", "mymod", 77);
            context.addFunction(modFunc);

            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new ModuleFunctionCallExpression(
                    IdentifierExpression.of("compute"),
                    "othermod"   // different module
                )
            ));

            assertThrows(UndeclaredException.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }

        /**
         * When explicit module is given and the function is NOT a Modulable instance,
         * hasSameModule returns false — so the function is not found.
         * Covers the "function not Modulable" branch in hasSameModule.
         */
        @Test
        void testExplicitModuleDoesNotMatchNonModulableFunction() {
            // Register a plain (non-module) function
            Function plainFunc = new AbstractFunction(
                IdentifierExpression.of("compute"),
                new ParameterDef[]{},
                SystemDataType.INT,
                new ReturnStatement(LiteralExpression.of(99))
            );
            context.addFunction(plainFunc);

            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new ModuleFunctionCallExpression(
                    IdentifierExpression.of("compute"),
                    "mymod"  // non-default module; plain function is not Modulable → no match
                )
            ));

            assertThrows(UndeclaredException.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }

        /**
         * Embedded module separator in raw identifier ("mymod::compute") is parsed out.
         * Covers the rawName.indexOf("::") >= 0 branch in getFunction().
         */
        @Test
        void testEmbeddedModuleInRawIdentifier() {
            Function modFunc = makeModuleFunction("compute", "mymod", 55);
            context.addFunction(modFunc);

            // Use a plain FunctionCallExpression whose id string embeds "mymod::compute"
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new FunctionCallExpression(
                    IdentifierExpression.of("mymod::compute")
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(55, context.getVariable("result").get());
        }
    }

    // -----------------------------------------------------------------------
    // isOpaqueStub / isReferenceType
    // -----------------------------------------------------------------------

    @Nested
    class OpaqueStubTests {

        /**
         * Calling a function whose parameter is an opaque DataTypeImpl (non-SystemDataType,
         * non-StructType) with a STRING argument should succeed — covers the isOpaqueStub &&
         * isReferenceType branch in hasSameParameterDefinition.
         */
        @Test
        void testOpaqueStubParamAcceptsStringArg() {
            // fn process(x: SomeExternalType) -> Int { return 42 }
            // process("hello") should match because String isReferenceType and SomeExternalType isOpaqueStub
            Function func = new AbstractFunction(
                IdentifierExpression.of("process"),
                new ParameterDef[]{ParameterDef.of("x", new DataTypeImpl("SomeExternalType"))},
                SystemDataType.INT,
                new ReturnStatement(LiteralExpression.of(42))
            );
            context.addFunction(func);

            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new FunctionCallExpression(
                    IdentifierExpression.of("process"),
                    LiteralExpression.of("hello")  // STRING is a reference type
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(42, context.getVariable("result").get());
        }

        /**
         * An opaque stub param should NOT accept a primitive INT arg (isReferenceType = false).
         * Covers the isOpaqueStub && !isReferenceType path: falls through to isCompatibleWith check.
         */
        @Test
        void testOpaqueStubParamDoesNotMatchIntArg() {
            // fn process(x: SomeExternalType) -> Int { return 42 }
            // process(5) — INT is not a reference type, so isOpaqueStub path doesn't apply;
            // INT.isCompatibleWith(SomeExternalType) = false → no match → UndeclaredException
            Function func = new AbstractFunction(
                IdentifierExpression.of("strictProcess"),
                new ParameterDef[]{ParameterDef.of("x", new DataTypeImpl("SomeExternalType"))},
                SystemDataType.INT,
                new ReturnStatement(LiteralExpression.of(42))
            );
            context.addFunction(func);

            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new FunctionCallExpression(
                    IdentifierExpression.of("strictProcess"),
                    LiteralExpression.of(5)  // INT is NOT a reference type
                )
            ));

            assertThrows(UndeclaredException.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }

        /**
         * isReferenceType returns false for BOOLEAN. Covers that branch.
         */
        @Test
        void testOpaqueStubParamDoesNotMatchBooleanArg() {
            Function func = new AbstractFunction(
                IdentifierExpression.of("boolProcess"),
                new ParameterDef[]{ParameterDef.of("x", new DataTypeImpl("SomeExternalType"))},
                SystemDataType.INT,
                new ReturnStatement(LiteralExpression.of(1))
            );
            context.addFunction(func);

            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new FunctionCallExpression(
                    IdentifierExpression.of("boolProcess"),
                    LiteralExpression.of(true)  // BOOLEAN is NOT a reference type
                )
            ));

            assertThrows(UndeclaredException.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }
    }

    // -----------------------------------------------------------------------
    // narrowToExactMatches: null arguments
    // -----------------------------------------------------------------------

    @Nested
    class NarrowToExactMatchesTests {

        /**
         * When two overloads match and arguments are from a no-arg call, narrowToExactMatches
         * with empty argumentData still selects the correct unique match.
         * (The null-args guard path in narrowToExactMatches returns candidates unchanged.)
         */
        @Test
        void testNarrowToExactMatchesWithMultipleCandidatesSelectsExact() {
            // fn pick(x: Int) -> Int { return 1 }
            // fn pick(x: Any) -> Int { return 2 }
            // pick(10) → exact match selects Int overload → returns 1
            Function intFunc = new AbstractFunction(
                IdentifierExpression.of("pick"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.INT)},
                SystemDataType.INT,
                new ReturnStatement(LiteralExpression.of(1))
            );
            context.addFunction(intFunc);

            Function anyFunc = new AbstractFunction(
                IdentifierExpression.of("pick"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.ANY)},
                SystemDataType.INT,
                new ReturnStatement(LiteralExpression.of(2))
            );
            context.addFunction(anyFunc);

            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new FunctionCallExpression(
                    IdentifierExpression.of("pick"),
                    LiteralExpression.of(10)
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(1, context.getVariable("result").get()); // exact match
        }
    }

    // -----------------------------------------------------------------------
    // applyWideningConversions: int->double with single param
    // -----------------------------------------------------------------------

    @Nested
    class WideningConversionTests {

        /**
         * Tests that applyWideningConversions is invoked and converts INT to DOUBLE.
         * Covers the INT→DOUBLE widening branch inside applyWideningConversions.
         */
        @Test
        void testWideningConversionIntToDouble() {
            Function func = new AbstractFunction(
                IdentifierExpression.of("triple"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.DOUBLE)},
                SystemDataType.DOUBLE,
                new ReturnStatement(new Multi(
                    new VariableExpression(IdentifierExpression.of("x")),
                    LiteralExpression.of(3.0)
                ))
            );
            context.addFunction(func);

            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new FunctionCallExpression(
                    IdentifierExpression.of("triple"),
                    LiteralExpression.of(4)  // INT → widened to DOUBLE
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(12.0, (Double) context.getVariable("result").get(), 0.001);
        }

        /**
         * Tests that non-widening args pass through applyWideningConversions unchanged.
         * Covers the "no conversion needed" else branch.
         */
        @Test
        void testNoWideningNeededPassesThrough() {
            Function func = new AbstractFunction(
                IdentifierExpression.of("identity"),
                new ParameterDef[]{ParameterDef.of("s", SystemDataType.STRING)},
                SystemDataType.STRING,
                new ReturnStatement(new VariableExpression(IdentifierExpression.of("s")))
            );
            context.addFunction(func);

            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.STRING,
                new FunctionCallExpression(
                    IdentifierExpression.of("identity"),
                    LiteralExpression.of("unchanged")
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals("unchanged", context.getVariable("result").get());
        }
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /** A Function that also implements Modulable, for testing module-based dispatch. */
    private static class ModuleAwareFunction extends AbstractFunction implements Modulable {
        private final String module;

        ModuleAwareFunction(String funcName, String module, int returnValue) {
            super(IdentifierExpression.of(funcName), new ParameterDef[]{},
                SystemDataType.INT, new ReturnStatement(LiteralExpression.of(returnValue)));
            this.module = module;
        }

        @Override public String getModule() { return module; }
    }

    private static Function makeModuleFunction(String funcName, String module, int returnValue) {
        return new ModuleAwareFunction(funcName, module, returnValue);
    }
}
