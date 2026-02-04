package com.elminster.jcp.compile.operator.arithmetic;

import com.elminster.jcp.ast.Node;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for subtraction expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.arithmetic.MinusEvaluator}.
 */
public class MinusCompiler extends ArithmeticCompiler {

    public MinusCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected void emitOperation(MethodVisitor mv) {
        mv.visitInsn(useDouble ? Opcodes.DSUB : Opcodes.ISUB);
    }
}
