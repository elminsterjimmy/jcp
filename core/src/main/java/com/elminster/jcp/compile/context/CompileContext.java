package com.elminster.jcp.compile.context;

import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.compile.util.TypeMapper;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.NamespacedTypeTable;
import com.elminster.jcp.eval.excpetion.AmbiguousTypeException;
import org.objectweb.asm.Label;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compilation context that tracks local variables, types, and control flow labels.
 *
 * <p>This class bridges JCP's type system with JVM's runtime model. It maintains
 * compile-time state that corresponds to JVM runtime structures.
 *
 * <h2>JVM Local Variable Table Mapping</h2>
 *
 * <p>The JVM uses a local variable table (LVT) as an indexed array of slots for
 * method-local storage. This context tracks variable-to-slot mappings:
 *
 * <pre>{@code
 * // JCP source:
 * func example(a: int, b: double): int {
 *     var c: int = 0;
 *     var d: String = "hi";
 *     ...
 * }
 *
 * // JVM Local Variable Table:
 * // Slot 0: a (int)      - 1 slot
 * // Slot 1: b (double)   - 2 slots (doubles use 2 slots)
 * // Slot 3: c (int)      - 1 slot
 * // Slot 4: d (String)   - 1 slot (reference)
 * }</pre>
 *
 * <p><b>Slot Size Rules:</b>
 * <ul>
 *   <li>int, boolean: 1 slot</li>
 *   <li>double, long: 2 slots</li>
 *   <li>Object references (String, structs): 1 slot</li>
 * </ul>
 *
 * <h2>Instance Methods vs Static Methods</h2>
 *
 * <pre>{@code
 * // Instance method: slot 0 = 'this'
 * // type Counter {
 * //   func increment(): void { this.count++; }
 * // }
 * // Slot 0: this (Counter reference)
 *
 * // Static method: slot 0 = first parameter
 * // func add(a: int, b: int): int { return a + b; }
 * // Slot 0: a (int)
 * // Slot 1: b (int)
 * }</pre>
 *
 * <h2>Scope and Parent Chain</h2>
 *
 * <p>Nested scopes (blocks, loops) create child contexts. Variable lookups
 * traverse the parent chain, mirroring JVM's single flat LVT per method:
 *
 * <pre>{@code
 * // JCP source:
 * func example(): void {
 *     var x: int = 1;       // Root context: x at slot 0
 *     {
 *         var y: int = 2;   // Child context: y at slot 1
 *         x = x + y;        // x found via parent lookup
 *     }
 *     // y no longer accessible (scope ended)
 * }
 * }</pre>
 *
 * <p><b>Note:</b> Unlike the JVM which doesn't reclaim slots, this implementation
 * continues allocating new slots. Slot reuse optimization is not implemented.
 *
 * <h2>Loop Labels for Control Flow</h2>
 *
 * <p>JVM bytecode uses labels for control flow. This context manages a stack
 * of loop labels enabling break/continue:
 *
 * <pre>{@code
 * // JCP source:
 * while (cond) {           // pushLoop(startLabel, endLabel)
 *     if (x) break;        // GOTO endLabel
 *     if (y) continue;     // GOTO startLabel
 * }                        // popLoop()
 *
 * // Nested loops:
 * while (outer) {          // push outer labels
 *     while (inner) {      // push inner labels
 *         break;           // uses inner's endLabel
 *     }                    // pop inner
 * }                        // pop outer
 * }</pre>
 *
 * <h2>Function Registry for Forward References</h2>
 *
 * <p>Enables mutual recursion and calling functions declared later in source:
 *
 * <pre>{@code
 * // Pass 1: Register all function signatures
 * // func foo(): void { bar(); }  -> register foo(V)
 * // func bar(): void { foo(); }  -> register bar(V)
 *
 * // Pass 2: Generate bytecode
 * // foo() can call bar() because bar's signature is already known
 * }</pre>
 *
 * <h2>Type Table for Custom Types</h2>
 *
 * <p>Maps type names to DataType for struct/type instantiation and method calls:
 *
 * <pre>{@code
 * // struct Point { x: int, y: int }
 * // Registered: "Point" -> StructType(name="Point", fields=[x:int, y:int])
 *
 * // var p: Point = Point(1, 2);
 * // Lookup "Point" to get field types for constructor descriptor
 * }</pre>
 *
 * <h2>Generated Classes Registry</h2>
 *
 * <p>Structs/types compile to separate JVM classes. This registry collects
 * bytecode for all auxiliary classes to be loaded together:
 *
 * <pre>{@code
 * // Main class: Program.class
 * // Struct classes: Point.class, Line.class
 * // All loaded via MultiClassLoader using generatedClasses map
 * }</pre>
 *
 * @see com.elminster.jcp.eval.context.EvalContext
 * @see com.elminster.jcp.compile.util.TypeMapper
 */
