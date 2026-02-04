package com.elminster.jcp.compile;

import com.elminster.jcp.compile.context.CompileContext;
import org.objectweb.asm.MethodVisitor;

/**
 * Interface for AST nodes that can be compiled to bytecode.
 * Similar to {@link com.elminster.jcp.eval.Evaluable} but for compilation.
 */
public interface Compilable {

    /**
     * Compile this AST node to JVM bytecode.
     *
     * @param mv  the ASM MethodVisitor to emit bytecode
     * @param ctx the compilation context
     */
    void compile(MethodVisitor mv, CompileContext ctx);
}
