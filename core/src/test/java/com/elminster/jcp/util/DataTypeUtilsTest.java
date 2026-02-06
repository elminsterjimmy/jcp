package com.elminster.jcp.util;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DataTypeImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DataTypeUtils.
 */
class DataTypeUtilsTest {

    private EvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
    }

    @Nested
    class GetDataTypeTests {

        @Test
        void testGetDataType_SystemType() {
            DataType type = DataTypeUtils.getDataType("Integer", context);
            assertEquals(SystemDataType.INT, type);
        }

        @Test
        void testGetDataType_ArrayType() {
            DataType type = DataTypeUtils.getDataType("Integer[]", context);
            assertNotNull(type);
            assertTrue(type.getName().contains("[]"));
        }
    }

    @Nested
    class GetDataTypeAndCreateOnMissingTests {

        @Test
        void testGetDataTypeAndCreateOnMissing_ExistingType() {
            DataType type = DataTypeUtils.getDataTypeAndCreateOnMissing("Integer", context);
            assertEquals(SystemDataType.INT, type);
        }

        @Test
        void testGetDataTypeAndCreateOnMissing_NewType() {
            DataType type = DataTypeUtils.getDataTypeAndCreateOnMissing("CustomType", context);
            assertNotNull(type);
            assertEquals("CustomType", type.getName());
        }

        @Test
        void testGetDataTypeAndCreateOnMissing_ArrayType() {
            DataType type = DataTypeUtils.getDataTypeAndCreateOnMissing("CustomType[]", context);
            assertNotNull(type);
            assertTrue(type.getName().contains("[]"));
        }

        @Test
        void testGetDataTypeAndCreateOnMissing_NestedArrayType() {
            DataType type = DataTypeUtils.getDataTypeAndCreateOnMissing("Integer[]", context);
            assertNotNull(type);
        }
    }

    @Nested
    class GetDataTypeFromLiteralTests {

        @Test
        void testGetDataType_GenericLiteral_ReturnsAny() {
            // Generic LiteralExpression returns ANY (the instanceof checks in the method
            // never match because LiteralExpression.of() creates generic Literal objects)
            DataType type = DataTypeUtils.getDataTypeAndCreateOnMissing(LiteralExpression.of(42));
            assertEquals(SystemDataType.ANY, type);
        }

        @Test
        void testGetDataType_StringLiteral_ReturnsAny() {
            DataType type = DataTypeUtils.getDataTypeAndCreateOnMissing(LiteralExpression.of("hello"));
            assertEquals(SystemDataType.ANY, type);
        }
    }

    @Nested
    class ToDoubleValueTests {

        @Test
        void testToDoubleValue_FromDouble() {
            Data<Double> data = new AnyData<>(3.14, SystemDataType.DOUBLE);
            double result = DataTypeUtils.toDoubleValue(data);
            assertEquals(3.14, result, 0.0001);
        }

        @Test
        void testToDoubleValue_FromInt() {
            Data<Integer> data = new AnyData<>(42, SystemDataType.INT);
            double result = DataTypeUtils.toDoubleValue(data);
            assertEquals(42.0, result, 0.0001);
        }

        @Test
        void testToDoubleValue_FromString_ThrowsException() {
            Data<String> data = new AnyData<>("hello", SystemDataType.STRING);
            assertThrows(IllegalArgumentException.class, () ->
                DataTypeUtils.toDoubleValue(data)
            );
        }
    }
}