public class CompileContext {

    /**
     * Represents a function signature for overload resolution and bytecode generation.
     *
     * <p>Maps to JVM method structure with name, parameter types, and return type.
     * The descriptor field contains the JVM method descriptor string.
     *
     * <h3>JVM Method Descriptor Format:</h3>
     * <pre>{@code
     * // func add(a: int, b: int): int
     * // Descriptor: "(II)I"
     * //             ^^   ^
     * //             ||   +-- Return type: int
     * //             |+------ Second param: int
     * //             +------- First param: int
     *
     * // func greet(name: String): void
     * // Descriptor: "(Ljava/lang/String;)V"
     *
     * // func calculate(x: double, y: double): double
     * // Descriptor: "(DD)D"
     * }</pre>
     *
     * <h3>Type Descriptors:</h3>
     * <table>
     *   <tr><th>JCP Type</th><th>JVM Descriptor</th></tr>
     *   <tr><td>int</td><td>I</td></tr>
     *   <tr><td>boolean</td><td>Z</td></tr>
     *   <tr><td>double</td><td>D</td></tr>
     *   <tr><td>void</td><td>V</td></tr>
     *   <tr><td>String</td><td>Ljava/lang/String;</td></tr>
     *   <tr><td>Point (struct)</td><td>LPoint;</td></tr>
     * </table>
     */
    public static class FunctionSignature {
        private final String name;
        private final ParameterDef[] parameters;
        private final DataType returnType;
        private final String descriptor;

        public FunctionSignature(String name, ParameterDef[] parameters, DataType returnType) {
            this.name = name;
            this.parameters = parameters != null ? parameters : new ParameterDef[0];
            this.returnType = returnType;
            this.descriptor = TypeMapper.buildMethodDescriptor(this.parameters, returnType);
        }

        public String getName() {
            return name;
        }

        public ParameterDef[] getParameters() {
            return parameters;
        }

        public DataType getReturnType() {
            return returnType;
        }

        public String getDescriptor() {
            return descriptor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FunctionSignature that = (FunctionSignature) o;
            return Objects.equals(name, that.name) && Objects.equals(descriptor, that.descriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, descriptor);
        }
    }

    /**
     * Key for function registry using name + parameter descriptor for O(1) lookup.
     *
     * <p>JVM identifies methods by name + descriptor, allowing overloading.
     * This key mirrors that identification scheme:
     *
     * <pre>{@code
     * // These are different methods (overloaded):
     * // func print(x: int): void      -> FunctionSignatureKey("print", "(I)")
     * // func print(x: String): void   -> FunctionSignatureKey("print", "(Ljava/lang/String;)")
     *
     * // These would conflict (same key):
     * // func foo(a: int): int
     * // func foo(b: int): void        -> Same key "(I)", different return (not allowed)
     * }</pre>
     */
    private static class FunctionSignatureKey {
        private final String name;
        private final String paramDescriptor;

