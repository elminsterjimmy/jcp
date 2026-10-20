package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import com.elminster.jcp.compile.util.TypeMapper;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for expression statements.
 *
 * <p>Compiles standalone expressions that are used as statements.
 * These are expressions whose return value (if any) is discarded.
 * Any value left on the stack by the expression is popped to keep
 * the operand stack balanced.
 *
 * @see com.elminster.jcp.eval.base.ExpressionStatementEvaluator
 */
public class ExpressionStatementCompiler extends AbstractStatementCompiler {

    public ExpressionStatementCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        ExpressionStatement exprStmt = (ExpressionStatement) astNode;
        Expression expression = exprStmt.getExpression();

        Compilable compilable = AstCompilerFactory.getCompiler(expression);
        compilable.compile(mv, ctx);

        DataType type = resolveExpressionType(expression, ctx);
        if (type != null && type != SystemDataType.VOID) {
            mv.visitInsn(TypeMapper.getSlotSize(type) == 2 ? Opcodes.POP2 : Opcodes.POP);
        }
    }

    private DataType resolveExpressionType(Expression expression, CompileContext ctx) {
        try {
            return TypeMapper.getExpressionType(expression, ctx);
        } catch (CompileException e) {
            return null;
        }
    }

}
