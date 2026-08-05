package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.Multi;
import com.elminster.jcp.ast.expression.operation.Plus;
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
import com.elminster.jcp.eval.excpetion.AlreadyDeclaredException.FunctionAlreadyDeclaredException;
import com.elminster.jcp.eval.excpetion.FunctionAmbiguityException;
import com.elminster.jcp.eval.excpetion.UndeclaredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FunCallEvaluator.
 */
class FunCallEvaluatorTest {

    private EvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
    }

    @Nested
    class BasicFunctionCallTests {

        @Test
        void testCallFunctionNoArgs() {
            // fn getAnswer() -> Int { return 42 }
            // result = getAnswer()  -> 42
            Function func = new AbstractFunction(
                IdentifierExpression.of("getAnswer"),
                new ParameterDef[]{},
                SystemDataType.INT,
                new ReturnStatement(LiteralExpression.of(42))
            );
            context.addFunction(func);

            // Call: getAnswer()
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new FunctionCallExpression(IdentifierExpression.of("getAnswer"))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(42, context.getVariable("result").get());
        }

        @Test
        void testCallFunctionWithArgs() {
            // fn add(a: Int, b: Int) -> Int { return a + b }
            // result = add(3, 5)  -> 8
            Function func = new AbstractFunction(
                IdentifierExpression.of("add"),
                new ParameterDef[]{
                    ParameterDef.of("a", SystemDataType.INT),
                    ParameterDef.of("b", SystemDataType.INT)
                },
                SystemDataType.INT,
                new ReturnStatement(new Plus(
                    new VariableExpression(IdentifierExpression.of("a")),
                    new VariableExpression(IdentifierExpression.of("b"))
                ))
            );
            context.addFunction(func);

            // Call: add(3, 5)
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new FunctionCallExpression(
                    IdentifierExpression.of("add"),
                    LiteralExpression.of(3),
                    LiteralExpression.of(5)
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(8, context.getVariable("result").get());
        }
    }

    @Nested
    class UndeclaredFunctionTests {

        @Test
        void testCallUndeclaredFunction() {
            // result = nonExistentFunction()  -> UndeclaredException
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new FunctionCallExpression(IdentifierExpression.of("nonExistentFunction"))
            ));

            assertThrows(UndeclaredException.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }

        @Test
        void testCallFunctionWrongArgCount() {
            // fn greet(name: String) -> String { return name }
            // result = greet()  -> UndeclaredException (wrong arg count)
            Function func = new AbstractFunction(
                IdentifierExpression.of("greet"),
                new ParameterDef[]{ParameterDef.of("name", SystemDataType.STRING)},
                SystemDataType.STRING,
                new ReturnStatement(new VariableExpression(IdentifierExpression.of("name")))
            );
            context.addFunction(func);

            // Call with wrong number of arguments: greet()
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.STRING,
                new FunctionCallExpression(IdentifierExpression.of("greet"))
            ));

            assertThrows(UndeclaredException.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }

        @Test
        void testCallFunctionWrongArgType() {
            // fn square(x: Int) -> Int { return x * x }
            // result = square("hello")  -> UndeclaredException (String not castable to Int)
            Function func = new AbstractFunction(
                IdentifierExpression.of("square"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.INT)},
                SystemDataType.INT,
                new ReturnStatement(new Multi(
                    new VariableExpression(IdentifierExpression.of("x")),
                    new VariableExpression(IdentifierExpression.of("x"))
                ))
            );
            context.addFunction(func);

            // Call with wrong type: square("hello") - String is not castable to Int
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new FunctionCallExpression(
                    IdentifierExpression.of("square"),
                    LiteralExpression.of("hello")
                )
            ));

            assertThrows(UndeclaredException.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }
    }

    @Nested
    class FunctionOverloadTests {

        @Test
        void testOverloadedFunctionSelection() {
            // fn process(x: Int) -> Int { return x }
            // fn process(s: String) -> String { return s }
            // intResult = process(100)      -> 100 (selects Int overload)
            // stringResult = process("hello") -> "hello" (selects String overload)
            Function intFunc = new AbstractFunction(
                IdentifierExpression.of("process"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.INT)},
                SystemDataType.INT,
                new ReturnStatement(new VariableExpression(IdentifierExpression.of("x")))
            );
            context.addFunction(intFunc);

            Function stringFunc = new AbstractFunction(
                IdentifierExpression.of("process"),
                new ParameterDef[]{ParameterDef.of("s", SystemDataType.STRING)},
                SystemDataType.STRING,
                new ReturnStatement(new VariableExpression(IdentifierExpression.of("s")))
            );
            context.addFunction(stringFunc);

            // Call with int: process(100)
            Block program1 = new BlockImpl();
            program1.addStatement(new VariableDeclarationImpl(
                "intResult",
                SystemDataType.INT,
                new FunctionCallExpression(
                    IdentifierExpression.of("process"),
                    LiteralExpression.of(100)
                )
            ));

            new EvalVisitor(context).visit(program1);
            assertEquals(100, context.getVariable("intResult").get());

            // Call with string: process("hello")
            Block program2 = new BlockImpl();
            program2.addStatement(new VariableDeclarationImpl(
                "stringResult",
                SystemDataType.STRING,
                new FunctionCallExpression(
                    IdentifierExpression.of("process"),
                    LiteralExpression.of("hello")
                )
            ));

            new EvalVisitor(context).visit(program2);
            assertEquals("hello", context.getVariable("stringResult").get());
        }

        @Test
        void testDuplicateFunctionDeclaration() {
            // fn duplicate(x: Int) -> Int { return x }
            // fn duplicate(y: Int) -> Int { return y }  -> FunctionAlreadyDeclaredException
            Function func1 = new AbstractFunction(
                IdentifierExpression.of("duplicate"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.INT)},
                SystemDataType.INT,
                new ReturnStatement(new VariableExpression(IdentifierExpression.of("x")))
            );
            context.addFunction(func1);

            // Try to define another function with the same signature
            // fn duplicate(y: Int) { return y }  (duplicate with same signature)
            Function func2 = new AbstractFunction(
                IdentifierExpression.of("duplicate"),
                new ParameterDef[]{ParameterDef.of("y", SystemDataType.INT)},
                SystemDataType.INT,
                new ReturnStatement(new VariableExpression(IdentifierExpression.of("y")))
            );

            // Should throw FunctionAlreadyDeclaredException at registration time
            assertThrows(FunctionAlreadyDeclaredException.class, () ->
                context.addFunction(func2)
            );
        }
    }

    @Nested
    class ParameterCompatibilityTests {

        /**
         * Tests calling function with compatible types (Int castable to Any).
         * <pre>
         * fn acceptAny(x: Any) -> Any { return x }
         * result = acceptAny(42)  // returns 42
         * </pre>
         */
        @Test
        void testCallWithCompatibleTypes() {
            // fn acceptAny(x: Any) -> Any { return x }
            // result = acceptAny(42)  -> 42 (Int is castable to Any)
            Function func = new AbstractFunction(
                IdentifierExpression.of("acceptAny"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.ANY)},
                SystemDataType.ANY,
                new ReturnStatement(new VariableExpression(IdentifierExpression.of("x")))
            );
            context.addFunction(func);

            // Call with Int (which is castable to Any)
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.ANY,
                new FunctionCallExpression(
                    IdentifierExpression.of("acceptAny"),
                    LiteralExpression.of(42)
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(42, context.getVariable("result").get());
        }
    }

    @Nested
    class AmbiguityTests {

        /**
         * Tests that ambiguous function call throws FunctionAmbiguityException.
         * <pre>
         * fn ambig(x: Any) -> Int { return 1 }
         * fn ambig(x: Numeric) -> Int { return 2 }
         * ambig(42)  // throws FunctionAmbiguityException (both match)
         * </pre>
         */
        @Test
        void testAmbiguousFunctionCall() {
            // fn ambig(x: Any) -> Int { return 1 }
            Function func1 = new AbstractFunction(
                IdentifierExpression.of("ambig"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.ANY)},
                SystemDataType.INT,
                new ReturnStatement(LiteralExpression.of(1))
            );
            context.addFunction(func1);

            // fn ambig(x: Numeric) -> Int { return 2 }
            // Both match for Int argument since Int.isCastableTo(Any) and Int.isCastableTo(Numeric)
            Function func2 = new AbstractFunction(
                IdentifierExpression.of("ambig"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.NUMERIC)},
                SystemDataType.INT,
                new ReturnStatement(LiteralExpression.of(2))
            );
            context.addFunction(func2);

            // Call with Int - both functions match
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new FunctionCallExpression(
                    IdentifierExpression.of("ambig"),
                    LiteralExpression.of(42)
                )
            ));

            assertThrows(FunctionAmbiguityException.class, () ->
                new EvalVisitor(context).visit(program)
            );
        }
    }

    @Nested
    class EmptyParameterTests {

        /**
         * Tests calling function with empty parameters (empty array).
         * <pre>
         * fn noParams() -> Int { return 99 }
         * result = noParams()  // returns 99
         * </pre>
         */
        @Test
        void testFunctionWithEmptyParameterDefs() {
            // fn noParams() -> Int { return 99 }
            // AbstractFunction with empty parameters
            Function func = new AbstractFunction(
                IdentifierExpression.of("noParams"),
                new ParameterDef[0],  // empty parameters
                SystemDataType.INT,
                new ReturnStatement(LiteralExpression.of(99))
            );
            context.addFunction(func);

            // Call: noParams()
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new FunctionCallExpression(IdentifierExpression.of("noParams"))
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(99, context.getVariable("result").get());
        }
    }

    @Nested
    class TypePromotionTests {

        /**
         * Tests int to double type promotion in function calls.
         * This is the key test case for issue #13: Function lookup fails for numeric type promotion.
         * <pre>
         * fn doubleIt(x: Double) -> Double { return x * 2.0 }
         * result = doubleIt(5)  // int argument, double parameter - should return 10.0
         * </pre>
         */
        @Test
        void testIntToDoublePromotion() {
            // fn doubleIt(x: Double) -> Double { return x * 2.0 }
            Function func = new AbstractFunction(
                IdentifierExpression.of("doubleIt"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.DOUBLE)},
                SystemDataType.DOUBLE,
                new ReturnStatement(new Multi(
                    new VariableExpression(IdentifierExpression.of("x")),
                    LiteralExpression.of(2.0)
                ))
            );
            context.addFunction(func);

            // Call with int: doubleIt(5)
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new FunctionCallExpression(
                    IdentifierExpression.of("doubleIt"),
                    LiteralExpression.of(5)  // INT literal, not DOUBLE
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(10.0, (Double) context.getVariable("result").get(), 0.001);
        }

        /**
         * Tests int to double promotion with multiple parameters.
         * <pre>
         * fn addDoubles(a: Double, b: Double) -> Double { return a + b }
         * result = addDoubles(3, 5)  // both int args, double params - should return 8.0
         * </pre>
         */
        @Test
        void testIntToDoublePromotion_MultipleParams() {
            // fn addDoubles(a: Double, b: Double) -> Double { return a + b }
            Function func = new AbstractFunction(
                IdentifierExpression.of("addDoubles"),
                new ParameterDef[]{
                    ParameterDef.of("a", SystemDataType.DOUBLE),
                    ParameterDef.of("b", SystemDataType.DOUBLE)
                },
                SystemDataType.DOUBLE,
                new ReturnStatement(new Plus(
                    new VariableExpression(IdentifierExpression.of("a")),
                    new VariableExpression(IdentifierExpression.of("b"))
                ))
            );
            context.addFunction(func);

            // Call with ints: addDoubles(3, 5)
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new FunctionCallExpression(
                    IdentifierExpression.of("addDoubles"),
                    LiteralExpression.of(3),  // INT
                    LiteralExpression.of(5)   // INT
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(8.0, (Double) context.getVariable("result").get(), 0.001);
        }

        /**
         * Tests that with both INT and DOUBLE overloads, an INT argument selects the
         * exact-match INT overload rather than throwing an ambiguity error.
         *
         * <p>Prior to issue #43 this threw {@link FunctionAmbiguityException} because
         * INT is compatible with both {@code process(int)} (exact) and
         * {@code process(double)} (widening). The exact-match resolver now prefers the
         * exact overload, giving C {@code <tgmath.h>}-style dispatch.
         * <pre>
         * fn process(x: Int) -> Int { return x * 2 }
         * fn process(x: Double) -> Double { return x * 3.0 }
         * result = process(5)  // selects process(int) -> 10
         * </pre>
         */
        @Test
        void testOverloadResolution_IntPrefersExactOverWidening() {
            // INT version: process(int) -> int { return x * 2 }
            Function intFunc = new AbstractFunction(
                IdentifierExpression.of("process"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.INT)},
                SystemDataType.INT,
                new ReturnStatement(new Multi(
                    new VariableExpression(IdentifierExpression.of("x")),
                    LiteralExpression.of(2)
                ))
            );
            context.addFunction(intFunc);

            // DOUBLE version: process(double) -> double { return x * 3.0 }
            Function doubleFunc = new AbstractFunction(
                IdentifierExpression.of("process"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.DOUBLE)},
                SystemDataType.DOUBLE,
                new ReturnStatement(new Multi(
                    new VariableExpression(IdentifierExpression.of("x")),
                    LiteralExpression.of(3.0)
                ))
            );
            context.addFunction(doubleFunc);

            // Call with INT - exact match is process(int) -> 5 * 2 = 10
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.INT,
                new FunctionCallExpression(
                    IdentifierExpression.of("process"),
                    LiteralExpression.of(5)  // INT
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(10, context.getVariable("result").get());
        }

        /**
         * Tests that a DOUBLE argument selects the exact-match DOUBLE overload when
         * both INT and DOUBLE overloads exist.
         * <pre>
         * fn process(x: Int) -> Int { return x * 2 }
         * fn process(x: Double) -> Double { return x * 3.0 }
         * result = process(5.0)  // selects process(double) -> 15.0
         * </pre>
         */
        @Test
        void testOverloadResolution_DoublePrefersExact() {
            Function intFunc = new AbstractFunction(
                IdentifierExpression.of("process"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.INT)},
                SystemDataType.INT,
                new ReturnStatement(new Multi(
                    new VariableExpression(IdentifierExpression.of("x")),
                    LiteralExpression.of(2)
                ))
            );
            context.addFunction(intFunc);

            Function doubleFunc = new AbstractFunction(
                IdentifierExpression.of("process"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.DOUBLE)},
                SystemDataType.DOUBLE,
                new ReturnStatement(new Multi(
                    new VariableExpression(IdentifierExpression.of("x")),
                    LiteralExpression.of(3.0)
                ))
            );
            context.addFunction(doubleFunc);

            // Call with DOUBLE - exact match is process(double) -> 5.0 * 3.0 = 15.0
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new FunctionCallExpression(
                    IdentifierExpression.of("process"),
                    LiteralExpression.of(5.0)  // DOUBLE
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(15.0, (Double) context.getVariable("result").get(), 0.001);
        }

        /**
         * Tests that only double parameter matches when INT version is not available.
         * <pre>
         * fn onlyDouble(x: Double) -> Double { return x * 3.0 }
         * result = onlyDouble(5)  // int widened to double, returns 15.0
         * </pre>
         */
        @Test
        void testOnlyDoubleOverload_IntWidens() {
            // DOUBLE version only: onlyDouble(double) -> double { return x * 3.0 }
            Function doubleFunc = new AbstractFunction(
                IdentifierExpression.of("onlyDouble"),
                new ParameterDef[]{ParameterDef.of("x", SystemDataType.DOUBLE)},
                SystemDataType.DOUBLE,
                new ReturnStatement(new Multi(
                    new VariableExpression(IdentifierExpression.of("x")),
                    LiteralExpression.of(3.0)
                ))
            );
            context.addFunction(doubleFunc);

            // Call with INT - should widen to DOUBLE
            Block program = new BlockImpl();
            program.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.DOUBLE,
                new FunctionCallExpression(
                    IdentifierExpression.of("onlyDouble"),
                    LiteralExpression.of(5)  // INT
                )
            ));

            new EvalVisitor(context).visit(program);
            assertEquals(15.0, (Double) context.getVariable("result").get(), 0.001);  // 5 * 3.0 = 15.0
        }
    }
}
