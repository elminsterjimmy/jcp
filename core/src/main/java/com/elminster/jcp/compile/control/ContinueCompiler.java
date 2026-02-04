package com.elminster.jcp.compile.control;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.context.CompileContext.LoopLabels;
import com.elminster.jcp.compile.exception.CompileException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for continue statements.
 * Similar to {@link com.elminster.jcp.eval.control.ContinueEvaluator}.
 */
public class ContinueCompiler extends AbstractAstCompiler {

    public ContinueCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        LoopLabels loop = ctx.currentLoop();
        if (loop == null) {
            throw new CompileException("continue statement outside of loop");
        }

        // Jump to the start of the loop
        mv.visitJumpInsn(Opcodes.GOTO, loop.getStartLabel());
    }
}
