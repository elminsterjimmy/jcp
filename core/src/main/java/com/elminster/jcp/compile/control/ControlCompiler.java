package com.elminster.jcp.compile.control;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;

/**
 * Base class for control flow statement compilers.
 *
 * <p>Control flow statements (break, continue, if-else, return, while) do not
 * produce a value on the JVM operand stack. This base class provides a single
 * concrete {@link #resolveType} implementation that returns {@link SystemDataType#VOID},
 * eliminating the identical override in each subclass.
 */
public abstract class ControlCompiler extends AbstractAstCompiler {

    protected ControlCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public DataType resolveType(CompileContext ctx) {
        return SystemDataType.VOID;
    }
}
