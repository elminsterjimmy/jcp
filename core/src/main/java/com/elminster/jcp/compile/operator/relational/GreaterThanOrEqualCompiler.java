package com.elminster.jcp.compile.operator.relational;

import com.elminster.jcp.ast.Node;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for greater than or equal (>=) expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.relational.GreaterThanOrEqualEvaluator}.
 */
public class GreaterThanOrEqualCompiler extends CompareCompiler {

    public GreaterThanOrEqualCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected int getCompareOpcode() {
        return Opcodes.IF_ICMPGE;
    }

    @Override
    protected int getDoubleCompareOpcode() {
        return Opcodes.DCMPG;  // Use DCMPG for >= so NaN returns +1 (not greater or equal)
    }

    @Override
    protected int getDoubleConditionOpcode() {
        return Opcodes.IFGE;  // DCMPG result >= 0 means left >= right
    }
}
