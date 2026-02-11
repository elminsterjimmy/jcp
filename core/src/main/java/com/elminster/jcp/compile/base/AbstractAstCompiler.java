package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.Locatable;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.compile.Compiler;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

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

    /**
     * Returns the source location of the current AST node being compiled.
     *
     * @return source location, or null if the node doesn't have location info
     */
    protected SourceLocation getSourceLocation() {
        return astNode instanceof Locatable ? ((Locatable) astNode).getLocation() : null;
    }

    /**
     * Emits line number information for the current AST node.
     * This enables meaningful stack traces when exceptions occur in compiled code.
     *
     * <p><b>CRITICAL:</b> Per ASM API, {@code visitLabel()} MUST be called
     * before {@code visitLineNumber()}, otherwise an IllegalArgumentException is thrown.
     *
     * @param mv the method visitor
     */
    protected void emitLineNumber(MethodVisitor mv) {
        SourceLocation loc = getSourceLocation();
        if (loc != null) {
            Label label = new Label();
            mv.visitLabel(label);
            mv.visitLineNumber(loc.getStartLine(), label);
        }
    }
}
