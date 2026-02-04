package com.elminster.jcp.compile.operator.arithmetic;

import com.elminster.jcp.ast.Node;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for addition expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.arithmetic.PlusEvaluator}.
 */
public class PlusCompiler extends ArithmeticCompiler {

    public PlusCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected void emitOperation(MethodVisitor mv) {
        mv.visitInsn(useDouble ? Opcodes.DADD : Opcodes.IADD);
    }
}
