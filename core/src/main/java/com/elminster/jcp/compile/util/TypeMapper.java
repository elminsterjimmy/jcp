package com.elminster.jcp.compile.util;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.BinaryExpression;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.ThisExpression;
import com.elminster.jcp.ast.expression.UnaryExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.literal.DoubleLiteral;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.context.CompileContext.FunctionSignature;
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
     * Build a JVM method descriptor from parameter definitions and return type.
     * Example: (II)I for func add(int a, int b) -> int
     *
     * @param params     the parameter definitions
     * @param returnType the return type
     * @return the JVM method descriptor
     */
    public static String buildMethodDescriptor(ParameterDef[] params, DataType returnType) {
        StringBuilder sb = new StringBuilder("(");
        if (params != null) {
            for (ParameterDef param : params) {
                sb.append(toDescriptor(param.getDataType()));
            }
        }
        sb.append(")");
        sb.append(toDescriptor(returnType));
        return sb.toString();
    }

    /**
     * Build a JVM method descriptor from argument types and return type.
     * Used for overload resolution when we only have types, not parameter definitions.
     *
     * @param argTypes   the argument types
     * @param returnType the return type (can be null for partial descriptor)
     * @return the JVM method descriptor
     */
    public static String buildMethodDescriptor(DataType[] argTypes, DataType returnType) {
        StringBuilder sb = new StringBuilder("(");
        if (argTypes != null) {
            for (DataType argType : argTypes) {
                sb.append(toDescriptor(argType));
            }
        }
        sb.append(")");
        if (returnType != null) {
            sb.append(toDescriptor(returnType));
        }
        return sb.toString();
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
        // For external Java classes, use the actual JVM internal name
        if (type instanceof com.elminster.jcp.eval.data.ExternalClassType) {
            com.elminster.jcp.eval.data.ExternalClassType extType =
                (com.elminster.jcp.eval.data.ExternalClassType) type;
            return "L" + extType.getInternalName() + ";";
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
            // Handle generic Literal created by Literal.of() - check value type
            if (literal instanceof com.elminster.jcp.ast.expression.literal.Literal) {
                Object value = ((com.elminster.jcp.ast.expression.literal.Literal<?>) literal).getValue();
                if (value instanceof Integer) return SystemDataType.INT;
                if (value instanceof Double) return SystemDataType.DOUBLE;
                if (value instanceof Boolean) return SystemDataType.BOOLEAN;
                if (value instanceof String) return SystemDataType.STRING;
            }
        }
        // Handle 'this' expression
        if (expr instanceof ThisExpression) {
            CompileContext.LocalVariable local = ctx.getLocal("this");
            return local != null ? local.getType() : null;
        }
        if (expr instanceof Identifier) {
            CompileContext.LocalVariable local = ctx.getLocal(((Identifier) expr).getId());
            return local != null ? local.getType() : null;
        }
        // Handle VariableExpression (wraps Identifier)
        if (expr instanceof VariableExpression) {
            VariableExpression varExpr = (VariableExpression) expr;
            CompileContext.LocalVariable local = ctx.getLocal(varExpr.getId().getId());
            return local != null ? local.getType() : null;
        }
        // Handle function call expressions - return type from function signature
        if (expr instanceof FunctionCallExpression) {
            FunctionCallExpression call = (FunctionCallExpression) expr;
            String funcName = call.getId().getId();
            Expression[] args = call.getArguments();
            DataType[] argTypes = new DataType[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = getExpressionType(args[i], ctx);
            }
            FunctionSignature sig = ctx.lookupFunction(funcName, argTypes);
            return sig != null ? sig.getReturnType() : null;
        }
        // For binary expressions, determine result type based on operands and operator
        if (expr instanceof BinaryExpression) {
            BinaryExpression bin = (BinaryExpression) expr;
            String op = bin.getName();

            // Comparison operators always return BOOLEAN
            if ("EQUAL".equals(op) || "NOT_EQUAL".equals(op) ||
                "LESS_THAN".equals(op) || "GREATER_THAN".equals(op) ||
                "LESS_THAN_OR_EQUAL".equals(op) || "GREATER_THAN_OR_EQUAL".equals(op)) {
                return SystemDataType.BOOLEAN;
            }

            // Logical operators always return BOOLEAN
            if ("AND".equals(op) || "OR".equals(op)) {
                return SystemDataType.BOOLEAN;
            }

            // Arithmetic operators: determine result type based on operands
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
        // For unary expressions, determine result type based on operator
        if (expr instanceof UnaryExpression) {
            UnaryExpression unary = (UnaryExpression) expr;
            String op = unary.getName();

            // NOT operator returns BOOLEAN
            if ("NOT".equals(op)) {
                return SystemDataType.BOOLEAN;
            }

            // NEGATE operator returns the same type as operand
            if ("NEGATE".equals(op)) {
                return getExpressionType(unary.getExpress(), ctx);
            }
        }
        return null;  // Unknown
    }
}
