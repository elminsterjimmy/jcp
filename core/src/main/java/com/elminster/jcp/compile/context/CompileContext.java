package com.elminster.jcp.compile.context;

import com.elminster.jcp.eval.data.DataType;
import org.objectweb.asm.Label;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Compilation context that tracks local variables, types, and control flow labels.
 * Similar to {@link com.elminster.jcp.eval.context.EvalContext} but for compilation.
 */
public class CompileContext {

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
        int index = nextLocalIndex++;
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
}
