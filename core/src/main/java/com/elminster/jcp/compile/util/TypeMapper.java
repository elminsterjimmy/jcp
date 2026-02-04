package com.elminster.jcp.compile.util;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.BinaryExpression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.literal.DoubleLiteral;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.compile.context.CompileContext;
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
        if (type == SystemDataType.DOUBLE) {
            return "D";
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
        if (type == SystemDataType.DOUBLE_ARRAY) {
            return "[D";
        }
        if (type == SystemDataType.STRING_ARRAY) {
            return "[Ljava/lang/String;";
        }
        if (type == SystemDataType.BOOLEAN_ARRAY) {
            return "[Z";
        }
        // For custom types (like StructType), use the type name as class descriptor
        if (type instanceof com.elminster.jcp.eval.data.StructType) {
            return "L" + type.getName() + ";";
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
        if (type == SystemDataType.DOUBLE) {
            return Opcodes.DLOAD;
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
        if (type == SystemDataType.DOUBLE) {
            return Opcodes.DSTORE;
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
        if (type == SystemDataType.DOUBLE) {
            return Opcodes.DRETURN;
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
        if (arrayType == SystemDataType.DOUBLE_ARRAY) {
            return SystemDataType.DOUBLE;
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
        return type == SystemDataType.INT || type == SystemDataType.BOOLEAN || type == SystemDataType.DOUBLE;
    }

    /**
     * Get the number of local variable slots a type occupies.
     * Doubles and longs use 2 slots, all others use 1.
     *
     * @param type the data type
     * @return the slot size (1 or 2)
     */
    public static int getSlotSize(DataType type) {
        return (type == SystemDataType.DOUBLE) ? 2 : 1;
    }

    /**
     * Alias for getSlotSize - get the size of a type for parameter indexing.
     *
     * @param type the data type
     * @return the slot size (1 or 2)
     */
    public static int getSize(DataType type) {
        return getSlotSize(type);
    }

    /**
     * Determine the data type of an expression at compile time.
     *
     * @param expr the expression
     * @param ctx  the compile context
     * @return the data type, or null if unknown
     */
    public static DataType getExpressionType(Expression expr, CompileContext ctx) {
        if (expr instanceof LiteralExpression) {
            LiteralExpression litExpr = (LiteralExpression) expr;
            Object literal = litExpr.getLiteral();
            if (literal instanceof IntLiteral) return SystemDataType.INT;
            if (literal instanceof DoubleLiteral) return SystemDataType.DOUBLE;
            if (literal instanceof BooleanLiteral) return SystemDataType.BOOLEAN;
            if (literal instanceof StringLiteral) return SystemDataType.STRING;
        }
        if (expr instanceof Identifier) {
            CompileContext.LocalVariable local = ctx.getLocal(((Identifier) expr).getId());
            return local != null ? local.getType() : null;
        }
        // For binary expressions, determine result type based on operands
        if (expr instanceof BinaryExpression) {
            BinaryExpression bin = (BinaryExpression) expr;
            DataType leftType = getExpressionType(bin.getLeft(), ctx);
            DataType rightType = getExpressionType(bin.getRight(), ctx);
            // Numeric promotion: if either is DOUBLE, result is DOUBLE
            if (leftType == SystemDataType.DOUBLE || rightType == SystemDataType.DOUBLE) {
                return SystemDataType.DOUBLE;
            }
            // Both INT → result is INT
            if (leftType == SystemDataType.INT && rightType == SystemDataType.INT) {
                return SystemDataType.INT;
            }
        }
        return null;  // Unknown
    }
}