        public FunctionSignatureKey(String name, String paramDescriptor) {
            this.name = name;
            this.paramDescriptor = paramDescriptor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FunctionSignatureKey that = (FunctionSignatureKey) o;
            return Objects.equals(name, that.name) && Objects.equals(paramDescriptor, that.paramDescriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, paramDescriptor);
        }
    }

    /**
     * Represents a local variable in the JVM local variable table.
     *
     * <p>Each local variable maps to one or more slots in the JVM's LVT:
     *
     * <pre>{@code
     * // JVM Local Variable Table Entry:
     * // +-------+------+------+
     * // | Index | Type | Name |
     * // +-------+------+------+
     * // |   0   | int  | x    |  <- LocalVariable(index=0, type=INT, name="x")
     * // |   1   |double| y    |  <- LocalVariable(index=1, type=DOUBLE, name="y")
     * // |   3   | ref  | s    |  <- LocalVariable(index=3, type=STRING, name="s")
     * // +-------+------+------+
     * //         Note: y uses slots 1 and 2 (doubles use 2 slots)
     * }</pre>
     *
     * <h3>Load/Store Opcodes by Type:</h3>
     * <table>
     *   <tr><th>Type</th><th>Load</th><th>Store</th></tr>
     *   <tr><td>int/boolean</td><td>ILOAD index</td><td>ISTORE index</td></tr>
     *   <tr><td>double</td><td>DLOAD index</td><td>DSTORE index</td></tr>
     *   <tr><td>reference</td><td>ALOAD index</td><td>ASTORE index</td></tr>
     * </table>
     */
    public static class LocalVariable {
        private final int index;
        private final DataType type;
        private final String name;

