package com.elminster.jcp.compile.util;

import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CallSiteNarrowingTest {

    @Test
    void emitNarrowing_doubleToFloat_emitsD2F() {
        List<Integer> insns = captureInsns(SystemDataType.DOUBLE, float.class);
        assertTrue(insns.contains(Opcodes.D2F));
    }

    @Test
    void emitNarrowing_intToByte_emitsI2B() {
        List<Integer> insns = captureInsns(SystemDataType.INT, byte.class);
        assertTrue(insns.contains(Opcodes.I2B));
    }

    @Test
    void emitNarrowing_intToShort_emitsI2S() {
        List<Integer> insns = captureInsns(SystemDataType.INT, short.class);
        assertTrue(insns.contains(Opcodes.I2S));
    }

    @Test
    void emitNarrowing_longToInt_emitsL2I() {
        List<Integer> insns = captureInsns(SystemDataType.LONG, int.class);
        assertTrue(insns.contains(Opcodes.L2I));
    }

    @Test
    void emitNarrowing_stringToChar_emitsCharAt() {
        List<Integer> insns = captureInsns(SystemDataType.STRING, char.class);
        assertTrue(insns.contains(Opcodes.ICONST_0));
    }

    @Test
    void emitNarrowing_noNarrowingNeeded_emitsNothing() {
        List<Integer> insns = captureInsns(SystemDataType.INT, int.class);
        assertTrue(insns.isEmpty());
    }

    private List<Integer> captureInsns(SystemDataType jcpType, Class<?> javaType) {
        List<Integer> captured = new ArrayList<>();
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, "Test", null, "java/lang/Object", null);
        MethodVisitor rawMv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "test", "()V", null, null);
        MethodVisitor capturingMv = new MethodVisitor(Opcodes.ASM9, rawMv) {
            @Override
            public void visitInsn(int opcode) {
                captured.add(opcode);
                super.visitInsn(opcode);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                captured.add(opcode);
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        };
        CallSiteNarrowing.emitNarrowing(capturingMv, jcpType, javaType);
        rawMv.visitInsn(Opcodes.RETURN);
        rawMv.visitMaxs(2, 0);
        rawMv.visitEnd();
        cw.visitEnd();
        return captured;
    }
}
