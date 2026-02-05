package com.elminster.jcp.compile.declare;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.declaration.StructDeclaration;
import com.elminster.jcp.compile.StructClassGenerator;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.eval.data.StructType;
import org.objectweb.asm.MethodVisitor;

/**
 * Compiler for type declarations (extended struct with methods).
 * Registers the type and generates the class with constructor and methods.
 *
 * <p>CRITICAL: Dual registration pattern is required:
 * 1. ctx.addDataType() - Enables compile-time lookups for other compilers
 * 2. ctx.addGeneratedClass() - Enables runtime loading via MultiClassLoader
 *
 * Omitting addDataType() causes: "Unknown struct type: TypeName" at compile time
 * Omitting addGeneratedClass() causes: NoClassDefFoundError at runtime
 */
public class TypeDeclarationCompiler extends AbstractAstCompiler {

    public TypeDeclarationCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        StructDeclaration decl = (StructDeclaration) astNode;
        String typeName = decl.getId().getId();

        // 1. Create and register StructType (for compile-time lookups)
        StructType structType = new StructType(
            typeName,
            decl.getFields(),
            decl.getConstructor(),
            decl.getInstanceMethods(),
            decl.getStaticMethods()
        );
        ctx.addDataType(structType);

        // 2. Generate class bytecode with methods
        StructClassGenerator generator = new StructClassGenerator();
        byte[] bytecode = generator.generateTypeClass(decl, structType, ctx);

        // 3. Register for runtime loading
        ctx.addGeneratedClass(typeName, bytecode);

        // No bytecode needed in the main method for the declaration itself
    }
}
