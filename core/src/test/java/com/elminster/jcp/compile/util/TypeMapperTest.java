package com.elminster.jcp.compile.util;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.ThisExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.eval.data.StructType;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TypeMapper.
 */
class TypeMapperTest {

    private CompileContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new CompileContext();
        ctx.setClassName("TestClass");
    }

    @Nested
    class DescriptorTests {

        @Test
        void testIntDescriptor() {
            assertEquals("I", TypeMapper.toDescriptor(SystemDataType.INT));
        }

        @Test
        void testDoubleDescriptor() {
            assertEquals("D", TypeMapper.toDescriptor(SystemDataType.DOUBLE));
        }

        @Test
        void testBooleanDescriptor() {
            assertEquals("Z", TypeMapper.toDescriptor(SystemDataType.BOOLEAN));
        }

        @Test
        void testStringDescriptor() {
            assertEquals("Ljava/lang/String;", TypeMapper.toDescriptor(SystemDataType.STRING));
        }

        @Test
        void testVoidDescriptor() {
            assertEquals("V", TypeMapper.toDescriptor(SystemDataType.VOID));
        }

        @Test
        void testIntArrayDescriptor() {
            assertEquals("[I", TypeMapper.toDescriptor(SystemDataType.INT_ARRAY));
        }

        @Test
        void testDoubleArrayDescriptor() {
            assertEquals("[D", TypeMapper.toDescriptor(SystemDataType.DOUBLE_ARRAY));
        }

        @Test
        void testStringArrayDescriptor() {
            assertEquals("[Ljava/lang/String;", TypeMapper.toDescriptor(SystemDataType.STRING_ARRAY));
        }

        @Test
        void testBooleanArrayDescriptor() {
            assertEquals("[Z", TypeMapper.toDescriptor(SystemDataType.BOOLEAN_ARRAY));
        }

        @Test
        void testStructTypeDescriptor() {
            StructType structType = new StructType("Person", Collections.emptyList());
            assertEquals("LPerson;", TypeMapper.toDescriptor(structType));
        }

        @Test
        void testExternalClassTypeDescriptor() {
            ExternalClassType extType = new ExternalClassType("String", String.class);
            assertEquals("Ljava/lang/String;", TypeMapper.toDescriptor(extType));
        }

        @Test
        void testAnyTypeDescriptor() {
            assertEquals("Ljava/lang/Object;", TypeMapper.toDescriptor(SystemDataType.ANY));
        }
    }

    @Nested
    class MethodDescriptorTests {

        @Test
        void testBuildMethodDescriptor_NoParams_IntReturn() {
            String desc = TypeMapper.buildMethodDescriptor((ParameterDef[]) null, SystemDataType.INT);
            assertEquals("()I", desc);
        }

        @Test
        void testBuildMethodDescriptor_TwoIntParams_IntReturn() {
            ParameterDef[] params = {
                ParameterDef.of("a", SystemDataType.INT),
                ParameterDef.of("b", SystemDataType.INT)
            };
            String desc = TypeMapper.buildMethodDescriptor(params, SystemDataType.INT);
            assertEquals("(II)I", desc);
        }

        @Test
        void testBuildMethodDescriptor_MixedParams() {
            ParameterDef[] params = {
                ParameterDef.of("a", SystemDataType.INT),
                ParameterDef.of("b", SystemDataType.DOUBLE),
                ParameterDef.of("c", SystemDataType.STRING)
            };
            String desc = TypeMapper.buildMethodDescriptor(params, SystemDataType.VOID);
            assertEquals("(IDLjava/lang/String;)V", desc);
        }

        @Test
        void testBuildMethodDescriptor_WithDataTypeArray() {
            DataType[] argTypes = {SystemDataType.INT, SystemDataType.BOOLEAN};
            String desc = TypeMapper.buildMethodDescriptor(argTypes, SystemDataType.DOUBLE);
            assertEquals("(IZ)D", desc);
        }

        @Test
        void testBuildMethodDescriptor_NullReturnType() {
            DataType[] argTypes = {SystemDataType.INT};
            String desc = TypeMapper.buildMethodDescriptor(argTypes, null);
            assertEquals("(I)", desc);
        }
    }

    @Nested
    class OpcodeTests {

        @Test
        void testLoadOpcode_Int() {
            assertEquals(Opcodes.ILOAD, TypeMapper.getLoadOpcode(SystemDataType.INT));
        }

        @Test
        void testLoadOpcode_Boolean() {
            assertEquals(Opcodes.ILOAD, TypeMapper.getLoadOpcode(SystemDataType.BOOLEAN));
        }

        @Test
        void testLoadOpcode_Double() {
            assertEquals(Opcodes.DLOAD, TypeMapper.getLoadOpcode(SystemDataType.DOUBLE));
        }

        @Test
        void testLoadOpcode_String() {
            assertEquals(Opcodes.ALOAD, TypeMapper.getLoadOpcode(SystemDataType.STRING));
        }

        @Test
        void testLoadOpcode_Array() {
            assertEquals(Opcodes.ALOAD, TypeMapper.getLoadOpcode(SystemDataType.INT_ARRAY));
        }

        @Test
        void testStoreOpcode_Int() {
            assertEquals(Opcodes.ISTORE, TypeMapper.getStoreOpcode(SystemDataType.INT));
        }

        @Test
        void testStoreOpcode_Boolean() {
            assertEquals(Opcodes.ISTORE, TypeMapper.getStoreOpcode(SystemDataType.BOOLEAN));
        }

        @Test
        void testStoreOpcode_Double() {
            assertEquals(Opcodes.DSTORE, TypeMapper.getStoreOpcode(SystemDataType.DOUBLE));
        }

        @Test
        void testStoreOpcode_String() {
            assertEquals(Opcodes.ASTORE, TypeMapper.getStoreOpcode(SystemDataType.STRING));
        }

        @Test
        void testReturnOpcode_Void() {
            assertEquals(Opcodes.RETURN, TypeMapper.getReturnOpcode(SystemDataType.VOID));
        }

        @Test
        void testReturnOpcode_Int() {
            assertEquals(Opcodes.IRETURN, TypeMapper.getReturnOpcode(SystemDataType.INT));
        }

        @Test
        void testReturnOpcode_Boolean() {
            assertEquals(Opcodes.IRETURN, TypeMapper.getReturnOpcode(SystemDataType.BOOLEAN));
        }

        @Test
        void testReturnOpcode_Double() {
            assertEquals(Opcodes.DRETURN, TypeMapper.getReturnOpcode(SystemDataType.DOUBLE));
        }

        @Test
        void testReturnOpcode_String() {
            assertEquals(Opcodes.ARETURN, TypeMapper.getReturnOpcode(SystemDataType.STRING));
        }
    }

    @Nested
    class ArrayElementTypeTests {

        @Test
        void testGetArrayElementType_IntArray() {
            assertEquals(SystemDataType.INT, TypeMapper.getArrayElementType(SystemDataType.INT_ARRAY));
        }

        @Test
        void testGetArrayElementType_StringArray() {
            assertEquals(SystemDataType.STRING, TypeMapper.getArrayElementType(SystemDataType.STRING_ARRAY));
        }

        @Test
        void testGetArrayElementType_BooleanArray() {
            assertEquals(SystemDataType.BOOLEAN, TypeMapper.getArrayElementType(SystemDataType.BOOLEAN_ARRAY));
        }

        @Test
        void testGetArrayElementType_DoubleArray() {
            assertEquals(SystemDataType.DOUBLE, TypeMapper.getArrayElementType(SystemDataType.DOUBLE_ARRAY));
        }

        @Test
        void testGetArrayElementType_Unknown() {
            assertEquals(SystemDataType.ANY, TypeMapper.getArrayElementType(SystemDataType.ANY_ARRAY));
        }
    }

    @Nested
    class PrimitiveAndSlotTests {

        @Test
        void testIsPrimitive_Int() {
            assertTrue(TypeMapper.isPrimitive(SystemDataType.INT));
        }

        @Test
        void testIsPrimitive_Boolean() {
            assertTrue(TypeMapper.isPrimitive(SystemDataType.BOOLEAN));
        }

        @Test
        void testIsPrimitive_Double() {
            assertTrue(TypeMapper.isPrimitive(SystemDataType.DOUBLE));
        }

        @Test
        void testIsPrimitive_String() {
            assertFalse(TypeMapper.isPrimitive(SystemDataType.STRING));
        }

        @Test
        void testSlotSize_Int() {
            assertEquals(1, TypeMapper.getSlotSize(SystemDataType.INT));
        }

        @Test
        void testSlotSize_Double() {
            assertEquals(2, TypeMapper.getSlotSize(SystemDataType.DOUBLE));
        }

        @Test
        void testGetSize_Int() {
            assertEquals(1, TypeMapper.getSize(SystemDataType.INT));
        }

        @Test
        void testGetSize_Double() {
            assertEquals(2, TypeMapper.getSize(SystemDataType.DOUBLE));
        }
    }

    @Nested
    class ExpressionTypeTests {

        @Test
        void testGetExpressionType_IntLiteral() {
            DataType type = TypeMapper.getExpressionType(LiteralExpression.of(42), ctx);
            assertEquals(SystemDataType.INT, type);
        }

        @Test
        void testGetExpressionType_DoubleLiteral() {
            DataType type = TypeMapper.getExpressionType(LiteralExpression.of(3.14), ctx);
            assertEquals(SystemDataType.DOUBLE, type);
        }

        @Test
        void testGetExpressionType_BooleanLiteral() {
            DataType type = TypeMapper.getExpressionType(LiteralExpression.of(true), ctx);
            assertEquals(SystemDataType.BOOLEAN, type);
        }

        @Test
        void testGetExpressionType_StringLiteral() {
            DataType type = TypeMapper.getExpressionType(LiteralExpression.of("hello"), ctx);
            assertEquals(SystemDataType.STRING, type);
        }

        @Test
        void testGetExpressionType_Variable() {
            ctx.allocateLocal("x", SystemDataType.INT);
            VariableExpression varExpr = new VariableExpression(Identifier.fromName("x"));
            DataType type = TypeMapper.getExpressionType(varExpr, ctx);
            assertEquals(SystemDataType.INT, type);
        }

        @Test
        void testGetExpressionType_UnknownVariable() {
            VariableExpression varExpr = new VariableExpression(Identifier.fromName("unknown"));
            assertThrows(CompileException.class, () -> TypeMapper.getExpressionType(varExpr, ctx));
        }

        @Test
        void testGetExpressionType_ThisExpression() {
            ctx.allocateLocal("this", new StructType("Person", Collections.emptyList()));
            DataType type = TypeMapper.getExpressionType(new ThisExpression(), ctx);
            assertNotNull(type);
        }
    }

    @Nested
    class AsmTypeTests {

        @Test
        void testToAsmType_Int() {
            org.objectweb.asm.Type asmType = TypeMapper.toAsmType(SystemDataType.INT);
            assertEquals("I", asmType.getDescriptor());
        }

        @Test
        void testToAsmType_Double() {
            org.objectweb.asm.Type asmType = TypeMapper.toAsmType(SystemDataType.DOUBLE);
            assertEquals("D", asmType.getDescriptor());
        }
    }

    @Nested
    class LiteralWrapperExpressionTypeTests {

        /**
         * Tests generic Literal.of() created values.
         * <pre>
         * Literal.of(42)  // INT type
         * </pre>
         */
        @Test
        void testGetExpressionType_GenericLiteral_Int() {
            LiteralExpression expr = LiteralExpression.of(com.elminster.jcp.ast.expression.literal.Literal.of(42));
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.INT, type);
        }

        /**
         * Tests generic Literal.of() with double.
         * <pre>
         * Literal.of(3.14)  // DOUBLE type
         * </pre>
         */
        @Test
        void testGetExpressionType_GenericLiteral_Double() {
            LiteralExpression expr = LiteralExpression.of(com.elminster.jcp.ast.expression.literal.Literal.of(3.14));
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.DOUBLE, type);
        }

        /**
         * Tests generic Literal.of() with boolean.
         * <pre>
         * Literal.of(true)  // BOOLEAN type
         * </pre>
         */
        @Test
        void testGetExpressionType_GenericLiteral_Boolean() {
            LiteralExpression expr = LiteralExpression.of(com.elminster.jcp.ast.expression.literal.Literal.of(true));
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.BOOLEAN, type);
        }

        /**
         * Tests generic Literal.of() with string.
         * <pre>
         * Literal.of("hello")  // STRING type
         * </pre>
         */
        @Test
        void testGetExpressionType_GenericLiteral_String() {
            LiteralExpression expr = LiteralExpression.of(com.elminster.jcp.ast.expression.literal.Literal.of("hello"));
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.STRING, type);
        }

        /**
         * Tests that a generic Literal with an unsupported value type (e.g. Long) throws.
         * <pre>
         * Literal.of(42L)  // Long not handled → CompileException
         * </pre>
         */
        @Test
        void testGetExpressionType_GenericLiteral_UnknownType_ReturnsNull() {
            LiteralExpression expr = LiteralExpression.of(
                com.elminster.jcp.ast.expression.literal.Literal.of(42L));
            assertThrows(CompileException.class, () -> TypeMapper.getExpressionType(expr, ctx));
        }
    }

    @Nested
    class IdentifierExpressionTypeTests {

        /**
         * Tests Identifier type lookup.
         * <pre>
         * var x: Int
         * x  // lookup type = INT
         * </pre>
         */
        @Test
        void testGetExpressionType_Identifier() {
            ctx.allocateLocal("x", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.IdentifierExpression idExpr =
                new com.elminster.jcp.ast.expression.operation.IdentifierExpression("x");
            DataType type = TypeMapper.getExpressionType(idExpr, ctx);
            assertEquals(SystemDataType.INT, type);
        }

        /**
         * Tests unknown Identifier throws CompileException.
         * <pre>
         * unknownVar  // undefined → CompileException
         * </pre>
         */
        @Test
        void testGetExpressionType_UnknownIdentifier() {
            com.elminster.jcp.ast.expression.operation.IdentifierExpression idExpr =
                new com.elminster.jcp.ast.expression.operation.IdentifierExpression("unknownVar");
            assertThrows(CompileException.class, () -> TypeMapper.getExpressionType(idExpr, ctx));
        }
    }

    @Nested
    class BinaryExpressionTypeTests {

        /**
         * Tests expression type for INT + INT binary expression.
         * <pre>
         * var x: Int = 1 + 2  // type = INT
         * </pre>
         */
        @Test
        void testGetExpressionType_IntPlusInt() {
            ctx.allocateLocal("a", SystemDataType.INT);
            ctx.allocateLocal("b", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.Plus expr =
                new com.elminster.jcp.ast.expression.operation.Plus(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.INT, type);
        }

        /**
         * Tests expression type for DOUBLE + INT binary expression (numeric promotion).
         * <pre>
         * var x: Double = 1.0 + 2  // type = DOUBLE
         * </pre>
         */
        @Test
        void testGetExpressionType_DoublePlusInt() {
            ctx.allocateLocal("a", SystemDataType.DOUBLE);
            ctx.allocateLocal("b", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.Plus expr =
                new com.elminster.jcp.ast.expression.operation.Plus(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.DOUBLE, type);
        }

        /**
         * Tests expression type for INT + DOUBLE binary expression (numeric promotion).
         * <pre>
         * var x = 2 + 1.0  // type = DOUBLE
         * </pre>
         */
        @Test
        void testGetExpressionType_IntPlusDouble() {
            ctx.allocateLocal("a", SystemDataType.INT);
            ctx.allocateLocal("b", SystemDataType.DOUBLE);
            com.elminster.jcp.ast.expression.operation.Plus expr =
                new com.elminster.jcp.ast.expression.operation.Plus(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.DOUBLE, type);
        }

        /**
         * Tests expression type for DOUBLE + DOUBLE binary expression.
         * <pre>
         * var x = 1.5 + 2.3  // type = DOUBLE
         * </pre>
         */
        @Test
        void testGetExpressionType_DoublePlusDouble() {
            ctx.allocateLocal("a", SystemDataType.DOUBLE);
            ctx.allocateLocal("b", SystemDataType.DOUBLE);
            com.elminster.jcp.ast.expression.operation.Plus expr =
                new com.elminster.jcp.ast.expression.operation.Plus(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.DOUBLE, type);
        }

        /**
         * Tests that a binary expression with non-numeric operands throws CompileException.
         * <pre>
         * var a: Boolean = true
         * var b: Boolean = false
         * a + b  // invalid arithmetic → CompileException
         * </pre>
         */
        @Test
        void testGetExpressionType_BinaryExpr_BooleanOperands_ReturnsNull() {
            ctx.allocateLocal("a", SystemDataType.BOOLEAN);
            ctx.allocateLocal("b", SystemDataType.BOOLEAN);
            com.elminster.jcp.ast.expression.operation.Plus expr =
                new com.elminster.jcp.ast.expression.operation.Plus(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            assertThrows(CompileException.class, () -> TypeMapper.getExpressionType(expr, ctx));
        }

        /**
         * Tests expression type for INT - INT subtraction.
         * <pre>
         * var x = 5 - 3  // type = INT
         * </pre>
         */
        @Test
        void testGetExpressionType_IntMinusInt() {
            ctx.allocateLocal("a", SystemDataType.INT);
            ctx.allocateLocal("b", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.Minus expr =
                new com.elminster.jcp.ast.expression.operation.Minus(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.INT, type);
        }

        /**
         * Tests expression type for INT * INT multiplication.
         * <pre>
         * var x = 5 * 3  // type = INT
         * </pre>
         */
        @Test
        void testGetExpressionType_IntTimesInt() {
            ctx.allocateLocal("a", SystemDataType.INT);
            ctx.allocateLocal("b", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.Multi expr =
                new com.elminster.jcp.ast.expression.operation.Multi(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.INT, type);
        }

        /**
         * Tests expression type for INT / INT division.
         * <pre>
         * var x = 10 / 2  // type = INT
         * </pre>
         */
        @Test
        void testGetExpressionType_IntDivideInt() {
            ctx.allocateLocal("a", SystemDataType.INT);
            ctx.allocateLocal("b", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.Divide expr =
                new com.elminster.jcp.ast.expression.operation.Divide(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.INT, type);
        }

        /**
         * Tests expression type for unknown variable + known variable throws CompileException.
         * <pre>
         * var x = unknown + 5  // undefined variable → CompileException
         * </pre>
         */
        @Test
        void testGetExpressionType_UnknownOperand() {
            ctx.allocateLocal("b", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.Plus expr =
                new com.elminster.jcp.ast.expression.operation.Plus(
                    new VariableExpression(Identifier.fromName("unknown")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            assertThrows(CompileException.class, () -> TypeMapper.getExpressionType(expr, ctx));
        }

        /**
         * Tests expression type for INT == INT comparison (always BOOLEAN).
         * <pre>
         * var result = (x == y)  // type = BOOLEAN
         * </pre>
         */
        @Test
        void testGetExpressionType_EqualComparison() {
            ctx.allocateLocal("a", SystemDataType.INT);
            ctx.allocateLocal("b", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.Equal expr =
                new com.elminster.jcp.ast.expression.operation.Equal(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.BOOLEAN, type);
        }

        /**
         * Tests expression type for INT != INT comparison (always BOOLEAN).
         * <pre>
         * var result = (x != y)  // type = BOOLEAN
         * </pre>
         */
        @Test
        void testGetExpressionType_NotEqualComparison() {
            ctx.allocateLocal("a", SystemDataType.INT);
            ctx.allocateLocal("b", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.NotEqual expr =
                new com.elminster.jcp.ast.expression.operation.NotEqual(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.BOOLEAN, type);
        }

        /**
         * Tests expression type for INT < INT comparison (always BOOLEAN).
         * <pre>
         * var result = (x < y)  // type = BOOLEAN
         * </pre>
         */
        @Test
        void testGetExpressionType_LessThanComparison() {
            ctx.allocateLocal("a", SystemDataType.INT);
            ctx.allocateLocal("b", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.LessThan expr =
                new com.elminster.jcp.ast.expression.operation.LessThan(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.BOOLEAN, type);
        }

        /**
         * Tests expression type for INT > INT comparison (always BOOLEAN).
         * <pre>
         * var result = (x > y)  // type = BOOLEAN
         * </pre>
         */
        @Test
        void testGetExpressionType_GreaterThanComparison() {
            ctx.allocateLocal("a", SystemDataType.INT);
            ctx.allocateLocal("b", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.GreaterThan expr =
                new com.elminster.jcp.ast.expression.operation.GreaterThan(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.BOOLEAN, type);
        }

        /**
         * Tests expression type for INT <= INT comparison (always BOOLEAN).
         * <pre>
         * var result = (x <= y)  // type = BOOLEAN
         * </pre>
         */
        @Test
        void testGetExpressionType_LessThanOrEqualComparison() {
            ctx.allocateLocal("a", SystemDataType.INT);
            ctx.allocateLocal("b", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.LessThanEqual expr =
                new com.elminster.jcp.ast.expression.operation.LessThanEqual(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.BOOLEAN, type);
        }

        /**
         * Tests expression type for INT >= INT comparison (always BOOLEAN).
         * <pre>
         * var result = (x >= y)  // type = BOOLEAN
         * </pre>
         */
        @Test
        void testGetExpressionType_GreaterThanOrEqualComparison() {
            ctx.allocateLocal("a", SystemDataType.INT);
            ctx.allocateLocal("b", SystemDataType.INT);
            com.elminster.jcp.ast.expression.operation.GreaterThanEqual expr =
                new com.elminster.jcp.ast.expression.operation.GreaterThanEqual(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.BOOLEAN, type);
        }
    }

    @Nested
    class LogicalExpressionTypeTests {

        /**
         * Tests expression type for BOOLEAN && BOOLEAN logical AND (always BOOLEAN).
         * <pre>
         * var result = (a && b)  // type = BOOLEAN
         * </pre>
         */
        @Test
        void testGetExpressionType_LogicalAnd() {
            ctx.allocateLocal("a", SystemDataType.BOOLEAN);
            ctx.allocateLocal("b", SystemDataType.BOOLEAN);
            com.elminster.jcp.ast.expression.operation.LogicalAndExpression expr =
                new com.elminster.jcp.ast.expression.operation.LogicalAndExpression(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.BOOLEAN, type);
        }

        /**
         * Tests expression type for BOOLEAN || BOOLEAN logical OR (always BOOLEAN).
         * <pre>
         * var result = (a || b)  // type = BOOLEAN
         * </pre>
         */
        @Test
        void testGetExpressionType_LogicalOr() {
            ctx.allocateLocal("a", SystemDataType.BOOLEAN);
            ctx.allocateLocal("b", SystemDataType.BOOLEAN);
            com.elminster.jcp.ast.expression.operation.LogicalOrExpression expr =
                new com.elminster.jcp.ast.expression.operation.LogicalOrExpression(
                    new VariableExpression(Identifier.fromName("a")),
                    new VariableExpression(Identifier.fromName("b"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.BOOLEAN, type);
        }
    }

    @Nested
    class UnaryExpressionTypeTests {

        /**
         * Tests expression type for logical NOT operator (always BOOLEAN).
         * <pre>
         * var result = !x  // type = BOOLEAN
         * </pre>
         */
        @Test
        void testGetExpressionType_LogicalNotOperator() {
            ctx.allocateLocal("a", SystemDataType.BOOLEAN);
            com.elminster.jcp.ast.expression.operation.LogicalNotExpression expr =
                new com.elminster.jcp.ast.expression.operation.LogicalNotExpression(
                    new VariableExpression(Identifier.fromName("a"))
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.BOOLEAN, type);
        }
    }

    @Nested
    class FunctionCallTypeTests {

        /**
         * Tests expression type for function call expression.
         * <pre>
         * fn getValue() -> Int { return 42 }
         * var x = getValue()  // type = INT
         * </pre>
         */
        @Test
        void testGetExpressionType_FunctionCall() {
            // Register function
            ctx.registerFunction("getValue", new ParameterDef[0], SystemDataType.INT);

            com.elminster.jcp.ast.expression.base.FunctionCallExpression expr =
                new com.elminster.jcp.ast.expression.base.FunctionCallExpression(
                    Identifier.fromName("getValue")
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.INT, type);
        }

        /**
         * Tests expression type for function call with arguments.
         * <pre>
         * fn add(a: Int, b: Int) -> Int { return a + b }
         * var x = add(1, 2)  // type = INT
         * </pre>
         */
        @Test
        void testGetExpressionType_FunctionCallWithArgs() {
            // Register function
            ParameterDef[] params = {
                ParameterDef.of("a", SystemDataType.INT),
                ParameterDef.of("b", SystemDataType.INT)
            };
            ctx.registerFunction("add", params, SystemDataType.INT);

            com.elminster.jcp.ast.expression.base.FunctionCallExpression expr =
                new com.elminster.jcp.ast.expression.base.FunctionCallExpression(
                    Identifier.fromName("add"),
                    LiteralExpression.of(1),
                    LiteralExpression.of(2)
                );
            DataType type = TypeMapper.getExpressionType(expr, ctx);
            assertEquals(SystemDataType.INT, type);
        }

        /**
         * Tests expression type for undefined function call throws CompileException.
         * <pre>
         * var x = unknownFunc()  // undefined → CompileException
         * </pre>
         */
        @Test
        void testGetExpressionType_UndefinedFunctionCall() {
            com.elminster.jcp.ast.expression.base.FunctionCallExpression expr =
                new com.elminster.jcp.ast.expression.base.FunctionCallExpression(
                    Identifier.fromName("unknownFunc")
                );
            assertThrows(CompileException.class, () -> TypeMapper.getExpressionType(expr, ctx));
        }
    }


    @Nested
    class LoadStoreOpcodeTests {

        /**
         * Tests load opcode for struct type.
         * <pre>
         * Point p  // ALOAD
         * </pre>
         */
        @Test
        void testLoadOpcode_StructType() {
            StructType structType = new StructType("Point", Collections.emptyList());
            assertEquals(Opcodes.ALOAD, TypeMapper.getLoadOpcode(structType));
        }

        /**
         * Tests store opcode for struct type.
         * <pre>
         * Point p  // ASTORE
         * </pre>
         */
        @Test
        void testStoreOpcode_StructType() {
            StructType structType = new StructType("Point", Collections.emptyList());
            assertEquals(Opcodes.ASTORE, TypeMapper.getStoreOpcode(structType));
        }

        /**
         * Tests return opcode for struct type.
         * <pre>
         * return point  // ARETURN
         * </pre>
         */
        @Test
        void testReturnOpcode_StructType() {
            StructType structType = new StructType("Point", Collections.emptyList());
            assertEquals(Opcodes.ARETURN, TypeMapper.getReturnOpcode(structType));
        }
    }
}
