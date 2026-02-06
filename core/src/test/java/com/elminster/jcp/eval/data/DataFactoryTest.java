package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DataFactory.
 */
class DataFactoryTest {

    private DataFactory factory;
    private EvalContext context;

    @BeforeEach
    void setUp() {
        factory = DataFactory.INSTANCE;
        context = new RootEvalContext();
    }

    @Nested
    class CreateVariableTests {

        @Test
        void testCreateIntVariable() {
            Data data = factory.createVariable(Identifier.fromName("x"), DataType.SystemDataType.INT, 42);

            assertInstanceOf(IntegerData.class, data);
            assertEquals(42, data.get());
            assertEquals("x", data.getIdentifier().getId());
        }

        @Test
        void testCreateBooleanVariable() {
            Data data = factory.createVariable(Identifier.fromName("flag"), DataType.SystemDataType.BOOLEAN, true);

            assertInstanceOf(BooleanData.class, data);
            assertEquals(true, data.get());
        }

        @Test
        void testCreateStringVariable() {
            Data data = factory.createVariable(Identifier.fromName("name"), DataType.SystemDataType.STRING, "hello");

            assertInstanceOf(StringData.class, data);
            assertEquals("hello", data.get());
        }

        @Test
        void testCreateDoubleVariable() {
            Data data = factory.createVariable(Identifier.fromName("pi"), DataType.SystemDataType.DOUBLE, 3.14);

            assertInstanceOf(DoubleData.class, data);
            assertEquals(3.14, data.get());
        }

        @Test
        void testCreateAnyVariable() {
            Data data = factory.createVariable(Identifier.fromName("obj"), DataType.SystemDataType.ANY, "anything");

            assertInstanceOf(AnyData.class, data);
            assertEquals("anything", data.get());
        }

        @Test
        void testCreateVariableWithNullValue() {
            Data data = factory.createVariable(Identifier.fromName("x"), DataType.SystemDataType.INT);

            assertInstanceOf(IntegerData.class, data);
            assertNull(data.get());
        }
    }

    @Nested
    class CreateArrayTests {

        @Test
        void testCreateIntArray() {
            Data data = factory.createVariable(Identifier.fromName("arr"), DataType.SystemDataType.INT_ARRAY, null);

            assertInstanceOf(ArrayData.class, data);
        }

        @Test
        void testCreateBooleanArray() {
            Data data = factory.createVariable(Identifier.fromName("flags"), DataType.SystemDataType.BOOLEAN_ARRAY, null);

            assertInstanceOf(ArrayData.class, data);
        }

        @Test
        void testCreateStringArray() {
            Data data = factory.createVariable(Identifier.fromName("names"), DataType.SystemDataType.STRING_ARRAY, null);

            assertInstanceOf(ArrayData.class, data);
        }

        @Test
        void testCreateDoubleArray() {
            Data data = factory.createVariable(Identifier.fromName("values"), DataType.SystemDataType.DOUBLE_ARRAY, null);

            assertInstanceOf(ArrayData.class, data);
        }

        @Test
        void testCreateAnyArray() {
            Data data = factory.createVariable(Identifier.fromName("objects"), DataType.SystemDataType.ANY_ARRAY, null);

            assertInstanceOf(ArrayData.class, data);
        }
    }

    @Nested
    class CreateConstValueTests {

        @Test
        void testCreateConstInt() {
            Data data = factory.createConstValue(42, context);

            assertInstanceOf(IntegerData.class, data);
            assertEquals(42, data.get());
        }

        @Test
        void testCreateConstDouble() {
            Data data = factory.createConstValue(3.14, context);

            assertInstanceOf(DoubleData.class, data);
            assertEquals(3.14, data.get());
        }

        @Test
        void testCreateConstBoolean() {
            Data data = factory.createConstValue(true, context);

            assertInstanceOf(BooleanData.class, data);
            assertEquals(true, data.get());
        }

        @Test
        void testCreateConstNull() {
            Data data = factory.createConstValue(null, context);

            assertInstanceOf(AnyData.class, data);
            assertNull(data.get());
        }

        @Test
        void testCreateConstString() {
            // Note: String.class is NOT a primitive wrapper by ClassUtils.isPrimitiveOrWrapper
            // So it goes to the non-primitive else branch
            Data data = factory.createConstValue("hello", context);

            assertInstanceOf(AnyData.class, data);
            assertEquals("hello", data.get());
            assertTrue(data.isConst());
        }

        @Test
        void testCreateConstLong() {
            // Long is a primitive wrapper but not handled specially - goes to AnyData
            Data data = factory.createConstValue(100L, context);

            assertInstanceOf(AnyData.class, data);
            assertEquals(100L, data.get());
            assertTrue(data.isConst());
        }

        @Test
        void testCreateConstShort() {
            // Short is a primitive wrapper but not handled specially
            Data data = factory.createConstValue((short) 10, context);

            assertInstanceOf(AnyData.class, data);
            assertTrue(data.isConst());
        }

        @Test
        void testCreateConstByte() {
            // Byte is a primitive wrapper but not handled specially
            Data data = factory.createConstValue((byte) 5, context);

            assertInstanceOf(AnyData.class, data);
            assertTrue(data.isConst());
        }

        @Test
        void testCreateConstFloat() {
            // Float is a primitive wrapper but not handled specially
            Data data = factory.createConstValue(3.14f, context);

            assertInstanceOf(AnyData.class, data);
            assertTrue(data.isConst());
        }

        @Test
        void testCreateConstChar() {
            // Character is a primitive wrapper but not handled specially
            Data data = factory.createConstValue('A', context);

            assertInstanceOf(AnyData.class, data);
            assertTrue(data.isConst());
        }

        @Test
        void testCreateConstNonPrimitiveObject() {
            // Non-primitive object gets wrapped in AnyData with type lookup
            StringBuilder sb = new StringBuilder("test");
            Data data = factory.createConstValue(sb, context);

            assertInstanceOf(AnyData.class, data);
            assertTrue(data.isConst());
        }
    }

    @Nested
    class CreateSystemDataConstTests {

        @Test
        void testCreateSystemDataConstInt() {
            Data data = factory.createSystemDataConst(Identifier.fromName("PI_INT"), DataType.SystemDataType.INT, 3);

            assertInstanceOf(IntegerData.class, data);
            assertEquals(3, data.get());
        }

        @Test
        void testCreateSystemDataConstBoolean() {
            Data data = factory.createSystemDataConst(Identifier.fromName("TRUE"), DataType.SystemDataType.BOOLEAN, true);

            assertInstanceOf(BooleanData.class, data);
            assertEquals(true, data.get());
        }
    }

    @Nested
    class CustomDataTypeTests {

        @Test
        void testCreateVariableWithCustomDataType() {
            DataType customType = new DataTypeImpl("CustomType");
            Data data = factory.createVariable(Identifier.fromName("custom"), customType, null);

            assertInstanceOf(AnyData.class, data);
        }
    }
}
