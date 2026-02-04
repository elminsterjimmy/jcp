package com.elminster.jcp.compile.control;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.context.CompileContext.LoopLabels;
import com.elminster.jcp.compile.exception.CompileException;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for break statements.
 * Similar to {@link com.elminster.jcp.eval.control.BreakEvaluator}.
 */
public class BreakCompiler extends AbstractAstCompiler {

    public BreakCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        LoopLabels loop = ctx.currentLoop();
        if (loop == null) {
            throw new CompileException("break statement outside of loop");
        }

        // Jump to the end of the loop
        mv.visitJumpInsn(Opcodes.GOTO, loop.getEndLabel());
    }
}
