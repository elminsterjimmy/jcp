package com.elminster.jcp.compile.operator.relational;

import com.elminster.jcp.ast.Node;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for inequality (!=) expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.relational.NotEqualEvaluator}.
 */
public class NotEqualCompiler extends CompareCompiler {

    public NotEqualCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected int getCompareOpcode() {
        return Opcodes.IF_ICMPNE;
    }
}
