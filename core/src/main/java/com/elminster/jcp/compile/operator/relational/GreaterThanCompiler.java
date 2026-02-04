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

    @Override
    protected int getDoubleCompareOpcode() {
        return Opcodes.DCMPG;  // Use DCMPG for > so NaN returns +1 (not greater)
    }

    @Override
    protected int getDoubleConditionOpcode() {
        return Opcodes.IFGT;  // DCMPG result > 0 means left > right
    }
}
