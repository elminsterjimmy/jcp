package com.elminster.jcp.eval.context;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.collection.FastStack;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.excpetion.AlreadyDeclaredException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DefaultEvalContext implements EvalContext {

    private Map<String, Data> variables = new HashMap<>();
    private Map<String, Function> functions = new HashMap<>();
    private Map<String, DataType> dataTypes = new HashMap<>();
    private LoopContext loopContext;
    private FastStack<EvalContext> contextStack = new FastStack<>();

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
        String name = dataType.getName();
        if (dataTypes.containsKey(name)) {
            AlreadyDeclaredException.throwDataTypeAlreadyDeclaredException(Identifier.fromName(name));
        }
        dataTypes.put(name, dataType);
    }

    @Override
    public DataType getDataType(String name) {
        String convertedName = convertSystemDataTypeName(name);
        return dataTypes.get(convertedName);
    }

    protected String convertSystemDataTypeName(String name) {
        if ("Object".equals(name)) {
            return DataType.SystemDataType.ANY.getName();
        } else if ("int".equals(name)) {
            return DataType.SystemDataType.INT.getName();
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
    public String toString() {
        return "DefaultEvalContext{" +
                "variables=" + variables +
                ", functions=" + functions +
                ", dataTypes=" + dataTypes +
                ", loopContext=" + loopContext +
                ", contextStack=" + contextStack +
                '}';
    }
}
