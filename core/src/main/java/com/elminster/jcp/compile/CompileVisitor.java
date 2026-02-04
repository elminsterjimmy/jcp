package com.elminster.jcp.compile;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.vistor.AstVisitor;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor for compiling AST nodes to bytecode.
 * Mirrors {@link com.elminster.jcp.eval.EvalVisitor} for compile mode.
 */
public class CompileVisitor implements AstVisitor {

    private static final Logger logger = LoggerFactory.getLogger(CompileVisitor.class);

    private final MethodVisitor methodVisitor;
    private final CompileContext context;

    public CompileVisitor(MethodVisitor methodVisitor, CompileContext context) {
        this.methodVisitor = methodVisitor;
        this.context = context;
    }

    public CompileContext getContext() {
        return context;
    }

    public MethodVisitor getMethodVisitor() {
        return methodVisitor;
    }

    @Override
    public void visit(Node node) {
        Compilable compilable = AstCompilerFactory.getCompiler(node);
        compilable.compile(methodVisitor, context);
        afterCompile(node);
    }

    protected void afterCompile(Node node) {
        if (logger.isDebugEnabled()) {
            logger.debug("Compiled: {}", node.getName());
        }
    }
}
