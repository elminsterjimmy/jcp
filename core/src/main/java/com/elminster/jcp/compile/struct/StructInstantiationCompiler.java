package com.elminster.jcp.compile.struct;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.StructInstantiation;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.compile.factory.AstCompilerFactory;
import com.elminster.jcp.compile.util.TypeMapper;
import com.elminster.jcp.eval.data.StructType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * Compiler for struct instantiation.
 * Generates bytecode to create a new struct instance.
 *
 * Example: Point(10, 20)
 * Generates:
 *   NEW Point
 *   DUP
 *   BIPUSH 10
 *   BIPUSH 20
 *   INVOKESPECIAL Point.<init>(II)V
 */
public class StructInstantiationCompiler extends AbstractAstCompiler {

    public StructInstantiationCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        StructInstantiation structInst = (StructInstantiation) astNode;
        String structName = structInst.getStructType().getId();

        // Look up struct type to get field definitions
        StructType structType = (StructType) ctx.getDataType(structName);
        if (structType == null) {
            throw new IllegalArgumentException("Unknown struct type: " + structName);
        }

        List<StructFieldDef> fields = structType.getFields();
        List<Expression> fieldValueExprs = structInst.getFieldValues();

        // Validate field count
        if (fields.size() != fieldValueExprs.size()) {
            throw new IllegalArgumentException(
                String.format("Struct %s expects %d fields, but got %d",
                    structName, fields.size(), fieldValueExprs.size()));
        }

        // Emit NEW instruction
        mv.visitTypeInsn(Opcodes.NEW, structName);

        // Emit DUP to have reference for constructor call
        mv.visitInsn(Opcodes.DUP);

        // Compile each field value expression
        for (int i = 0; i < fields.size(); i++) {
            Expression valueExpr = fieldValueExprs.get(i);
            AstCompilerFactory.getCompiler(valueExpr).compile(mv, ctx);

            // TODO: Type checking - verify value type matches field type
            // This would require type inference at compile time
        }

        // Build constructor descriptor
        StringBuilder descriptor = new StringBuilder("(");
        for (StructFieldDef field : fields) {
            descriptor.append(TypeMapper.toDescriptor(field.getDataType()));
        }
        descriptor.append(")V");

        // Emit INVOKESPECIAL to call constructor
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            structName,
            "<init>",
            descriptor.toString(),
            false
        );

        // Result: struct instance reference is on the stack
    }
}
