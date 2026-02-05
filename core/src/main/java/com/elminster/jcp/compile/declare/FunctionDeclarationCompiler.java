package com.elminster.jcp.compile.declare;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclaration;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import com.elminster.jcp.eval.data.DataType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Compiler for function declarations.
 * In two-pass mode:
 * - compile() registers the function signature (Pass 1)
 * - compileBody() generates the function body bytecode (Pass 2)
 */
public class FunctionDeclarationCompiler extends AbstractAstCompiler {

    public FunctionDeclarationCompiler(Node astNode) {
        super(astNode);
    }

    /**
     * Register the function signature in the context.
     * Called during Pass 1 or when encountered in main method.
     * No bytecode is emitted to the main method.
     */
    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        FunctionDeclaration fd = (FunctionDeclaration) astNode;
        ctx.registerFunction(
            fd.getId().getId(),
            fd.getParameterDefines(),
            fd.getDataType()
        );
        // No bytecode emitted to main method's MethodVisitor
    }

    /**
     * Compile the function body statements.
     * Called by BytecodeGenerator.generateFunctionMethod() during Pass 2.
     *
     * @param mv      the MethodVisitor for this function's method
     * @param funcCtx the compilation context for this function (isolated locals)
     */
    public void compileBody(MethodVisitor mv, CompileContext funcCtx) {
        FunctionDeclaration fd = (FunctionDeclaration) astNode;

        // Allocate parameters as local variables (starting at index 0 for static methods)
        for (ParameterDef param : fd.getParameterDefines()) {
            funcCtx.allocateLocal(param.getId(), param.getDataType());
        }

        // Compile body statements
        for (Statement stmt : fd.getBody()) {
            Compilable compiler = AstCompilerFactory.getCompiler(stmt);
            compiler.compile(mv, funcCtx);
        }

        // Add implicit return for void functions
        if (fd.getDataType() == DataType.SystemDataType.VOID) {
            mv.visitInsn(Opcodes.RETURN);
        }
    }
}
