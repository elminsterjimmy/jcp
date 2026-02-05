package com.elminster.jcp.compile.struct;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.FieldAccessExpression;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import com.elminster.jcp.compile.util.TypeMapper;
import com.elminster.jcp.eval.data.StructType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for field access: obj.field
 * Reads and returns the value of a field from a struct instance.
 *
 * Example: p.x
 * Generates:
 *   ALOAD n    // load struct reference from local var
 *   GETFIELD Point.x I
 */
public class FieldAccessCompiler extends AbstractAstCompiler {

    public FieldAccessCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        FieldAccessExpression fieldAccess = (FieldAccessExpression) astNode;
        Expression objectExpr = fieldAccess.getObject();
        String fieldName = fieldAccess.getFieldName().getId();

        // Compile object expression (leaves struct reference on stack)
        AstCompilerFactory.getCompiler(objectExpr).compile(mv, ctx);

        // Need to determine struct type from the object expression
        // For now, we'll need to get the type from context if it's a variable
        // or from the expression itself if it's a struct instantiation
        StructType structType = getStructTypeFromExpression(objectExpr, ctx);

        if (structType == null) {
            throw new IllegalArgumentException("Cannot determine struct type for field access: " + fieldName);
        }

        String structName = structType.getName();

        // Look up the field to get its type
        StructFieldDef fieldDef = structType.getField(fieldName);
        if (fieldDef == null) {
            throw new IllegalArgumentException(
                "Struct " + structName + " has no field: " + fieldName);
        }

        // Get field descriptor
        String fieldDescriptor = TypeMapper.toDescriptor(fieldDef.getDataType());

        // Emit GETFIELD instruction
        mv.visitFieldInsn(
            Opcodes.GETFIELD,
            structName,
            fieldName,
            fieldDescriptor
        );

        // Result: field value left on stack
    }

    /**
     * Determine the struct type from an expression.
     * This is a helper method to figure out what struct type an expression returns.
     */
    private StructType getStructTypeFromExpression(Expression expr, CompileContext ctx) {
        // If it's an identifier (variable), look it up in the context
        if (expr instanceof com.elminster.jcp.ast.Identifier) {
            String varName = ((com.elminster.jcp.ast.Identifier) expr).getId();
            CompileContext.LocalVariable local = ctx.getLocal(varName);
            if (local != null && local.getType() instanceof StructType) {
                return (StructType) local.getType();
            }
        }

        // If it's an identifier expression, look it up
        if (expr instanceof com.elminster.jcp.ast.expression.operation.IdentifierExpression) {
            String varName = ((com.elminster.jcp.ast.expression.operation.IdentifierExpression) expr).getId();
            CompileContext.LocalVariable local = ctx.getLocal(varName);
            if (local != null && local.getType() instanceof StructType) {
                return (StructType) local.getType();
            }
        }

        // If it's a variable expression, look it up
        if (expr instanceof com.elminster.jcp.ast.expression.base.VariableExpression) {
            String varName = ((com.elminster.jcp.ast.expression.base.VariableExpression) expr).getId().getId();
            CompileContext.LocalVariable local = ctx.getLocal(varName);
            if (local != null && local.getType() instanceof StructType) {
                return (StructType) local.getType();
            }
        }

        // If it's a struct instantiation, get the type from the instantiation node
        if (expr instanceof com.elminster.jcp.ast.expression.StructInstantiation) {
            com.elminster.jcp.ast.expression.StructInstantiation structInst =
                (com.elminster.jcp.ast.expression.StructInstantiation) expr;
            String structName = structInst.getStructType().getId();
            return (StructType) ctx.getDataType(structName);
        }

        // If it's a nested field access, get the type of that field
        if (expr instanceof FieldAccessExpression) {
            FieldAccessExpression nestedAccess = (FieldAccessExpression) expr;
            StructType parentType = getStructTypeFromExpression(nestedAccess.getObject(), ctx);
            if (parentType != null) {
                String fieldName = nestedAccess.getFieldName().getId();
                StructFieldDef field = parentType.getField(fieldName);
                if (field != null && field.getDataType() instanceof StructType) {
                    return (StructType) field.getDataType();
                }
            }
        }

        return null;
    }
}
