package com.elminster.jcp.compile.operator.relational;

import com.elminster.jcp.ast.Node;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for less than or equal (<=) expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.relational.LessThanOrEqualEvaluator}.
 */
public class LessThanOrEqualCompiler extends CompareCompiler {

    public LessThanOrEqualCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected int getCompareOpcode() {
        return Opcodes.IF_ICMPLE;
    }

    @Override
    protected int getDoubleConditionOpcode() {
        return Opcodes.IFLE;  // DCMPL result <= 0 means left <= right
    }
}