        public LocalVariable(int index, DataType type, String name) {
            this.index = index;
            this.type = type;
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public DataType getType() {
            return type;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Loop labels for break/continue support in JVM bytecode.
     *
     * <p>JVM has no native loop construct—loops are implemented with
     * conditional jumps and labels. This class pairs the labels needed:
     *
     * <pre>{@code
     * // while (x < 10) { x++; }
     * //
     * // Generated bytecode with labels:
     * // startLabel:              <- continue jumps here
     * //   ILOAD x
     * //   BIPUSH 10
     * //   IF_ICMPGE endLabel     // if x >= 10, exit
     * //   IINC x, 1
     * //   GOTO startLabel
     * // endLabel:                <- break jumps here
     *
     * // With break:
     * // while (true) {
     * //   if (done) break;       // GOTO endLabel
     * //   ...
     * // }
     *
     * // With continue:
     * // while (x < 10) {
     * //   if (skip) continue;    // GOTO startLabel
     * //   process(x);
     * //   x++;
     * // }
     * }</pre>
     */
    public static class LoopLabels {
        private final Label startLabel;
        private final Label endLabel;

        public LoopLabels(Label startLabel, Label endLabel) {
            this.startLabel = startLabel;
            this.endLabel = endLabel;
        }

        public Label getStartLabel() {
            return startLabel;
        }

        public Label getEndLabel() {
            return endLabel;
        }
    }

    /**
     * Local variable table mapping variable names to JVM slots.
     * Corresponds to the JVM's LocalVariableTable attribute.
     */
    private final Map<String, LocalVariable> locals = new HashMap<>();

    /**
     * Next available local variable slot index.
     * <ul>
     *   <li>Instance methods: starts at 1 (slot 0 = 'this')</li>
     *   <li>Static methods: starts at 0</li>
     *   <li>Increments by 1 for int/boolean/reference, 2 for double/long</li>
     * </ul>
     */
    private int nextLocalIndex = 0;

    /**
     * Stack of loop labels for nested break/continue.
     * Top of stack is the innermost loop. Break/continue operate on
     * the innermost enclosing loop.
     */
    private final Deque<LoopLabels> loopStack = new ArrayDeque<>();

    /**
     * Type registry supporting FQN-keyed storage with simple-name alias resolution.
     * Allows classes that share a simple name across packages to coexist.
     */
    private final NamespacedTypeTable typeTable = new NamespacedTypeTable();

    /**
     * JVM internal class name being compiled (e.g., "MyProgram").
     * Used in INVOKESTATIC instructions for self-referential calls.
     */
    private String className;

    /**
     * Parent context for nested scopes (blocks, loops).
     * Variable lookup traverses this chain until found.
     * Null for root context.
     */
    private CompileContext parent;

    /**
     * Registry of auxiliary class bytecode (structs, types).
     * Key: class name, Value: compiled bytecode.
     * All registered classes are loaded together via MultiClassLoader.
     */
    private final Map<String, byte[]> generatedClasses = new HashMap<>();

    /**
     * Function registry for O(1) exact signature lookup.
     * Enables forward references and overload resolution.
     */
    private final Map<FunctionSignatureKey, FunctionSignature> functionRegistry = new HashMap<>();

    /**
     * Secondary index: all overloads grouped by function name.
     * Used for compatible signature search when exact match fails.
     */
    private final Map<String, Set<FunctionSignature>> functionsByName = new HashMap<>();

    /**
     * Return type of the function currently being compiled.
     * Used by ReturnCompiler to select correct return opcode.
     * Null when compiling main method body.
     */
    private DataType currentFunctionReturnType;

    public CompileContext() {
        this(null);
    }

    public CompileContext(CompileContext parent) {
        this.parent = parent;
        if (parent != null) {
            this.nextLocalIndex = parent.nextLocalIndex;
            this.className = parent.className;
        }
    }

    /**
     * Allocate a new local variable slot in the JVM local variable table.
     *
     * <p>Assigns the next available slot index and advances the counter.
     * Double/long types consume 2 slots per JVM specification.
     *
     * <h3>Example Allocation Sequence:</h3>
     * <pre>{@code
     * // var a: int;        allocateLocal("a", INT)     -> index 0, next = 1
     * // var b: double;     allocateLocal("b", DOUBLE)  -> index 1, next = 3
     * // var c: int;        allocateLocal("c", INT)     -> index 3, next = 4
     * // var d: String;     allocateLocal("d", STRING)  -> index 4, next = 5
     * }</pre>
     *
     * <h3>Bytecode Usage:</h3>
     * <pre>{@code
     * // After allocation, variable is accessed via:
     * // ILOAD index   (for int/boolean)
     * // DLOAD index   (for double)
     * // ALOAD index   (for references)
     * }</pre>
     *
     * @param name the variable name (for lookup)
     * @param type the variable type (determines slot size)
     * @return the allocated local variable index (slot number)
     */
    public int allocateLocal(String name, DataType type) {
        int index = nextLocalIndex;
        int slotSize = TypeMapper.getSlotSize(type);
        nextLocalIndex += slotSize;  // 2 for double, 1 for others
        locals.put(name, new LocalVariable(index, type, name));
        return index;
    }

    /**
     * Get a local variable by name, searching parent scopes if necessary.
     *
     * <p>Lookup traverses the scope chain (child → parent → ... → root)
     * until the variable is found or the chain is exhausted.
     *
     * <h3>Scope Chain Example:</h3>
     * <pre>{@code
     * // func example(): void {
     * //     var x: int = 1;           // Root scope: x
     * //     {
     * //         var y: int = 2;       // Child scope: y
     * //         x = x + y;            // x found in parent
     * //     }
     * // }
     *
     * // childContext.getLocal("y") -> found in childContext.locals
     * // childContext.getLocal("x") -> not in child, found in parent.locals
     * // childContext.getLocal("z") -> not found anywhere, returns null
     * }</pre>
     *
     * @param name the variable name
     * @return the LocalVariable with slot index, or null if not found
     */
    public LocalVariable getLocal(String name) {
        LocalVariable local = locals.get(name);
        if (local == null && parent != null) {
            return parent.getLocal(name);
        }
        return local;
    }

    /**
     * Check if a variable exists in the current scope.
     *
     * @param name the variable name
     * @return true if the variable exists
     */
    public boolean hasLocal(String name) {
        return getLocal(name) != null;
    }

    /**
     * Push loop labels onto the stack when entering a loop.
     *
     * <p>Called by WhileCompiler at loop start. The labels are used by
     * BreakCompiler and ContinueCompiler to generate correct jump targets.
     *
     * <h3>Generated Pattern:</h3>
     * <pre>{@code
     * // WhileCompiler:
     * Label startLabel = new Label();
     * Label endLabel = new Label();
     * ctx.pushLoop(startLabel, endLabel);
     *
     * mv.visitLabel(startLabel);      // <- continue target
     * [condition code]
     * IFEQ endLabel
     * [body code]                     // may contain break/continue
     * GOTO startLabel
     * mv.visitLabel(endLabel);        // <- break target
     *
     * ctx.popLoop();
     * }</pre>
     *
     * @param startLabel the loop start label (for continue)
     * @param endLabel   the loop end label (for break)
     */
    public void pushLoop(Label startLabel, Label endLabel) {
        loopStack.push(new LoopLabels(startLabel, endLabel));
    }

    /**
     * Pop loop labels when exiting a loop.
     *
     * <p>Called by WhileCompiler after generating the loop body.
     * Must be paired with pushLoop().
     */
    public void popLoop() {
        loopStack.pop();
    }

    /**
     * Get the innermost loop's labels for break/continue generation.
     *
     * <p>Used by BreakCompiler and ContinueCompiler to get the jump target:
     * <ul>
     *   <li>break: {@code GOTO currentLoop().getEndLabel()}</li>
     *   <li>continue: {@code GOTO currentLoop().getStartLabel()}</li>
     * </ul>
     *
     * <p>Searches parent contexts for nested scopes without their own loops.
     *
     * @return the innermost loop's labels, or null if not inside any loop
     */
    public LoopLabels currentLoop() {
        if (loopStack.isEmpty()) {
            return parent != null ? parent.currentLoop() : null;
        }
        return loopStack.peek();
    }

    /**
     * Check if currently inside a loop.
     *
     * @return true if inside a loop
     */
    public boolean isInLoop() {
        return currentLoop() != null;
    }

    /**
     * Register a user-defined type for compile-time lookups.
     *
     * <p>Called by StructDeclarationCompiler and TypeDeclarationCompiler
     * when processing type definitions. Enables subsequent code to:
     * <ul>
     *   <li>Instantiate the type: {@code Point(1, 2)}</li>
     *   <li>Access fields: {@code p.x}</li>
     *   <li>Call methods: {@code p.distance()}</li>
     * </ul>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * // struct Point { x: int, y: int }
     * StructType pointType = new StructType("Point", fields);
     * ctx.addDataType(pointType);
     *
     * // Later: var p: Point = Point(1, 2);
     * // StructInstantiationCompiler calls ctx.getDataType("Point")
     * // to build constructor descriptor "(II)V"
     * }</pre>
     *
     * @param type the DataType to register (StructType or ExternalClassType)
     */
    public void addDataType(DataType type) {
        typeTable.register(type);
    }

    /**
     * Get a data type by simple name, searching this scope then parent scopes.
     *
     * @throws CompileException if the simple name is ambiguous in this scope
     */
    public DataType getDataType(String name) {
        try {
            DataType type = typeTable.getBySimpleName(name);
            if (type == null && parent != null) {
                return parent.getDataType(name);
            }
            return type;
        } catch (AmbiguousTypeException e) {
            throw new CompileException(
                "Type '" + name + "' is ambiguous: " + e.getCandidates()
                + ". Qualify the type with its fully-qualified class name.");
        }
    }

    /**
     * Get a data type by its fully-qualified name (e.g. "java.util.Date").
     * Used internally during stub registration to avoid ambiguity from simple-name lookup.
     */
    public DataType getDataTypeByFqn(String fqn) {
        DataType type = typeTable.getByFqn(fqn);
        if (type == null && parent != null) {
            return parent.getDataTypeByFqn(fqn);
        }
        return type;
    }

    /**
     * Get the total number of local variable slots used.
     *
     * <p>Required by ASM's {@code visitMaxs(maxStack, maxLocals)} to set
     * the Code attribute's max_locals field. The JVM uses this to allocate
     * the local variable array at method entry.
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * // After compiling method body:
     * mv.visitMaxs(computedMaxStack, ctx.getLocalCount());
     * // JVM allocates array of this size for local variables
     * }</pre>
     *
     * @return the next local index (equals total slots used)
     */
    public int getLocalCount() {
        return nextLocalIndex;
    }

    /**
     * Set the starting local variable index.
     *
     * <p>Used when entering a method to account for implicit parameters:
     * <ul>
     *   <li>Static methods: index starts at 0</li>
     *   <li>Instance methods: index starts at 1 (slot 0 = 'this')</li>
     *   <li>After parameters: index continues from last parameter slot + size</li>
     * </ul>
     *
     * <h3>Example for Instance Method:</h3>
     * <pre>{@code
     * // type Counter {
     * //   func add(amount: int): void { ... }
     * // }
     *
     * // Slot allocation:
     * // 0: this (implicit)
     * // 1: amount (parameter)
     * // 2+: local variables
     *
     * funcCtx.setNextLocalIndex(0);
     * funcCtx.allocateLocal("this", counterType);  // index 0
     * funcCtx.allocateLocal("amount", INT);        // index 1
     * // Body locals start at index 2
     * }</pre>
     *
     * @param index the starting slot index
     */
    public void setNextLocalIndex(int index) {
        this.nextLocalIndex = index;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public CompileContext getParent() {
        return parent;
    }

    /**
     * Create a child context for a nested scope (block, loop body).
     *
     * <p>The child inherits:
     * <ul>
     *   <li>Current local index (continues allocating from parent's position)</li>
     *   <li>Class name (for INVOKESTATIC calls)</li>
     *   <li>Access to parent's variables via getLocal() lookup chain</li>
     * </ul>
     *
     * <p>The child does NOT inherit (has its own copy/registry):
     * <ul>
     *   <li>Local variable map (new scope's declarations)</li>
     *   <li>Loop stack (though parent's loops are accessible via parent chain)</li>
     * </ul>
     *
     * <h3>Scope Semantics:</h3>
     * <pre>{@code
     * // {
     * //     var x: int = 1;    // Declared in block scope
     * // }
     * // x = 2;                 // Error: x not visible here
     *
     * // JVM doesn't enforce scope—variables remain in LVT.
     * // Scope is a compile-time concept enforced by context chain.
     * }</pre>
     *
     * @return a new child context linked to this parent
     */
    public CompileContext createChildContext() {
        return new CompileContext(this);
    }

    /**
     * Register a generated auxiliary class (struct/type) for loading.
     *
     * <p>Struct and type declarations compile to separate JVM classes.
     * This registry collects their bytecode for batch loading via
     * {@link com.elminster.jcp.compile.MultiClassLoader}.
     *
     * <h3>Class Loading Flow:</h3>
     * <pre>{@code
     * // 1. Compile struct declaration
     * // struct Point { x: int, y: int }
     * byte[] pointBytecode = structGenerator.generate(...);
     * ctx.addGeneratedClass("Point", pointBytecode);
     *
     * // 2. After compiling all code
     * Map<String, byte[]> allClasses = ctx.getGeneratedClasses();
     * // Contains: {"Point" -> bytecode, "Line" -> bytecode, ...}
     *
     * // 3. Load all classes together
     * MultiClassLoader loader = new MultiClassLoader(allClasses);
     * Class<?> mainClass = loader.loadClass("Program");
     * // Point, Line, etc. are also loadable from this loader
     * }</pre>
     *
     * <p>Always registers at root context to ensure all generated classes
     * are collected in one place.
     *
     * @param className the JVM class name (e.g., "Point")
     * @param bytecode  the compiled class file bytes
     */
    public void addGeneratedClass(String className, byte[] bytecode) {
        if (parent != null) {
            // Always register at root context
            parent.addGeneratedClass(className, bytecode);
        } else {
            generatedClasses.put(className, bytecode);
        }
    }

    /**
     * Get all generated classes.
     *
     * @return map of class names to bytecode
     */
    public Map<String, byte[]> getGeneratedClasses() {
        if (parent != null) {
            return parent.getGeneratedClasses();
        }
        return generatedClasses;
    }

    /**
     * Register a function signature for forward reference and overload resolution.
     *
     * <p>Called during Pass 1 of two-pass compilation. Enables:
     * <ul>
     *   <li>Forward references: calling functions declared later in source</li>
     *   <li>Mutual recursion: func A calls B, func B calls A</li>
     *   <li>Overload resolution: multiple functions with same name, different params</li>
     * </ul>
     *
     * <h3>Two-Pass Compilation:</h3>
     * <pre>{@code
     * // Pass 1: Collect all signatures
     * // func foo(x: int): int { return bar(x); }
     * ctx.registerFunction("foo", [x:int], INT);
     * // func bar(y: int): int { return y * 2; }
     * ctx.registerFunction("bar", [y:int], INT);
     *
     * // Pass 2: Generate bytecode
     * // foo() can emit INVOKESTATIC bar(I)I because bar is registered
     * }</pre>
     *
     * <h3>Overloading:</h3>
     * <pre>{@code
     * // func print(x: int): void
     * ctx.registerFunction("print", [x:int], VOID);
     * // func print(s: String): void
     * ctx.registerFunction("print", [s:String], VOID);
     *
     * // lookupFunction("print", [INT]) -> first signature
     * // lookupFunction("print", [STRING]) -> second signature
     * }</pre>
     *
     * @param name       the function name
     * @param params     the parameter definitions (types and names)
     * @param returnType the return type
     */
    public void registerFunction(String name, ParameterDef[] params, DataType returnType) {
        if (parent != null) {
            parent.registerFunction(name, params, returnType);
            return;
        }
        FunctionSignature sig = new FunctionSignature(name, params, returnType);
        String paramDescriptor = buildParamDescriptor(params);
        FunctionSignatureKey key = new FunctionSignatureKey(name, paramDescriptor);
        functionRegistry.put(key, sig);
        functionsByName.computeIfAbsent(name, k -> new HashSet<>()).add(sig);
    }

    /**
     * Lookup a function by name and argument types for INVOKESTATIC generation.
     *
     * <p>Resolution strategy:
     * <ol>
     *   <li>Exact match: argument types match parameter types exactly (O(1))</li>
     *   <li>Compatible match: argument types castable to parameter types</li>
     * </ol>
     *
     * <h3>Exact Match:</h3>
     * <pre>{@code
     * // func add(a: int, b: int): int
     * lookupFunction("add", [INT, INT])
     * // -> Exact match via functionRegistry.get(key)
     * }</pre>
     *
     * <h3>Compatible Match (Type Coercion):</h3>
     * <pre>{@code
     * // func process(x: double): void
     * lookupFunction("process", [INT])
     * // -> No exact match for (I)
     * // -> Fallback: INT.isCastableTo(DOUBLE) = true
     * // -> Returns process(D)V signature
     * // -> Caller emits I2D before INVOKESTATIC
     * }</pre>
     *
     * <h3>Generated Bytecode:</h3>
     * <pre>{@code
     * // add(1, 2)
     * FunctionSignature sig = ctx.lookupFunction("add", [INT, INT]);
     * // sig.getDescriptor() = "(II)I"
     *
     * ICONST_1
     * ICONST_2
     * INVOKESTATIC Program.add(II)I
     * }</pre>
     *
     * @param name     the function name
     * @param argTypes the argument types at call site
     * @return the matching FunctionSignature, or null if not found
     */
    public FunctionSignature lookupFunction(String name, DataType[] argTypes) {
        if (parent != null) {
            return parent.lookupFunction(name, argTypes);
        }

        // Try exact match first (O(1))
        String paramDescriptor = buildParamDescriptor(argTypes);
        FunctionSignatureKey key = new FunctionSignatureKey(name, paramDescriptor);
        FunctionSignature exact = functionRegistry.get(key);
        if (exact != null) {
            return exact;
        }

        // Fallback: find compatible signature with type coercion
        Set<FunctionSignature> candidates = functionsByName.get(name);
        if (candidates != null) {
            for (FunctionSignature sig : candidates) {
                if (isCompatible(sig.getParameters(), argTypes)) {
                    return sig;
                }
            }
        }
        return null;
    }

    /**
     * Check if argument types are compatible with parameter types.
     */
    private boolean isCompatible(ParameterDef[] params, DataType[] argTypes) {
        if (params == null && argTypes == null) return true;
        if (params == null || argTypes == null) return false;
        if (params.length != argTypes.length) return false;

        for (int i = 0; i < params.length; i++) {
            DataType paramType = params[i].getDataType();
            DataType argType = argTypes[i];
            // Handle null argType (unknown type at compile time)
            if (argType == null) {
                continue;  // Assume compatible, runtime will verify
            }
            // Check type compatibility (hierarchy + widening)
            if (!argType.isCompatibleWith(paramType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Build parameter descriptor from parameter definitions.
     */
    private String buildParamDescriptor(ParameterDef[] params) {
        StringBuilder sb = new StringBuilder("(");
        if (params != null) {
            for (ParameterDef param : params) {
                sb.append(TypeMapper.toDescriptor(param.getDataType()));
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Build parameter descriptor from argument types.
     */
    private String buildParamDescriptor(DataType[] argTypes) {
        StringBuilder sb = new StringBuilder("(");
        if (argTypes != null) {
            for (DataType argType : argTypes) {
                sb.append(TypeMapper.toDescriptor(argType));
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Get the return type of the function currently being compiled.
     *
     * <p>Used by ReturnCompiler to select the correct return opcode:
     * <ul>
     *   <li>VOID → RETURN</li>
     *   <li>INT/BOOLEAN → IRETURN</li>
     *   <li>DOUBLE → DRETURN</li>
     *   <li>Reference → ARETURN</li>
     * </ul>
     *
     * <p>Also used to validate return statements:
     * <ul>
     *   <li>Void function with return value → error</li>
     *   <li>Non-void function without return value → error</li>
     * </ul>
     *
     * @return the function's declared return type, or null if in main method
     */
    public DataType getCurrentFunctionReturnType() {
        if (currentFunctionReturnType != null) {
            return currentFunctionReturnType;
        }
        if (parent != null) {
            return parent.getCurrentFunctionReturnType();
        }
        return null;
    }

    /**
     * Set the return type for the function being compiled.
     *
     * <p>Called by BytecodeGenerator when entering a function body:
     *
     * <pre>{@code
     * // Compiling: func factorial(n: int): int { ... }
     * CompileContext funcCtx = new CompileContext();
     * funcCtx.setCurrentFunctionReturnType(INT);
     * // ReturnCompiler will emit IRETURN
     * }</pre>
     *
     * @param type the function's declared return type
     */
    public void setCurrentFunctionReturnType(DataType type) {
        this.currentFunctionReturnType = type;
    }
}
