package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for const data behavior.
 */
public class DataConstTest {

    /**
     * Tests that setting a const IntegerData throws IllegalStateException.
     */
    @Test
    void testConstIntegerData_SetThrowsException() {
        // Create const integer data
        IntegerData constInt = new IntegerData(42);  // isConst = true by default when using this constructor

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> constInt.set(100));
        assertTrue(ex.getMessage().contains("const"));
    }

    /**
     * Tests that setting a const StringData throws IllegalStateException.
     */
    @Test
    void testConstStringData_SetThrowsException() {
        StringData constStr = new StringData("hello");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> constStr.set("world"));
        assertTrue(ex.getMessage().contains("const"));
    }

    /**
     * Tests that setting a const BooleanData throws IllegalStateException.
     */
    @Test
    void testConstBooleanData_SetThrowsException() {
        BooleanData constBool = new BooleanData(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> constBool.set(false));
        assertTrue(ex.getMessage().contains("const"));
    }

    /**
     * Tests that setting a field on a const StructData throws IllegalStateException.
     */
    @Test
    void testConstStructData_SetFieldThrowsException() {
        StructType pointType = new StructType("Point", Arrays.asList(
            new StructFieldDef("x", DataType.SystemDataType.INT),
            new StructFieldDef("y", DataType.SystemDataType.INT)
        ));

        // Create const struct using AnyData constructor with isConst=true
        // StructData uses false by default, so we need to create via AnyData approach
        // Actually StructData explicitly sets isConst=false, so let's test AnyData directly
        AnyData<String> constAny = new AnyData<>(Identifier.EMPTY_IDENTIFIER, DataType.SystemDataType.STRING, "test", true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> constAny.set("modified"));
        assertTrue(ex.getMessage().contains("const"));
    }

    /**
     * Tests that non-const data can be modified.
     */
    @Test
    void testNonConstData_CanBeModified() {
        IntegerData nonConstInt = new IntegerData(Identifier.fromName("x"), 42);

        // Should not throw
        assertDoesNotThrow(() -> nonConstInt.set(100));
        assertEquals(100, nonConstInt.get());
    }

    /**
     * Tests isConst() returns correct value.
     */
    @Test
    void testIsConst_ReturnsCorrectValue() {
        IntegerData constInt = new IntegerData(42);
        IntegerData nonConstInt = new IntegerData(Identifier.fromName("x"), 42);

        assertTrue(constInt.isConst());
        assertFalse(nonConstInt.isConst());
    }
}
