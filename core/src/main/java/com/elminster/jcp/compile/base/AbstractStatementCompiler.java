package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;

/**
 * Base class for general statement compilers in the base package.
 *
 * <p>Statements (block, expression statement) do not produce a value on the JVM
 * operand stack. This base class provides a single concrete {@link #resolveType}
 * implementation that returns {@link SystemDataType#VOID}, eliminating the
 * identical override in each subclass.
 */
public abstract class AbstractStatementCompiler extends AbstractAstCompiler {

    protected AbstractStatementCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public DataType resolveType(CompileContext ctx) {
        return SystemDataType.VOID;
    }
}
