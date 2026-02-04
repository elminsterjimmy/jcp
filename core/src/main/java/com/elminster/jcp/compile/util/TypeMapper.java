package com.elminster.jcp.compile.util;

import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Utility class for mapping JCP types to JVM types.
 */
public final class TypeMapper {

    private TypeMapper() {
    }

    /**
     * Get the JVM type descriptor for a JCP data type.
     *
     * @param type the JCP data type
     * @return the JVM type descriptor
     */
    public static String toDescriptor(DataType type) {
        if (type == SystemDataType.INT) {
            return "I";
        }
        if (type == SystemDataType.BOOLEAN) {
            return "Z";
        }
        if (type == SystemDataType.STRING) {
            return "Ljava/lang/String;";
        }
        if (type == SystemDataType.VOID) {
            return "V";
        }
        if (type == SystemDataType.INT_ARRAY) {
            return "[I";
        }
        if (type == SystemDataType.STRING_ARRAY) {
            return "[Ljava/lang/String;";
        }
        if (type == SystemDataType.BOOLEAN_ARRAY) {
            return "[Z";
        }
        // Default to Object for ANY or unknown types
        return "Ljava/lang/Object;";
    }

    /**
     * Get the ASM Type for a JCP data type.
     *
     * @param dataType the JCP data type
     * @return the ASM Type
     */
    public static Type toAsmType(DataType dataType) {
        return Type.getType(toDescriptor(dataType));
    }

    /**
     * Get the appropriate LOAD instruction opcode for a type.
     *
     * @param type the JCP data type
     * @return the LOAD opcode (ILOAD, ALOAD, etc.)
     */
    public static int getLoadOpcode(DataType type) {
        if (type == SystemDataType.INT || type == SystemDataType.BOOLEAN) {
            return Opcodes.ILOAD;
        }
        // Objects and arrays use ALOAD
        return Opcodes.ALOAD;
    }

    /**
     * Get the appropriate STORE instruction opcode for a type.
     *
     * @param type the JCP data type
     * @return the STORE opcode (ISTORE, ASTORE, etc.)
     */
    public static int getStoreOpcode(DataType type) {
        if (type == SystemDataType.INT || type == SystemDataType.BOOLEAN) {
            return Opcodes.ISTORE;
        }
        // Objects and arrays use ASTORE
        return Opcodes.ASTORE;
    }

    /**
     * Get the appropriate RETURN instruction opcode for a type.
     *
     * @param type the JCP data type
     * @return the RETURN opcode (IRETURN, ARETURN, RETURN, etc.)
     */
    public static int getReturnOpcode(DataType type) {
        if (type == SystemDataType.VOID) {
            return Opcodes.RETURN;
        }
        if (type == SystemDataType.INT || type == SystemDataType.BOOLEAN) {
            return Opcodes.IRETURN;
        }
        return Opcodes.ARETURN;
    }

    /**
     * Get the array element type for an array type.
     *
     * @param arrayType the array data type
     * @return the element data type
     */
    public static DataType getArrayElementType(DataType arrayType) {
        if (arrayType == SystemDataType.INT_ARRAY) {
            return SystemDataType.INT;
        }
        if (arrayType == SystemDataType.STRING_ARRAY) {
            return SystemDataType.STRING;
        }
        if (arrayType == SystemDataType.BOOLEAN_ARRAY) {
            return SystemDataType.BOOLEAN;
        }
        return SystemDataType.ANY;
    }

    /**
     * Check if a type is a primitive type.
     *
     * @param type the data type
     * @return true if primitive (int, boolean)
     */
    public static boolean isPrimitive(DataType type) {
        return type == SystemDataType.INT || type == SystemDataType.BOOLEAN;
    }
}
