package com.elminster.jcp.compile;

import com.elminster.jcp.ast.Node;

/**
 * Interface for AST node compilers.
 * Similar to {@link com.elminster.jcp.eval.Evaluator}.
 */
public interface Compiler extends Compilable {

    /**
     * Get the AST node being compiled.
     *
     * @return the AST node
     */
    Node getAstNode();
}
