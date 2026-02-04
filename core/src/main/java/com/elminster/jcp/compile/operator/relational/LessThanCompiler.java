package com.elminster.jcp.compile.operator.relational;

import com.elminster.jcp.ast.Node;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for less than (<) expressions.
 * Similar to {@link com.elminster.jcp.eval.operator.relational.LessThanEvaluator}.
 */
public class LessThanCompiler extends CompareCompiler {

    public LessThanCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    protected int getCompareOpcode() {
        return Opcodes.IF_ICMPLT;
    }
}
