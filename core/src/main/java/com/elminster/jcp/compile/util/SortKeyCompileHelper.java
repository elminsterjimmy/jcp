package com.elminster.jcp.compile.util;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.compile.Compilable;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.module.base.arrays.SortKey;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Shared bytecode helper for {@code SortKey.sortByFunc(String)} → two-arg transformation.
 *
 * <p>Both {@code FunCallCompiler} and {@code StaticMethodCallCompiler} need to transform
 * the one-arg call into the two-arg version that includes the declaring class. This helper
 * centralises that logic.
 */
public final class SortKeyCompileHelper {

    static final String SORT_KEY_INTERNAL =
        "com/elminster/jcp/module/base/arrays/SortKey";

    private SortKeyCompileHelper() {}

    /**
     * Emits bytecode for {@code SortKey.sortByFunc(String, Class)} given a single
     * string argument expression and the current compile context.
     */
    public static void emitSortByFuncWithClass(MethodVisitor mv, CompileContext ctx,
                                                Expression stringArg) {
        Compilable argCompiler = AstCompilerFactory.getCompiler(stringArg);
        argCompiler.compile(mv, ctx);

        mv.visitLdcInsn(Type.getObjectType(ctx.getClassName()));

        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            SORT_KEY_INTERNAL,
            "sortByFunc",
            "(Ljava/lang/String;Ljava/lang/Class;)L" + SORT_KEY_INTERNAL + ";",
            false
        );
    }

    /**
     * Returns {@code true} if the given method is {@code SortKey.sortByFunc(String)}.
     */
    public static boolean isSortByFuncOneArg(java.lang.reflect.Method method) {
        return "sortByFunc".equals(method.getName())
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == String.class;
    }

    /**
     * Returns {@code true} if this is a {@code SortKey.sortByFunc(String)} call
     * via the {@code StaticMethodCallExpression} path (ExternalClassType match).
     */
    public static boolean isSortByFuncExternalClass(ExternalClassType extType,
                                                     String methodName,
                                                     Expression[] args,
                                                     com.elminster.jcp.eval.data.DataType[] argTypes) {
        return extType.getJavaClass() == SortKey.class
            && "sortByFunc".equals(methodName)
            && args.length == 1
            && argTypes[0] == SystemDataType.STRING;
    }
}
