package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.literal.DoubleLiteral;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.expression.operation.Equal;
import com.elminster.jcp.ast.expression.operation.GreaterThan;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.LessThan;
import com.elminster.jcp.ast.expression.operation.MinusMinus;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.expression.operation.PlusPlus;
import com.elminster.jcp.compile.base.IdentifierCompiler;
import com.elminster.jcp.compile.base.LiteralCompiler;
import com.elminster.jcp.compile.base.VariableCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.compile.operator.arithmetic.ArithmeticCompiler;
import com.elminster.jcp.compile.operator.arithmetic.PlusCompiler;
import com.elminster.jcp.compile.operator.postfix.MinusMinusCompiler;
import com.elminster.jcp.compile.operator.postfix.PlusPlusCompiler;
import com.elminster.jcp.compile.operator.relational.CompareCompiler;
import com.elminster.jcp.compile.operator.relational.EqualCompiler;
import com.elminster.jcp.compile.operator.relational.GreaterThanCompiler;
import com.elminster.jcp.compile.operator.relational.LessThanCompiler;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for resolveType() on compiler classes.
 *
 * Tests type resolution without full bytecode compilation, verifying that each
 * compiler correctly reports the DataType of the value it would leave on the JVM stack.
 */
class ResolveTypeTest {

    private CompileContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new CompileContext();
        ctx.setClassName("TestClass");
    }

    // -------------------------------------------------------------------------
    // LiteralCompiler
    // -------------------------------------------------------------------------

    @Nested
    class LiteralCompilerResolveType {

        @Test
        void intLiteralReturnsInt() {
            LiteralCompiler compiler = new LiteralCompiler(LiteralExpression.of(42));
            assertEquals(SystemDataType.INT, compiler.resolveType(ctx));
        }

        @Test
        void doubleLiteralReturnsDouble() {
            LiteralCompiler compiler = new LiteralCompiler(LiteralExpression.of(3.14));
            assertEquals(SystemDataType.DOUBLE, compiler.resolveType(ctx));
        }

        @Test
        void booleanLiteralReturnsBoolean() {
            LiteralCompiler compiler = new LiteralCompiler(LiteralExpression.of(true));
            assertEquals(SystemDataType.BOOLEAN, compiler.resolveType(ctx));
        }

        @Test
        void stringLiteralReturnsString() {
            LiteralCompiler compiler = new LiteralCompiler(LiteralExpression.of("hello"));
            assertEquals(SystemDataType.STRING, compiler.resolveType(ctx));
        }

        @Test
        void intLiteralViaTypedClassReturnsInt() {
            LiteralCompiler compiler = new LiteralCompiler(
                new com.elminster.jcp.ast.expression.LiteralExpression(IntLiteral.of(10)));
            assertEquals(SystemDataType.INT, compiler.resolveType(ctx));
        }

        @Test
        void doubleLiteralViaTypedClassReturnsDouble() {
            LiteralCompiler compiler = new LiteralCompiler(
                new com.elminster.jcp.ast.expression.LiteralExpression(DoubleLiteral.of(2.5)));
            assertEquals(SystemDataType.DOUBLE, compiler.resolveType(ctx));
        }

        @Test
        void booleanLiteralViaTypedClassReturnsBoolean() {
            LiteralCompiler compiler = new LiteralCompiler(
                new com.elminster.jcp.ast.expression.LiteralExpression(BooleanLiteral.of(false)));
            assertEquals(SystemDataType.BOOLEAN, compiler.resolveType(ctx));
        }

        @Test
        void stringLiteralViaTypedClassReturnsString() {
            LiteralCompiler compiler = new LiteralCompiler(
                new com.elminster.jcp.ast.expression.LiteralExpression(StringLiteral.of("world")));
            assertEquals(SystemDataType.STRING, compiler.resolveType(ctx));
        }

        @Test
        void nullValueLiteralReturnsAny() {
            LiteralCompiler compiler = new LiteralCompiler(
                new com.elminster.jcp.ast.expression.LiteralExpression(
                    com.elminster.jcp.ast.expression.literal.Literal.of((Object) null)));
            assertEquals(SystemDataType.ANY, compiler.resolveType(ctx));
        }

        @Test
        void unknownValueTypeLiteralReturnsNull() {
            LiteralCompiler compiler = new LiteralCompiler(
                new com.elminster.jcp.ast.expression.LiteralExpression(
                    com.elminster.jcp.ast.expression.literal.Literal.of(42L)));
            assertThrows(CompileException.class, () -> compiler.resolveType(ctx));
        }
    }

    // -------------------------------------------------------------------------
    // IdentifierCompiler
    // -------------------------------------------------------------------------

    @Nested
    class IdentifierCompilerResolveType {

        @Test
        void knownIntVariableReturnsInt() {
            ctx.allocateLocal("x", SystemDataType.INT);
            IdentifierCompiler compiler = new IdentifierCompiler(
                new IdentifierExpression("x"));
            assertEquals(SystemDataType.INT, compiler.resolveType(ctx));
        }

        @Test
        void knownDoubleVariableReturnsDouble() {
            ctx.allocateLocal("d", SystemDataType.DOUBLE);
            IdentifierCompiler compiler = new IdentifierCompiler(
                new IdentifierExpression("d"));
            assertEquals(SystemDataType.DOUBLE, compiler.resolveType(ctx));
        }

        @Test
        void unknownVariableReturnsNull() {
            IdentifierCompiler compiler = new IdentifierCompiler(
                new IdentifierExpression("unknown"));
            assertThrows(CompileException.class, () -> compiler.resolveType(ctx));
        }

        @Test
        void rawIdentifierNodeKnownReturnsType() {
            ctx.allocateLocal("y", SystemDataType.BOOLEAN);
            IdentifierCompiler compiler = new IdentifierCompiler(new IdentifierExpression("y"));
            assertEquals(SystemDataType.BOOLEAN, compiler.resolveType(ctx));
        }

        @Test
        void rawIdentifierNodeUnknownReturnsNull() {
            IdentifierCompiler compiler = new IdentifierCompiler(new IdentifierExpression("notDeclared"));
            assertThrows(CompileException.class, () -> compiler.resolveType(ctx));
        }
    }

    // -------------------------------------------------------------------------
    // VariableCompiler
    // -------------------------------------------------------------------------

    @Nested
    class VariableCompilerResolveType {

        @Test
        void knownIntVariableReturnsInt() {
            ctx.allocateLocal("x", SystemDataType.INT);
            VariableCompiler compiler = new VariableCompiler(VariableExpression.of("x"));
            assertEquals(SystemDataType.INT, compiler.resolveType(ctx));
        }

        @Test
        void knownStringVariableReturnsString() {
            ctx.allocateLocal("s", SystemDataType.STRING);
            VariableCompiler compiler = new VariableCompiler(VariableExpression.of("s"));
            assertEquals(SystemDataType.STRING, compiler.resolveType(ctx));
        }

        @Test
        void unknownVariableReturnsNull() {
            VariableCompiler compiler = new VariableCompiler(VariableExpression.of("unknown"));
            assertThrows(CompileException.class, () -> compiler.resolveType(ctx));
        }
    }

    // -------------------------------------------------------------------------
    // ArithmeticCompiler (PlusCompiler as representative)
    // -------------------------------------------------------------------------

    @Nested
    class ArithmeticCompilerResolveType {

        @Test
        void intPlusIntReturnsInt() {
            PlusCompiler compiler = new PlusCompiler(
                new Plus(LiteralExpression.of(1), LiteralExpression.of(2)));
            assertEquals(SystemDataType.INT, compiler.resolveType(ctx));
        }

        @Test
        void intPlusDoubleReturnsDouble() {
            PlusCompiler compiler = new PlusCompiler(
                new Plus(LiteralExpression.of(1), LiteralExpression.of(2.0)));
            assertEquals(SystemDataType.DOUBLE, compiler.resolveType(ctx));
        }

        @Test
        void doublePlusIntReturnsDouble() {
            PlusCompiler compiler = new PlusCompiler(
                new Plus(LiteralExpression.of(1.5), LiteralExpression.of(2)));
            assertEquals(SystemDataType.DOUBLE, compiler.resolveType(ctx));
        }

        @Test
        void unknownLeftOperandReturnsNull() {
            // IdentifierExpression for undeclared variable → CompileException
            PlusCompiler compiler = new PlusCompiler(
                new Plus(new IdentifierExpression("undeclared"), LiteralExpression.of(1)));
            assertThrows(CompileException.class, () -> compiler.resolveType(ctx));
        }

        @Test
        void unknownRightOperandReturnsNull() {
            PlusCompiler compiler = new PlusCompiler(
                new Plus(LiteralExpression.of(1), new IdentifierExpression("undeclared")));
            assertThrows(CompileException.class, () -> compiler.resolveType(ctx));
        }
    }

    // -------------------------------------------------------------------------
    // Relational compilers — CompareCompiler base handles via inheritance
    // -------------------------------------------------------------------------

    @Nested
    class RelationalCompilerResolveType {

        @Test
        void equalCompilerReturnsBooleanViaInheritance() {
            EqualCompiler compiler = new EqualCompiler(
                new Equal(LiteralExpression.of(1), LiteralExpression.of(1)));
            assertEquals(SystemDataType.BOOLEAN, compiler.resolveType(ctx));
        }

        @Test
        void lessThanCompilerReturnsBoolean() {
            LessThanCompiler compiler = new LessThanCompiler(
                new LessThan(LiteralExpression.of(1), LiteralExpression.of(2)));
            assertEquals(SystemDataType.BOOLEAN, compiler.resolveType(ctx));
        }

        @Test
        void greaterThanCompilerReturnsBoolean() {
            GreaterThanCompiler compiler = new GreaterThanCompiler(
                new GreaterThan(LiteralExpression.of(5), LiteralExpression.of(3)));
            assertEquals(SystemDataType.BOOLEAN, compiler.resolveType(ctx));
        }
    }

    // -------------------------------------------------------------------------
    // Postfix compilers
    // -------------------------------------------------------------------------

    @Nested
    class PostfixCompilerResolveType {

        @Test
        void plusPlusReturnsInt() {
            ctx.allocateLocal("i", SystemDataType.INT);
            PlusPlusCompiler compiler = new PlusPlusCompiler(
                new PlusPlus(new IdentifierExpression("i")));
            assertEquals(SystemDataType.INT, compiler.resolveType(ctx));
        }

        @Test
        void minusMinusReturnsInt() {
            ctx.allocateLocal("i", SystemDataType.INT);
            MinusMinusCompiler compiler = new MinusMinusCompiler(
                new MinusMinus(new IdentifierExpression("i")));
            assertEquals(SystemDataType.INT, compiler.resolveType(ctx));
        }
    }

    // -------------------------------------------------------------------------
    // ThisCompiler
    // -------------------------------------------------------------------------

    @Nested
    class ThisCompilerResolveType {

        @Test
        void thisWithKnownStructTypeReturnsStructType() {
            com.elminster.jcp.eval.data.StructType pointType =
                new com.elminster.jcp.eval.data.StructType("Point", java.util.Collections.emptyList());
            ctx.allocateLocal("this", pointType);
            com.elminster.jcp.compile.struct.ThisCompiler compiler =
                new com.elminster.jcp.compile.struct.ThisCompiler(
                    new com.elminster.jcp.ast.expression.ThisExpression());
            assertEquals(pointType, compiler.resolveType(ctx));
        }

        @Test
        void thisWithoutLocalReturnsNull() {
            com.elminster.jcp.compile.struct.ThisCompiler compiler =
                new com.elminster.jcp.compile.struct.ThisCompiler(
                    new com.elminster.jcp.ast.expression.ThisExpression());
            assertThrows(CompileException.class, () -> compiler.resolveType(ctx));
        }
    }

    // -------------------------------------------------------------------------
    // Statement compilers return VOID
    // -------------------------------------------------------------------------

    @Nested
    class StatementCompilerResolveType {

        @Test
        void blockCompilerReturnsVoid() {
            com.elminster.jcp.compile.base.BlockCompiler compiler =
                new com.elminster.jcp.compile.base.BlockCompiler(new com.elminster.jcp.ast.statement.BlockImpl());
            assertEquals(SystemDataType.VOID, compiler.resolveType(ctx));
        }

        @Test
        void returnCompilerReturnsVoid() {
            com.elminster.jcp.compile.control.ReturnCompiler compiler =
                new com.elminster.jcp.compile.control.ReturnCompiler(
                    new com.elminster.jcp.ast.statement.control.ReturnStatement(LiteralExpression.of(0)));
            assertEquals(SystemDataType.VOID, compiler.resolveType(ctx));
        }
    }
}
