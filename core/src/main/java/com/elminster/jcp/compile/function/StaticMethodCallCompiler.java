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
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.eval.data.ExternalMethodDef;
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
        if (dataType == null) {
            throw new CompileException("Type not found: " + typeName);
        }

        // 2. Determine argument types for overload resolution
        DataType[] argTypes = new DataType[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = TypeMapper.getExpressionType(args[i], ctx);
        }

        // 3. Handle different type kinds
        if (dataType instanceof ExternalClassType) {
            compileExternalClassCall(mv, ctx, (ExternalClassType) dataType, methodName, args, argTypes);
        } else if (dataType instanceof StructType) {
            compileStructTypeCall(mv, ctx, (StructType) dataType, typeName, methodName, args, argTypes);
        } else {
            throw new CompileException("Type does not support static methods: " + typeName);
        }
    }

    /**
     * Compile static method call on ExternalClassType (external Java class).
     */
    private void compileExternalClassCall(MethodVisitor mv, CompileContext ctx,
                                          ExternalClassType extType, String methodName,
                                          Expression[] args, DataType[] argTypes) {
        // Look up static method with overload resolution
        ExternalMethodDef method = extType.getStaticMethod(methodName, argTypes);
        if (method == null) {
            throw new CompileException("Static method '" + methodName +
                "' with argument types " + Arrays.toString(argTypes) +
                " not found in type '" + extType.getName() + "'");
        }

        // Compile arguments (push values onto stack)
        DataType[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            Compilable argCompiler = AstCompilerFactory.getCompiler(args[i]);
            argCompiler.compile(mv, ctx);

            // Type promotion and boxing
            DataType argType = argTypes[i];
            DataType paramType = paramTypes[i];
            if (paramType == SystemDataType.DOUBLE && argType == SystemDataType.INT) {
                mv.visitInsn(Opcodes.I2D);
            } else if (paramType == SystemDataType.ANY) {
                // Box primitives when passing to Object parameter
                boxPrimitive(mv, argType);
            }
        }

        // Emit INVOKESTATIC with actual Java class internal name
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            extType.getInternalName(),
            methodName,
            method.getDescriptor(),
            false
        );
    }

    /**
     * Compile static method call on StructType (user-defined type).
     */
    private void compileStructTypeCall(MethodVisitor mv, CompileContext ctx,
                                       StructType structType, String typeName,
                                       String methodName, Expression[] args, DataType[] argTypes) {
        // Look up static method with overload resolution
        MethodDef method = structType.getStaticMethod(methodName, argTypes);
        if (method == null) {
            throw new CompileException("Static method '" + methodName +
                "' with argument types " + Arrays.toString(argTypes) +
                " not found in type '" + typeName + "'");
        }

        // Compile arguments (push values onto stack)
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

        // Build method descriptor
        String descriptor = buildMethodDescriptor(params, method.getReturnType());

        // Emit INVOKESTATIC
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            typeName,
            methodName,
            descriptor,
            false
        );
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

    /**
     * Box a primitive type to its wrapper class.
     * INT -> Integer, BOOLEAN -> Boolean, DOUBLE -> Double
     */
    private void boxPrimitive(MethodVisitor mv, DataType type) {
        if (type == SystemDataType.INT) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf",
                "(I)Ljava/lang/Integer;", false);
        } else if (type == SystemDataType.BOOLEAN) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf",
                "(Z)Ljava/lang/Boolean;", false);
        } else if (type == SystemDataType.DOUBLE) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf",
                "(D)Ljava/lang/Double;", false);
        }
        // String and Object types don't need boxing
    }
}
