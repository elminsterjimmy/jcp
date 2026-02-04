package com.elminster.jcp.compile.operator.arithmetic;

import com.elminster.jcp.ast.Node;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for division expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.arithmetic.DivideEvaluator}.
 */
public class DivideCompiler extends ArithmeticCompiler {

    public DivideCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected void emitOperation(MethodVisitor mv) {
        mv.visitInsn(useDouble ? Opcodes.DDIV : Opcodes.IDIV);
    }
}
