package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.compile.Compiler;

/**
 * Abstract base class for AST compilers.
 * Similar to {@link com.elminster.jcp.eval.base.AbstractAstEvaluator}.
 */
public abstract class AbstractAstCompiler implements AstCompiler {

    protected final Node astNode;

    public AbstractAstCompiler(Node astNode) {
        this.astNode = astNode;
    }

    @Override
    public Node getAstNode() {
        return astNode;
    }
}
