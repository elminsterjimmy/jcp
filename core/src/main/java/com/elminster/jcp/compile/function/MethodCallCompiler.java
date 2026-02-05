package com.elminster.jcp.compile.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.statement.declaration.MethodDef;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import com.elminster.jcp.compile.util.TypeMapper;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.StructType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

/**
 * Compiler for instance method calls: instance.method(args)
 * Compiles target object, arguments, and emits INVOKEVIRTUAL instruction.
 */
public class MethodCallCompiler extends AbstractAstCompiler {

    public MethodCallCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        MethodCallExpression call = (MethodCallExpression) astNode;
        String methodName = call.getMethodName();
        Expression targetExpr = call.getExpression();
        Expression[] args = call.getArguments();

        // 1. Compile target expression (push instance reference onto stack)
        Compilable targetCompiler = AstCompilerFactory.getCompiler(targetExpr);
        targetCompiler.compile(mv, ctx);

        // 2. Determine the struct type of the target
        DataType targetType = TypeMapper.getExpressionType(targetExpr, ctx);
        if (!(targetType instanceof StructType)) {
            throw new CompileException("Cannot call method on non-struct type: " + targetType);
        }
        StructType structType = (StructType) targetType;

        // 3. Determine argument types for overload resolution
        DataType[] argTypes = new DataType[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = TypeMapper.getExpressionType(args[i], ctx);
        }

        // 4. Look up method with overload resolution (supports type hierarchy)
        MethodDef method = structType.getInstanceMethod(methodName, argTypes);
        if (method == null) {
            throw new CompileException("Method '" + methodName +
                "' with argument types " + Arrays.toString(argTypes) +
                " not found in type '" + structType.getName() + "'");
        }

        // 5. Compile arguments (push values onto stack)
        ParameterDef[] params = method.getParameters();
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

        // 6. Build method descriptor
        String descriptor = buildMethodDescriptor(params, method.getReturnType());

        // 7. Emit INVOKEVIRTUAL
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            structType.getName(),
            methodName,
            descriptor,
            false
        );
        // Result (if any) is now on stack
    }

    /**
     * Build method descriptor from parameters and return type.
     */
    private String buildMethodDescriptor(ParameterDef[] params, DataType returnType) {
        StringBuilder sb = new StringBuilder("(");
        if (params != null) {
            for (ParameterDef param : params) {
                sb.append(TypeMapper.toDescriptor(param.getDataType()));
            }
        }
        sb.append(")");
        sb.append(TypeMapper.toDescriptor(returnType));
        return sb.toString();
    }
}
