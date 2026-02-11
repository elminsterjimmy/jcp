package com.elminster.jcp.eval.data;

import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enum defining JVM numeric widening conversions (JLS §5.1.2).
 *
 * <p>Each enum constant represents a valid widening conversion with its
 * source type, target type, and JVM opcode. This design follows the
 * Open-Closed Principle: add new conversions by adding enum values,
 * without modifying existing lookup methods.
 *
 * <h3>Widening vs Hierarchy:</h3>
 * <ul>
 *   <li>{@code isCastableTo()}: Parent-child hierarchy (INT is-a NUMERIC)</li>
 *   <li>{@code TypePromotion}: Cross-hierarchy conversion (INT → DOUBLE)</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * // Check if widening is allowed
 * if (TypePromotion.isWideningAllowed(INT, DOUBLE)) {
 *     // Get opcode for bytecode emission
 *     int opcode = TypePromotion.getPromotionOpcode(INT, DOUBLE);
 *     mv.visitInsn(opcode);  // Emits I2D
 * }
 * }</pre>
 *
 * <p><b>ARCHITECTURAL NOTE:</b> Despite being in eval/data/, this class serves
 * BOTH interpreter and compiler modes. Returns ASM opcodes for compiler
 * convenience. This is acceptable because type promotion rules are
 * semantically part of the type system.
 *
 * @see DataType#isCastableTo(DataType) for hierarchical type compatibility
 * @see <a href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-5.html">JLS §5.1.2</a>
 */
public enum TypePromotion {

    // ========================================
    // Widening Conversions (JLS §5.1.2)
    // Add new conversions here - no other changes needed!
    // ========================================

    /** int → double: Always safe, no precision loss */
    INT_TO_DOUBLE(SystemDataType.INT, SystemDataType.DOUBLE, Opcodes.I2D);

    // Future conversions - just uncomment when types are added:
    // INT_TO_LONG(SystemDataType.INT, SystemDataType.LONG, Opcodes.I2L),
    // INT_TO_FLOAT(SystemDataType.INT, SystemDataType.FLOAT, Opcodes.I2F),
    // LONG_TO_FLOAT(SystemDataType.LONG, SystemDataType.FLOAT, Opcodes.L2F),
    // LONG_TO_DOUBLE(SystemDataType.LONG, SystemDataType.DOUBLE, Opcodes.L2D),
    // FLOAT_TO_DOUBLE(SystemDataType.FLOAT, SystemDataType.DOUBLE, Opcodes.F2D),

    // ========================================
    // Lookup infrastructure (O(1) performance)
    // ========================================

    private static final Map<PromotionKey, TypePromotion> LOOKUP_MAP = new HashMap<>();

    static {
        for (TypePromotion promotion : values()) {
            LOOKUP_MAP.put(new PromotionKey(promotion.from, promotion.to), promotion);
        }
    }

    private static final class PromotionKey {
        private final DataType from;
        private final DataType to;

        PromotionKey(DataType from, DataType to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PromotionKey that = (PromotionKey) o;
            return from == that.from && to == that.to;
        }

        @Override
        public int hashCode() {
            return 31 * from.hashCode() + to.hashCode();
        }
    }

    // ========================================
    // Instance fields
    // ========================================

    private final DataType from;
    private final DataType to;
    private final int opcode;

    TypePromotion(DataType from, DataType to, int opcode) {
        this.from = from;
        this.to = to;
        this.opcode = opcode;
    }

    // ========================================
    // Public API (static methods)
    // ========================================

    /**
     * Check if widening conversion is allowed from source to target type.
     *
     * @param from source data type
     * @param to target data type
     * @return true if a widening conversion exists
     */
    public static boolean isWideningAllowed(DataType from, DataType to) {
        return LOOKUP_MAP.containsKey(new PromotionKey(from, to));
    }

    /**
     * Get the JVM opcode for widening conversion.
     *
     * @param from source data type
     * @param to target data type
     * @return the opcode (I2D, I2L, etc.) or -1 if no conversion exists
     */
    public static int getPromotionOpcode(DataType from, DataType to) {
        TypePromotion promotion = LOOKUP_MAP.get(new PromotionKey(from, to));
        return promotion != null ? promotion.opcode : -1;
    }

    /**
     * Find the TypePromotion enum for a given type pair.
     *
     * @param from source data type
     * @param to target data type
     * @return Optional containing the promotion, or empty if none exists
     */
    public static Optional<TypePromotion> find(DataType from, DataType to) {
        return Optional.ofNullable(LOOKUP_MAP.get(new PromotionKey(from, to)));
    }

    // ========================================
    // Instance accessors
    // ========================================

    public DataType getFrom() {
        return from;
    }

    public DataType getTo() {
        return to;
    }

    public int getOpcode() {
        return opcode;
    }
}
