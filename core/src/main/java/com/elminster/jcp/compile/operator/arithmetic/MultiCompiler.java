package com.elminster.jcp.compile.operator.arithmetic;

import com.elminster.jcp.ast.Node;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for multiplication expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.arithmetic.MultiEvaluator}.
 */
public class MultiCompiler extends ArithmeticCompiler {

    public MultiCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected void emitOperation(MethodVisitor mv) {
        mv.visitInsn(Opcodes.IMUL);
    }
}
