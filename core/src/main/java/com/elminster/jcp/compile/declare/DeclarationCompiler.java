package com.elminster.jcp.compile.declare;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;

/**
 * Base class for declaration statement compilers.
 *
 * <p>Declaration statements (function, struct, type, variable declarations) do not
 * produce a value on the JVM operand stack. This base class provides a single
 * concrete {@link #resolveType} implementation that returns {@link SystemDataType#VOID},
 * eliminating the identical override in each subclass.
 */
public abstract class DeclarationCompiler extends AbstractAstCompiler {

    protected DeclarationCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public DataType resolveType(CompileContext ctx) {
        return SystemDataType.VOID;
    }
}
