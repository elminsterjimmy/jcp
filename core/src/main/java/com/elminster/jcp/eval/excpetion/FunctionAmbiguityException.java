package com.elminster.jcp.eval.excpetion;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.util.FunctionUtils;

public class FunctionAmbiguityException  extends EvaluationException {

    private static final String MESSAGE_PATTERN = "ambiguous function call: %s";

    private final Identifier identifier;
    private final DataType[] dataTypes;

    public FunctionAmbiguityException(Identifier identifier, DataType... dataTypes) {
        this.identifier = identifier;
        this.dataTypes = dataTypes;
    }

    @Override
    public String getMessage() {
        return String.format(MESSAGE_PATTERN, FunctionUtils.functionToString(identifier, dataTypes));
    }
}
