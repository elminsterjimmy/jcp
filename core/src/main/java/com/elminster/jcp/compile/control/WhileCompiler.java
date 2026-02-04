package com.elminster.jcp.compile.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.statement.control.WhileStatement;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for while statements.
 * Similar to {@link com.elminster.jcp.eval.control.WhileEvaluator}.
 */
public class WhileCompiler extends AbstractAstCompiler {

    public WhileCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        WhileStatement whileStmt = (WhileStatement) astNode;
        Expression condition = whileStmt.getConditionExpression();
        Statement body = whileStmt.getBody();

        Label startLabel = new Label();
        Label endLabel = new Label();

        // Push loop labels for break/continue support
        ctx.pushLoop(startLabel, endLabel);

        // Loop start
        mv.visitLabel(startLabel);

        // Compile condition
        Compilable condCompiler = AstCompilerFactory.getCompiler(condition);
        condCompiler.compile(mv, ctx);

        // If condition is false (0), exit loop
        mv.visitJumpInsn(Opcodes.IFEQ, endLabel);

        // Compile loop body
        Compilable bodyCompiler = AstCompilerFactory.getCompiler(body);
        bodyCompiler.compile(mv, ctx);

        // Jump back to loop start
        mv.visitJumpInsn(Opcodes.GOTO, startLabel);

        // Loop end
        mv.visitLabel(endLabel);

        // Pop loop labels
        ctx.popLoop();
    }
}
