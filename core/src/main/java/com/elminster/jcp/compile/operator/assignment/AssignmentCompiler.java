package com.elminster.jcp.compile.operator.assignment;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.operation.AssignmentExpression;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.context.CompileContext.LocalVariable;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import com.elminster.jcp.compile.util.TypeMapper;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for assignment expressions (=).
 */
public class AssignmentCompiler extends AbstractAstCompiler {

    public AssignmentCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        AssignmentExpression assignExpr = (AssignmentExpression) astNode;
        Identifier id = assignExpr.getId();
        Expression valueExpr = assignExpr.getExpression();

        // Get the variable name
        String varName = id.getId();
        LocalVariable local = ctx.getLocal(varName);
        if (local == null) {
            throw new CompileException("Undefined variable: " + varName, getSourceLocation());
        }

        // Compile the right-hand side expression
        Compilable rightCompiler = AstCompilerFactory.getCompiler(valueExpr);
        rightCompiler.compile(mv, ctx);

        // Store into the variable
        int storeOpcode = TypeMapper.getStoreOpcode(local.getType());
        mv.visitVarInsn(storeOpcode, local.getIndex());
    }
}
