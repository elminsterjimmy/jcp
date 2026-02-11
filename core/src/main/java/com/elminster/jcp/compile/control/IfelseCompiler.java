package com.elminster.jcp.compile.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.statement.control.IfElseStatement;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for if-else statements.
 * Similar to {@link com.elminster.jcp.eval.control.IfelseEvaluator}.
 */
public class IfelseCompiler extends AbstractAstCompiler {

    public IfelseCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        emitLineNumber(mv);
        IfElseStatement ifElseStmt = (IfElseStatement) astNode;
        Expression condition = ifElseStmt.getCondition();
        Statement ifStatement = ifElseStmt.getIfStatement();
        Statement elseStatement = ifElseStmt.getElseStatement();

        Label elseLabel = new Label();
        Label endLabel = new Label();

        // Compile the condition (pushes boolean onto stack)
        Compilable condCompiler = AstCompilerFactory.getCompiler(condition);
        condCompiler.compile(mv, ctx);

        // If condition is false (0), jump to else branch
        mv.visitJumpInsn(Opcodes.IFEQ, elseLabel);

        // Compile the 'if' branch
        Compilable ifCompiler = AstCompilerFactory.getCompiler(ifStatement);
        ifCompiler.compile(mv, ctx);

        // Jump over the else branch
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);

        // Else branch
        mv.visitLabel(elseLabel);
        if (elseStatement != null) {
            Compilable elseCompiler = AstCompilerFactory.getCompiler(elseStatement);
            elseCompiler.compile(mv, ctx);
        }

        mv.visitLabel(endLabel);
    }
}
