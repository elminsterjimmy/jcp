package com.elminster.jcp.compile.context;

import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.compile.util.TypeMapper;
import com.elminster.jcp.eval.data.DataType;
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
 * Similar to {@link com.elminster.jcp.eval.context.EvalContext} but for compilation.
 */
public class CompileContext {

    /**
     * Represents a function signature for overload resolution and bytecode generation.
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
     * Loop labels for break/continue support.
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

    // Local variable table
    private final Map<String, LocalVariable> locals = new HashMap<>();

    // Next available local variable index (0 is 'this' or args for static methods)
    private int nextLocalIndex = 0;

    // Loop label stack for break/continue
    private final Deque<LoopLabels> loopStack = new ArrayDeque<>();

    // Type information
    private final Map<String, DataType> typeTable = new HashMap<>();

    // Class name being compiled
    private String className;

    // Parent context for nested scopes
    private CompileContext parent;

    // Generated classes (for structs and other auxiliary classes)
    private final Map<String, byte[]> generatedClasses = new HashMap<>();

    // Function registry for forward reference and overload resolution
    private final Map<FunctionSignatureKey, FunctionSignature> functionRegistry = new HashMap<>();
    private final Map<String, Set<FunctionSignature>> functionsByName = new HashMap<>();

    // Current function return type (for ReturnCompiler)
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
     * Allocate a new local variable slot.
     *
     * @param name the variable name
     * @param type the variable type
     * @return the allocated local variable index
     */
    public int allocateLocal(String name, DataType type) {
        int index = nextLocalIndex;
        int slotSize = TypeMapper.getSlotSize(type);
        nextLocalIndex += slotSize;  // 2 for double, 1 for others
        locals.put(name, new LocalVariable(index, type, name));
        return index;
    }

    /**
     * Get a local variable by name.
     *
     * @param name the variable name
     * @return the local variable, or null if not found
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
     * Push loop labels for break/continue support.
     *
     * @param startLabel the loop start label (for continue)
     * @param endLabel   the loop end label (for break)
     */
    public void pushLoop(Label startLabel, Label endLabel) {
        loopStack.push(new LoopLabels(startLabel, endLabel));
    }

    /**
     * Pop loop labels.
     */
    public void popLoop() {
        loopStack.pop();
    }

    /**
     * Get current loop labels.
     *
     * @return the current loop labels, or null if not in a loop
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
     * Register a data type.
     *
     * @param type the data type to register
     */
    public void addDataType(DataType type) {
        typeTable.put(type.getName(), type);
    }

    /**
     * Get a data type by name.
     *
     * @param name the type name
     * @return the data type, or null if not found
     */
    public DataType getDataType(String name) {
        DataType type = typeTable.get(name);
        if (type == null && parent != null) {
            return parent.getDataType(name);
        }
        return type;
    }

    /**
     * Get the current local variable count.
     *
     * @return the next local index (which is also the count)
     */
    public int getLocalCount() {
        return nextLocalIndex;
    }

    /**
     * Set the next local variable index.
     * Used when entering a method with parameters.
     *
     * @param index the next index to use
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
     * Create a child context for a nested scope.
     *
     * @return a new child context
     */
    public CompileContext createChildContext() {
        return new CompileContext(this);
    }

    /**
     * Register a generated class (e.g., a struct class).
     *
     * @param className the class name
     * @param bytecode  the class bytecode
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
     * @param name       the function name
     * @param params     the parameter definitions
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
     * Lookup a function by name and argument types.
     *
     * @param name     the function name
     * @param argTypes the argument types
     * @return the matching function signature, or null if not found
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
            if (!argType.isCastableTo(paramType)) {
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
     * Get the current function's return type.
     *
     * @return the return type, or null if not in a function
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
     * Set the current function's return type.
     *
     * @param type the return type
     */
    public void setCurrentFunctionReturnType(DataType type) {
        this.currentFunctionReturnType = type;
    }
}
