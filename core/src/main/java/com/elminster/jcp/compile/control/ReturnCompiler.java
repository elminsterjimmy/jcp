package com.elminster.jcp.compile.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import com.elminster.jcp.compile.util.TypeMapper;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
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
        Expression returnExpr = returnStmt.getExpression();

        // Get declared return type from context
        DataType returnType = ctx.getCurrentFunctionReturnType();

        // Validate return type is set
        if (returnType == null) {
            throw new CompileException("Return statement outside function context", getSourceLocation());
        }

        // Validate void/non-void consistency
        if (returnType == SystemDataType.VOID && returnExpr != null) {
            throw new CompileException("Void function cannot return a value", getSourceLocation());
        }
        if (returnType != SystemDataType.VOID && returnExpr == null) {
            throw new CompileException("Non-void function must return a value", getSourceLocation());
        }

        if (returnExpr != null) {
            // Compile the return expression
            Compilable exprCompiler = AstCompilerFactory.getCompiler(returnExpr);
            exprCompiler.compile(mv, ctx);

            // Emit correct return instruction based on return type
            int returnOpcode = TypeMapper.getReturnOpcode(returnType);
            mv.visitInsn(returnOpcode);
        } else {
            // Return void
            mv.visitInsn(Opcodes.RETURN);
        }
    }
}
