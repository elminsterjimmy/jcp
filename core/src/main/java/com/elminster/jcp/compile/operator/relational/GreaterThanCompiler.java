package com.elminster.jcp.compile.operator.relational;

import com.elminster.jcp.ast.Node;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for greater than (>) expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.relational.GreaterThanEvaluator}.
 */
public class GreaterThanCompiler extends CompareCompiler {

    public GreaterThanCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected int getCompareOpcode() {
        return Opcodes.IF_ICMPGT;
    }
}
