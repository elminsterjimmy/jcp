package com.elminster.jcp.compile.util;

import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class CallSiteNarrowing {

    private CallSiteNarrowing() {
    }

    public static void emitNarrowing(MethodVisitor mv, DataType jcpArgType, Class<?> javaParamType) {
        if (jcpArgType == SystemDataType.DOUBLE && javaParamType == float.class) {
            mv.visitInsn(Opcodes.D2F);
        } else if (jcpArgType == SystemDataType.INT && javaParamType == byte.class) {
            mv.visitInsn(Opcodes.I2B);
        } else if (jcpArgType == SystemDataType.INT && javaParamType == short.class) {
            mv.visitInsn(Opcodes.I2S);
        } else if (jcpArgType == SystemDataType.LONG && javaParamType == int.class) {
            mv.visitInsn(Opcodes.L2I);
        } else if (jcpArgType == SystemDataType.STRING && javaParamType == char.class) {
            // Extract first character: String.charAt(0)
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt",
                    "(I)C", false);
        }
    }
}
