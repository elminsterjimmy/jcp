package com.elminster.jcp.compile.declare;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.declaration.StructDeclaration;
import com.elminster.jcp.compile.StructClassGenerator;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import org.objectweb.asm.MethodVisitor;

/**
 * Compiler for struct declarations.
 * Registers the struct type and schedules class generation.
 */
public class StructDeclarationCompiler extends AbstractAstCompiler {

    public StructDeclarationCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        StructDeclaration structDecl = (StructDeclaration) astNode;
        String structName = structDecl.getId().getId();

        // Generate the struct class bytecode
        StructClassGenerator generator = new StructClassGenerator();
        byte[] structBytecode = generator.generateStructClass(structName, structDecl.getFields());

        // Register the struct class in the context for later loading
        ctx.addGeneratedClass(structName, structBytecode);

        // No bytecode needed in the main method for the declaration itself
        // The struct class will be loaded alongside the main class
    }
}
