package com.elminster.jcp.compile.struct;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for 'this' keyword.
 * In JVM, 'this' is always local variable slot 0 in instance methods/constructors.
 */
public class ThisCompiler extends AbstractAstCompiler {

    public ThisCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        // 'this' is always local variable slot 0 in instance methods/constructors
        // This is a JVM convention - slot 0 holds the receiver reference
        mv.visitVarInsn(Opcodes.ALOAD, 0);
    }
}
