package com.elminster.jcp.compile.declare;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.declaration.StructDeclaration;
import com.elminster.jcp.compile.StructClassGenerator;
import com.elminster.jcp.compile.context.CompileContext;
import com.elminster.jcp.eval.data.StructType;
import org.objectweb.asm.MethodVisitor;

/**
 * Compiler for type declarations (extended struct with constructor and methods).
 *
 * <p>Generates a complete JVM class with fields, constructor, instance methods,
 * and static methods. More powerful than simple struct declarations.
 *
 * <h3>Generated Class Structure:</h3>
 * <pre>{@code
 * // For: type Counter {
 * //   count: int
 * //   init(start: int) { this.count = start; }
 * //   increment(): void { this.count = this.count + 1; }
 * //   static create(): Counter { return Counter.new(0); }
 * // }
 *
 * public class Counter {
 *     public int count;
 *
 *     public Counter(int start) {   // from init()
 *         this.count = start;
 *     }
 *
 *     public void increment() {     // instance method
 *         this.count = this.count + 1;
 *     }
 *
 *     public static Counter create() {  // static method
 *         return new Counter(0);
 *     }
 * }
 * }</pre>
 *
 * <h4>Constructor Bytecode (from init):</h4>
 * <pre>{@code
 * // Counter.<init>(I)V
 *
 * ALOAD 0                           // load 'this'
 * INVOKESPECIAL Object.<init>()V    // call super constructor
 *
 * ALOAD 0                           // load 'this'
 * ILOAD 1                           // load 'start' parameter
 * PUTFIELD Counter.count I          // this.count = start
 *
 * RETURN
 * }</pre>
 *
 * <h4>Instance Method Bytecode:</h4>
 * <pre>{@code
 * // Counter.increment()V
 *
 * ALOAD 0                           // load 'this'
 * ALOAD 0                           // load 'this' for getfield
 * GETFIELD Counter.count I          // get current count
 * ICONST_1                          // push 1
 * IADD                              // compute count + 1
 * PUTFIELD Counter.count I          // store new count
 *
 * RETURN
 * }</pre>
 *
 * <h4>Static Method Bytecode:</h4>
 * <pre>{@code
 * // Counter.create()LCounter;
 *
 * NEW Counter                       // allocate Counter
 * DUP                               // duplicate reference
 * ICONST_0                          // push 0 argument
 * INVOKESPECIAL Counter.<init>(I)V  // call constructor
 * ARETURN                           // return Counter instance
 * }</pre>
 *
 * <h4>Dual Registration Pattern (CRITICAL):</h4>
 * <ul>
 *   <li>{@code ctx.addDataType()}: Enables compile-time lookups for other compilers.
 *       Omitting causes "Unknown struct type: TypeName" at compile time.</li>
 *   <li>{@code ctx.addGeneratedClass()}: Enables runtime loading via MultiClassLoader.
 *       Omitting causes NoClassDefFoundError at runtime.</li>
 * </ul>
 *
 * <h4>Main Method Effect:</h4>
 * <p>No bytecode emitted - type class is loaded alongside the main class.
 */
public class TypeDeclarationCompiler extends DeclarationCompiler {

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
