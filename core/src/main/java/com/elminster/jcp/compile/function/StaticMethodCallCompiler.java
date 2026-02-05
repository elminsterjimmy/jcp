package com.elminster.jcp.compile.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
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
 * Compiler for static method calls: Type.method(args)
 * Compiles arguments and emits INVOKESTATIC instruction.
 */
public class StaticMethodCallCompiler extends AbstractAstCompiler {

    public StaticMethodCallCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        StaticMethodCallExpression call = (StaticMethodCallExpression) astNode;
        String typeName = call.getTypeName().getId();
        String methodName = call.getMethodName();
        Expression[] args = call.getArguments();

        // 1. Look up the type
        DataType dataType = ctx.getDataType(typeName);
        if (!(dataType instanceof StructType)) {
            throw new CompileException("Type not found or not a struct type: " + typeName);
        }
        StructType structType = (StructType) dataType;

        // 2. Determine argument types for overload resolution
        DataType[] argTypes = new DataType[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = TypeMapper.getExpressionType(args[i], ctx);
        }

        // 3. Look up static method with overload resolution
        MethodDef method = structType.getStaticMethod(methodName, argTypes);
        if (method == null) {
            throw new CompileException("Static method '" + methodName +
                "' with argument types " + Arrays.toString(argTypes) +
                " not found in type '" + typeName + "'");
        }

        // 4. Compile arguments (push values onto stack)
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

        // 5. Build method descriptor
        String descriptor = buildMethodDescriptor(params, method.getReturnType());

        // 6. Emit INVOKESTATIC
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            typeName,
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
