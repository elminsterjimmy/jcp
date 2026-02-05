package com.elminster.jcp.compile.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.context.CompileContext.FunctionSignature;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import com.elminster.jcp.compile.util.TypeMapper;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

/**
 * Compiler for function call expressions.
 * Compiles arguments and emits INVOKESTATIC instruction.
 */
public class FunCallCompiler extends AbstractAstCompiler {

    public FunCallCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        FunctionCallExpression call = (FunctionCallExpression) astNode;
        String funcName = call.getId().getId();
        Expression[] args = call.getArguments();

        // Determine argument types for overload resolution
        DataType[] argTypes = new DataType[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = TypeMapper.getExpressionType(args[i], ctx);
        }

        // Lookup function signature
        FunctionSignature sig = ctx.lookupFunction(funcName, argTypes);
        if (sig == null) {
            throw new CompileException("Undefined function: " + funcName +
                " with argument types " + Arrays.toString(argTypes));
        }

        // Compile arguments (push values onto stack)
        ParameterDef[] params = sig.getParameters();
        for (int i = 0; i < args.length; i++) {
            Compilable argCompiler = AstCompilerFactory.getCompiler(args[i]);
            argCompiler.compile(mv, ctx);

            // Type promotion if needed (int to double)
            DataType argType = argTypes[i];
            DataType paramType = params[i].getDataType();
            if (paramType == SystemDataType.DOUBLE && argType == SystemDataType.INT) {
                mv.visitInsn(Opcodes.I2D);
            }
        }

        // Emit INVOKESTATIC
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            ctx.getClassName(),
            funcName,
            sig.getDescriptor(),
            false
        );
        // Result (if any) is now on stack
    }
}
