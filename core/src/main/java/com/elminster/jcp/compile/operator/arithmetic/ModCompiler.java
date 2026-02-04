package com.elminster.jcp.compile.operator.arithmetic;

import com.elminster.jcp.ast.Node;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for modulo expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.arithmetic.ModEvaluator}.
 */
public class ModCompiler extends ArithmeticCompiler {

    public ModCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected void emitOperation(MethodVisitor mv) {
        mv.visitInsn(useDouble ? Opcodes.DREM : Opcodes.IREM);
    }
}
