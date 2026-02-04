package com.elminster.jcp.compile.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for return statements.
 * Similar to {@link com.elminster.jcp.eval.control.ReturnEvaluator}.
 */
public class ReturnCompiler extends AbstractAstCompiler {

    public ReturnCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        ReturnStatement returnStmt = (ReturnStatement) astNode;
        Expression returnExpr = returnStmt.getReturnExpression();

        if (returnExpr != null) {
            // Compile the return expression
            Compilable exprCompiler = AstCompilerFactory.getCompiler(returnExpr);
            exprCompiler.compile(mv, ctx);

            // TODO: Determine return type and use appropriate return instruction
            // For now, assume int return
            mv.visitInsn(Opcodes.IRETURN);
        } else {
            // Return void
            mv.visitInsn(Opcodes.RETURN);
        }
    }
}
