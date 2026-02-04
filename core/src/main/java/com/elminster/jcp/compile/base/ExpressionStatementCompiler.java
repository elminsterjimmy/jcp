package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for expression statements.
 * Similar to {@link com.elminster.jcp.eval.base.ExpressionStatementEvaluator}.
 */
public class ExpressionStatementCompiler extends AbstractAstCompiler {

    public ExpressionStatementCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        ExpressionStatement exprStmt = (ExpressionStatement) astNode;
        Expression expression = exprStmt.getExpression();

        Compilable compilable = AstCompilerFactory.getCompiler(expression);
        compilable.compile(mv, ctx);

        // Pop the result if the expression leaves a value on the stack
        // TODO: Track whether expressions leave values on the stack
        // For now, we'll handle this on a case-by-case basis
    }
}
