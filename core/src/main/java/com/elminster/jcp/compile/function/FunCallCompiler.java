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
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.eval.data.ExternalMethodDef;
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

        // Check if this is an external class constructor call (TypeName.new)
        if (funcName.endsWith(".new")) {
            String typeName = funcName.substring(0, funcName.length() - 4);
            DataType dataType = ctx.getDataType(typeName);
            if (dataType instanceof ExternalClassType) {
                compileExternalClassConstructor(mv, ctx, (ExternalClassType) dataType, args);
                return;
            }
        }

        // Determine argument types for overload resolution
        DataType[] argTypes = new DataType[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = TypeMapper.getExpressionType(args[i], ctx);
        }

        // Lookup function signature
        FunctionSignature sig = ctx.lookupFunction(funcName, argTypes);
        if (sig == null) {
            throw new CompileException("Undefined function: " + funcName +
                " with argument types " + Arrays.toString(argTypes), getSourceLocation());
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

    /**
     * Compile external class constructor call: TypeName.new(args)
     */
    private void compileExternalClassConstructor(MethodVisitor mv, CompileContext ctx,
                                                  ExternalClassType extType, Expression[] args) {
        // Determine argument types for overload resolution
        DataType[] argTypes = new DataType[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = TypeMapper.getExpressionType(args[i], ctx);
        }

        // Look up constructor with overload resolution
        ExternalMethodDef constructor = extType.getConstructor(argTypes);
        if (constructor == null) {
            throw new CompileException("Constructor for '" + extType.getName() +
                "' with argument types " + Arrays.toString(argTypes) + " not found", getSourceLocation());
        }

        // Emit NEW instruction
        mv.visitTypeInsn(Opcodes.NEW, extType.getInternalName());

        // Emit DUP to have reference for constructor call
        mv.visitInsn(Opcodes.DUP);

        // Compile arguments (push values onto stack)
        DataType[] paramTypes = constructor.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            Compilable argCompiler = AstCompilerFactory.getCompiler(args[i]);
            argCompiler.compile(mv, ctx);

            // Type promotion and boxing
            DataType argType = argTypes[i];
            DataType paramType = paramTypes[i];
            if (paramType == SystemDataType.DOUBLE && argType == SystemDataType.INT) {
                mv.visitInsn(Opcodes.I2D);
            } else if (paramType == SystemDataType.ANY) {
                boxPrimitive(mv, argType);
            }
        }

        // Emit INVOKESPECIAL to call constructor
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            extType.getInternalName(),
            "<init>",
            constructor.getDescriptor(),
            false
        );
        // Result: new instance reference is on the stack
    }

    /**
     * Box a primitive type to its wrapper class.
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
    }
}
