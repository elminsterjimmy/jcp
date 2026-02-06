package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ArrayData.
 */
class ArrayDataTest {

    @Test
    void testArrayData_WithDataOnly() {
        Integer[] data = {1, 2, 3};
        ArrayData<Integer[]> arrayData = new ArrayData<>(SystemDataType.INT, data);

        assertArrayEquals(data, arrayData.get());
        assertEquals(SystemDataType.INT, arrayData.getBaseDataType());
    }

    @Test
    void testArrayData_WithIdentifier() {
        Integer[] data = {1, 2, 3};
        Identifier id = Identifier.fromName("myArray");
        ArrayData<Integer[]> arrayData = new ArrayData<>(SystemDataType.INT, id, data);

        assertArrayEquals(data, arrayData.get());
        assertEquals(SystemDataType.INT, arrayData.getBaseDataType());
    }

    @Test
    void testArrayData_WithConst() {
        Integer[] data = {1, 2, 3};
        ArrayData<Integer[]> arrayData = new ArrayData<>(SystemDataType.INT, data, true);

        assertTrue(arrayData.isConst());
        assertArrayEquals(data, arrayData.get());
    }

    @Test
    void testArrayData_WithIdentifierAndConst() {
        Integer[] data = {1, 2, 3};
        Identifier id = Identifier.fromName("constArray");
        ArrayData<Integer[]> arrayData = new ArrayData<>(SystemDataType.INT, id, data, true);

        assertTrue(arrayData.isConst());
        assertArrayEquals(data, arrayData.get());
    }

    @Test
    void testArrayData_GetIdentifier() {
        Integer[] data = {1, 2, 3};
        ArrayData<Integer[]> arrayData = new ArrayData<>(SystemDataType.INT, data);

        assertEquals("Integer[]", arrayData.getIdentifier().getId());
    }

    @Test
    void testArrayData_StringArray() {
        String[] data = {"a", "b", "c"};
        ArrayData<String[]> arrayData = new ArrayData<>(SystemDataType.STRING, data);

        assertArrayEquals(data, arrayData.get());
        assertEquals(SystemDataType.STRING, arrayData.getBaseDataType());
        assertEquals("String[]", arrayData.getIdentifier().getId());
    }

    @Test
    void testArrayData_BooleanArray() {
        Boolean[] data = {true, false, true};
        ArrayData<Boolean[]> arrayData = new ArrayData<>(SystemDataType.BOOLEAN, data);

        assertArrayEquals(data, arrayData.get());
        assertEquals(SystemDataType.BOOLEAN, arrayData.getBaseDataType());
        assertEquals("Boolean[]", arrayData.getIdentifier().getId());
    }
}
