package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.context.CompileContext.LocalVariable;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.compile.util.TypeMapper;
import org.objectweb.asm.MethodVisitor;

/**
 * Compiler for identifier expressions (reading a variable by name).
 */
public class IdentifierCompiler extends AbstractAstCompiler {

    public IdentifierCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        String varName;

        if (astNode instanceof IdentifierExpression) {
            varName = ((IdentifierExpression) astNode).getId();
        } else if (astNode instanceof Identifier) {
            varName = ((Identifier) astNode).getId();
        } else {
            throw new CompileException("Unknown identifier type: " + astNode.getClass().getSimpleName(), getSourceLocation());
        }

        LocalVariable local = ctx.getLocal(varName);
        if (local == null) {
            throw new CompileException("Undefined variable: " + varName, getSourceLocation());
        }

        // Load the variable onto the stack
        int loadOpcode = TypeMapper.getLoadOpcode(local.getType());
        mv.visitVarInsn(loadOpcode, local.getIndex());
    }
}
