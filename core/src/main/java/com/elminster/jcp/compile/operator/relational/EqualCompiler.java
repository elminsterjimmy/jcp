package com.elminster.jcp.compile.operator.relational;

import com.elminster.jcp.ast.Node;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for equality (==) expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.relational.EqualEvaluator}.
 */
public class EqualCompiler extends CompareCompiler {

    public EqualCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected int getCompareOpcode() {
        return Opcodes.IF_ICMPEQ;
    }
}
