package com.elminster.jcp.eval.data;

import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TypePromotion enum.
 */
class TypePromotionTest {

    @Test
    void isWideningAllowed_IntToDouble_ReturnsTrue() {
        assertTrue(TypePromotion.isWideningAllowed(SystemDataType.INT, SystemDataType.DOUBLE));
    }

    @Test
    void isWideningAllowed_DoubleToInt_ReturnsFalse() {
        // Narrowing conversion is NOT allowed
        assertFalse(TypePromotion.isWideningAllowed(SystemDataType.DOUBLE, SystemDataType.INT));
    }

    @Test
    void isWideningAllowed_IntToInt_ReturnsFalse() {
        // Use isCastableTo() for same-type checks, not TypePromotion
        assertFalse(TypePromotion.isWideningAllowed(SystemDataType.INT, SystemDataType.INT));
    }

    @Test
    void isWideningAllowed_StringToInt_ReturnsFalse() {
        assertFalse(TypePromotion.isWideningAllowed(SystemDataType.STRING, SystemDataType.INT));
    }

    @Test
    void isWideningAllowed_IntToNumeric_ReturnsFalse() {
        // Hierarchy check is handled by isCastableTo(), not TypePromotion
        assertFalse(TypePromotion.isWideningAllowed(SystemDataType.INT, SystemDataType.NUMERIC));
    }

    @Test
    void getPromotionOpcode_IntToDouble_ReturnsI2D() {
        assertEquals(Opcodes.I2D, TypePromotion.getPromotionOpcode(SystemDataType.INT, SystemDataType.DOUBLE));
    }

    @Test
    void getPromotionOpcode_InvalidPromotion_ReturnsNegativeOne() {
        assertEquals(-1, TypePromotion.getPromotionOpcode(SystemDataType.DOUBLE, SystemDataType.INT));
    }

    @Test
    void find_IntToDouble_ReturnsEnumValue() {
        Optional<TypePromotion> promotion = TypePromotion.find(SystemDataType.INT, SystemDataType.DOUBLE);
        assertTrue(promotion.isPresent());
        assertEquals(TypePromotion.INT_TO_DOUBLE, promotion.get());
    }

    @Test
    void find_InvalidPromotion_ReturnsEmpty() {
        Optional<TypePromotion> promotion = TypePromotion.find(SystemDataType.DOUBLE, SystemDataType.INT);
        assertTrue(promotion.isEmpty());
    }

    @Test
    void enumValues_AllHaveValidOpcodes() {
        for (TypePromotion promotion : TypePromotion.values()) {
            assertTrue(promotion.getOpcode() > 0, "Invalid opcode for " + promotion);
            assertNotNull(promotion.getFrom(), "Null 'from' type for " + promotion);
            assertNotNull(promotion.getTo(), "Null 'to' type for " + promotion);
        }
    }

    @Test
    void enumAccessors_ReturnCorrectValues() {
        TypePromotion promotion = TypePromotion.INT_TO_DOUBLE;
        assertEquals(SystemDataType.INT, promotion.getFrom());
        assertEquals(SystemDataType.DOUBLE, promotion.getTo());
        assertEquals(Opcodes.I2D, promotion.getOpcode());
    }

    @Test
    void hierarchyUnchanged_IntNotCastableToDouble() {
        // Verify that isCastableTo() does NOT include widening conversions
        // This ensures we haven't accidentally changed the hierarchy semantics
        assertFalse(SystemDataType.INT.isCastableTo(SystemDataType.DOUBLE));
    }

    @Test
    void hierarchyUnchanged_IntIsCastableToNumeric() {
        // Verify that hierarchical casts still work
        assertTrue(SystemDataType.INT.isCastableTo(SystemDataType.NUMERIC));
    }
}
