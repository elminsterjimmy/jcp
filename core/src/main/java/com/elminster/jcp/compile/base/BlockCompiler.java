package com.elminster.jcp.compile.base;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

/**
 * Compiler for Block statements.
 * Similar to {@link com.elminster.jcp.eval.base.BlockEvaluator}.
 */
public class BlockCompiler extends AbstractAstCompiler {

    public BlockCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        Block block = (Block) astNode;
        List<Statement> body = block.getBody();

        // Create a child context for the block scope
        CompileContext blockContext = ctx.createChildContext();

        for (Statement statement : body) {
            Compilable compilable = AstCompilerFactory.getCompiler(statement);
            compilable.compile(mv, blockContext);
        }
    }
}
