package com.elminster.jcp.util;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.ParameterDef;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;

import java.util.Arrays;
import java.util.StringJoiner;

public class FunctionUtils {

    private static final String FUNCTION_FULLNAME_PARAMETER_SPLITTER = "@";
    private static final String FUNCTION_FULLNAME_FUNCTION_NAME_SPLITTER = "#";

    private static final String FUNCTION_TOSTRING_PARAMETER_SPLITTER = ",";
    private static final String FUNCTION_TOSTRING_PARAMETER_STARTER = "(";
    private static final String FUNCTION_TOSTRING_PARAMETER_ENDER = ")";

    public static String generateFunctionFullName(Identifier identifier, ParameterDef[] parameterDefs) {
        String functionName = identifier.getId();
        StringJoiner parameterStringList = new StringJoiner(FUNCTION_FULLNAME_PARAMETER_SPLITTER);
        Arrays.stream(parameterDefs).forEach(parameterDef -> parameterStringList.add(parameterDef.getDataType().getName()));
        return functionName.concat(FUNCTION_FULLNAME_FUNCTION_NAME_SPLITTER).concat(parameterStringList.toString());
    }

    public static String generateFunctionFullName(Identifier identifier, DataType[] parameterDataTypes) {
        String functionName = identifier.getId();
        StringJoiner parameterStringList = new StringJoiner(FUNCTION_FULLNAME_PARAMETER_SPLITTER);
        Arrays.stream(parameterDataTypes).forEach(dataType -> parameterStringList.add(dataType.getName()));
        return functionName.concat(FUNCTION_FULLNAME_FUNCTION_NAME_SPLITTER).concat(parameterStringList.toString());
    }

    public static String generateFunctionFullName(Identifier identifier, Data[] parameterData) {
        String functionName = identifier.getId();
        StringJoiner parameterStringList = new StringJoiner(FUNCTION_FULLNAME_PARAMETER_SPLITTER);
        Arrays.stream(parameterData).forEach(data -> parameterStringList.add(data.getDataType().getName()));
        return functionName.concat(FUNCTION_FULLNAME_FUNCTION_NAME_SPLITTER).concat(parameterStringList.toString());
    }

    public static String functionToString(Identifier identifier, DataType[] parameterDataTypes) {
        String functionName = identifier.getId();
        StringJoiner parameterStringList = new StringJoiner(FUNCTION_TOSTRING_PARAMETER_SPLITTER);
        Arrays.stream(parameterDataTypes).forEach(dataType -> parameterStringList.add(dataType.getName()));
        return functionName
                .concat(FUNCTION_TOSTRING_PARAMETER_STARTER)
                .concat(parameterStringList.toString())
                .concat(FUNCTION_TOSTRING_PARAMETER_ENDER);
    }
}
