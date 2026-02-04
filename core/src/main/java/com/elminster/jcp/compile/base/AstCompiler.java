package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.compile.Compiler;

/**
 * Interface for AST node compilers.
 * Similar to {@link com.elminster.jcp.eval.base.AstEvaluator}.
 */
public interface AstCompiler extends Compiler {

    /**
     * Get the AST node being compiled.
     *
     * @return the AST node
     */
    Node getAstNode();
}
