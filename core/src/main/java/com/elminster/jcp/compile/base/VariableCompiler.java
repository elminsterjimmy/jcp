package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.context.CompileContext.LocalVariable;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.compile.util.TypeMapper;
import org.objectweb.asm.MethodVisitor;

/**
 * Compiler for variable expressions (reading a variable).
 * Similar to {@link com.elminster.jcp.eval.base.VariableEvaluator}.
 */
public class VariableCompiler extends AbstractAstCompiler {

    public VariableCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        VariableExpression varExpr = (VariableExpression) astNode;
        String varName = varExpr.getId().getId();

        LocalVariable local = ctx.getLocal(varName);
        if (local == null) {
            throw new CompileException("Undefined variable: " + varName, getSourceLocation());
        }

        // Load the variable onto the stack
        int loadOpcode = TypeMapper.getLoadOpcode(local.getType());
        mv.visitVarInsn(loadOpcode, local.getIndex());
    }
}
