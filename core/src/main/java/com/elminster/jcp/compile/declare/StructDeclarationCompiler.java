package com.elminster.jcp.compile.declare;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.declaration.StructDeclaration;
import com.elminster.jcp.compile.StructClassGenerator;
import com.elminster.jcp.compile.base.AbstractAstCompiler;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.objectweb.asm.MethodVisitor;

/**
 * Compiler for struct declarations (data-only types).
 *
 * <p>Registers the struct type in the compile context and generates a separate
 * JVM class file for the struct. No bytecode is emitted to the main method.
 *
 * <h3>Generated Class Structure:</h3>
 * <pre>{@code
 * // For: struct Point { x: int, y: int }
 *
 * public class Point {
 *     public int x;
 *     public int y;
 *
 *     public Point(int x, int y) {
 *         this.x = x;
 *         this.y = y;
 *     }
 * }
 * }</pre>
 *
 * <h4>Constructor Bytecode:</h4>
 * <pre>{@code
 * // Point.<init>(II)V
 *
 * ALOAD 0                   // load 'this'
 * INVOKESPECIAL Object.<init>()V  // call super constructor
 *
 * ALOAD 0                   // load 'this'
 * ILOAD 1                   // load first parameter (x)
 * PUTFIELD Point.x I        // store in field
 *
 * ALOAD 0                   // load 'this'
 * ILOAD 2                   // load second parameter (y)
 * PUTFIELD Point.y I        // store in field
 *
 * RETURN                    // return void
 * }</pre>
 *
 * <h4>Dual Registration:</h4>
 * <ul>
 *   <li>{@code ctx.addDataType()}: Enables compile-time type lookups</li>
 *   <li>{@code ctx.addGeneratedClass()}: Enables runtime loading</li>
 * </ul>
 *
 * <h4>Main Method Effect:</h4>
 * <p>No bytecode emitted - struct class is loaded alongside the main class.
 */
public class StructDeclarationCompiler extends AbstractAstCompiler {

    public StructDeclarationCompiler(Node astNode) {
        super(astNode);
    }

    @Override
    public void compile(MethodVisitor mv, CompileContext ctx) {
        StructDeclaration structDecl = (StructDeclaration) astNode;
        String structName = structDecl.getId().getId();

        // Create and register the struct type in the context
        // This is needed so that StructInstantiationCompiler can look up the type
        com.elminster.jcp.eval.data.StructType structType =
            new com.elminster.jcp.eval.data.StructType(structName, structDecl.getFields());
        ctx.addDataType(structType);

        // Generate the struct class bytecode
        StructClassGenerator generator = new StructClassGenerator();
        byte[] structBytecode = generator.generateStructClass(structName, structDecl.getFields());

        // Register the struct class in the context for later loading
        ctx.addGeneratedClass(structName, structBytecode);

        // No bytecode needed in the main method for the declaration itself
        // The struct class will be loaded alongside the main class
    }
    @Override
    public DataType resolveType(CompileContext ctx) {
        return SystemDataType.VOID;  // Statements don't produce values
    }

}
