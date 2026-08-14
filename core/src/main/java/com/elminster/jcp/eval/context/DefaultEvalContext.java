package com.elminster.jcp.eval.context;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.collection.FastStack;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.ExternalClassType;
import com.elminster.jcp.eval.data.NamespacedTypeTable;
import com.elminster.jcp.eval.excpetion.AlreadyDeclaredException;
import com.elminster.jcp.eval.excpetion.AmbiguousTypeException;
import com.elminster.jcp.eval.excpetion.EvaluationException;
import com.elminster.jcp.exception.CallStack;
import com.elminster.jcp.exception.StackFrame;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

public class DefaultEvalContext implements EvalContext {

    private Map<String, Data> variables = new HashMap<>();
    private Map<String, Function> functions = new HashMap<>();
    private final NamespacedTypeTable dataTypes = new NamespacedTypeTable();
    private LoopContext loopContext;
    private FastStack<EvalContext> contextStack = new FastStack<>();
    private volatile boolean isReturn = false;
    private StackFrame stackFrame;

    public DefaultEvalContext() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Data> getVariables() {
        return variables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Data getVariable(String name) {
        return variables.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addVariable(Data variable) {
        EvalContext currentContext = getContextStack().peek();
        Identifier identifier = variable.getIdentifier();
        String id = identifier.getId();
        Map<String, Data> currentVariables = currentContext.getVariables();
        if (currentVariables.containsKey(id)) {
            AlreadyDeclaredException.throwVariableAlreadyDeclaredException(identifier);
        }
        currentVariables.put(id, variable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Function> getFunctions() {
        return this.functions;
    }

    @Override
    public void addFunction(Function function) {
        EvalContext currentContext = getContextStack().peek();
        String functionFullName = function.getFullName();
        Map<String, Function> currentFunctions = currentContext.getFunctions();
        if (currentFunctions.containsKey(functionFullName)) {
            AlreadyDeclaredException.throwFunctionAlreadyDeclaredException(function);
        }
        currentFunctions.put(functionFullName, function);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Function getFunction(String name) {
        return functions.get(name);
    }

    /**
     * NOT support scope of data type.
     * @param dataType
     */
    @Override
    public void addDataType(DataType dataType) {
        boolean added = dataTypes.register(dataType);
        // ExternalClassType stubs are registered idempotently — silent no-op on duplicate.
        // User-declared types (struct/system) must not be declared twice.
        if (!added && !(dataType instanceof ExternalClassType)) {
            AlreadyDeclaredException.throwDataTypeAlreadyDeclaredException(
                new IdentifierExpression(dataType.getName()));
        }
    }

    @Override
    public DataType getDataType(String name) {
        String convertedName = convertSystemDataTypeName(name);
        try {
            return dataTypes.getBySimpleName(convertedName);
        } catch (AmbiguousTypeException e) {
            throw new EvaluationException(
                "Type '" + name + "' is ambiguous: " + e.getCandidates()
                + ". Qualify the type with its fully-qualified class name.");
        }
    }

    @Override
    public DataType getDataTypeByFqn(String fqn) {
        return dataTypes.getByFqn(fqn);
    }

    protected String convertSystemDataTypeName(String name) {
        if ("Object".equals(name)) {
            return DataType.SystemDataType.ANY.getName();
        } else if ("int".equals(name)) {
            return DataType.SystemDataType.INT.getName();
        } else if ("double".equals(name)) {
            return DataType.SystemDataType.DOUBLE.getName();
        } else if ("boolean".equals(name)) {
            return DataType.SystemDataType.BOOLEAN.getName();
        } else if ("void".equals(name)) {
            return DataType.SystemDataType.VOID.getName();
        }
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoopContext getLoopContext() {
        return loopContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLoopContext(LoopContext loopContext) {
        this.loopContext = loopContext;
    }

    @Override
    public FastStack<EvalContext> getContextStack() {
        return contextStack;
    }

    @Override
    public boolean isReturn() {
        return isReturn;
    }

    @Override
    public void setReturn(boolean isReturn) {
        this.isReturn = isReturn;
    }

    @Override
    public StackFrame getStackFrame() {
        return stackFrame;
    }

    @Override
    public void setStackFrame(StackFrame stackFrame) {
        this.stackFrame = stackFrame;
    }

    @Override
    public CallStack getCallStack() {
        throw new UnsupportedOperationException("getCallStack() must be called on RootEvalContext");
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
