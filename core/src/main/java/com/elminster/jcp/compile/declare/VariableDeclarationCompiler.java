package com.elminster.jcp.compile.declare;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.declaration.VariableDeclaration;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import com.elminster.jcp.compile.util.TypeMapper;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for variable declarations.
 * Similar to {@link com.elminster.jcp.eval.declare.VariableDeclarationEvaluator}.
 */
public class VariableDeclarationCompiler extends AbstractAstCompiler {

    public VariableDeclarationCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        VariableDeclaration varDecl = (VariableDeclaration) astNode;
        Identifier id = varDecl.getId();
        String varName = id.getId();

        // Get the data type
        DataType dataType = resolveDataType(varDecl.getDataType().getName());

        // Allocate a local variable slot
        int localIndex = ctx.allocateLocal(varName, dataType);

        // If there's an initializer, compile it
        Expression initExpr = varDecl.getInit();
        if (initExpr != null) {
            // Compile the initializer expression (pushes value onto stack)
            Compilable compilable = AstCompilerFactory.getCompiler(initExpr);
            compilable.compile(mv, ctx);

            // Store the value in the local variable
            int storeOpcode = TypeMapper.getStoreOpcode(dataType);
            mv.visitVarInsn(storeOpcode, localIndex);
        } else {
            // Initialize with default value
            compileDefaultValue(dataType, mv);
            int storeOpcode = TypeMapper.getStoreOpcode(dataType);
            mv.visitVarInsn(storeOpcode, localIndex);
        }
    }

    private DataType resolveDataType(String typeName) {
        // Try to match system data types
        for (SystemDataType sdt : SystemDataType.values()) {
            if (sdt.getName().equalsIgnoreCase(typeName)) {
                return sdt;
            }
        }
        // Default to ANY
        return SystemDataType.ANY;
    }

    private void compileDefaultValue(DataType type, MethodVisitor mv) {
        if (type == SystemDataType.INT) {
            mv.visitInsn(Opcodes.ICONST_0);
        } else if (type == SystemDataType.BOOLEAN) {
            mv.visitInsn(Opcodes.ICONST_0);
        } else if (type == SystemDataType.STRING) {
            mv.visitLdcInsn("");
        } else {
            // For reference types, push null
            mv.visitInsn(Opcodes.ACONST_NULL);
        }
    }
}
