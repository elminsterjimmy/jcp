package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StructData.
 */
class StructDataTest {

    private StructType pointType;

    @BeforeEach
    void setUp() {
        pointType = new StructType("Point", Arrays.asList(
            new StructFieldDef("x", SystemDataType.INT),
            new StructFieldDef("y", SystemDataType.INT)
        ));
    }

    @Nested
    class ConstructorTests {

        /**
         * Tests creating StructData with just type.
         * <pre>
         * new StructData(pointType)  // empty fields
         * </pre>
         */
        @Test
        void testConstructor_TypeOnly() {
            StructData data = new StructData(pointType);

            assertEquals(pointType, data.getStructType());
            assertNull(data.getField("x"));
        }

        /**
         * Tests creating StructData with identifier and type.
         * <pre>
         * new StructData(id, pointType)  // empty fields with identifier
         * </pre>
         */
        @Test
        void testConstructor_WithIdentifier() {
            StructData data = new StructData(Identifier.fromName("p"), pointType);

            assertEquals(pointType, data.getStructType());
            assertEquals("p", data.getIdentifier().getId());
        }

        /**
         * Tests creating StructData with predefined fields.
         * <pre>
         * new StructData(id, pointType, {x: 10, y: 20})
         * </pre>
         */
        @Test
        void testConstructor_WithFieldValues() {
            Map<String, Data> fields = new HashMap<>();
            fields.put("x", new IntegerData(10, false));
            fields.put("y", new IntegerData(20, false));

            StructData data = new StructData(Identifier.fromName("p"), pointType, fields);

            assertEquals(10, data.getField("x").get());
            assertEquals(20, data.getField("y").get());
        }
    }

    @Nested
    class FieldAccessTests {

        /**
         * Tests getting and setting fields.
         * <pre>
         * p.x = 10
         * p.getField("x")  // returns 10
         * </pre>
         */
        @Test
        void testSetAndGetField() {
            StructData data = new StructData(Identifier.fromName("p"), pointType);

            data.setField("x", new IntegerData(10, false));
            data.setField("y", new IntegerData(20, false));

            assertEquals(10, data.getField("x").get());
            assertEquals(20, data.getField("y").get());
        }

        /**
         * Tests getting non-existent field returns null.
         * <pre>
         * p.getField("z")  // returns null
         * </pre>
         */
        @Test
        void testGetNonExistentField() {
            StructData data = new StructData(Identifier.fromName("p"), pointType);

            assertNull(data.getField("z"));
        }
    }

    @Nested
    class ToStringTests {

        /**
         * Tests toString contains type name.
         * <pre>
         * p.toString()  // contains "Point"
         * </pre>
         */
        @Test
        void testToString() {
            StructData data = new StructData(Identifier.fromName("p"), pointType);
            data.setField("x", new IntegerData(10, false));

            String str = data.toString();
            assertTrue(str.contains("Point"));
            assertTrue(str.contains("p"));
        }
    }
}
