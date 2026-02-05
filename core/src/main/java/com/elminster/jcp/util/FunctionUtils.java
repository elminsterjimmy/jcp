package com.elminster.jcp.util;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;

import java.util.Arrays;
import java.util.StringJoiner;

/**
 * The function utils.
 *
 * Function naming patterns:
 *
 * Type methods:
 * - Logger.log            - base module type method (module omitted)
 * - Assertions.assertTrue - base module type method
 * - Math.sum              - user module type method (user treated like base)
 * - mymodule::Counter.get - other module type method
 *
 * Global functions:
 * - abs                   - base/user module global function (module and type omitted)
 * - mymodule::plus        - other module global function
 *
 * Full name with params: funcName#paramType1@paramType2
 *
 * @author jgu
 * @version 1.0
 */
public class FunctionUtils {

    private static final String FUNCTION_FULLNAME_PARAMETER_SPLITTER = "@";
    private static final String FUNCTION_FULLNAME_FUNCTION_NAME_SPLITTER = "#";

    private static final String FUNCTION_TOSTRING_PARAMETER_SPLITTER = ",";
    private static final String FUNCTION_TOSTRING_PARAMETER_STARTER = "(";
    private static final String FUNCTION_TOSTRING_PARAMETER_ENDER = ")";

    private static final String MODULE_SPLITTER = "::";

    /** Default module for user-defined functions/types */
    public static final String USER_MODULE = "user";

    /** Type name for global functions (not belonging to any type) */
    public static final String GLOBAL_TYPE = "global";

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

    public static String generateFunctionFullName(String moduleName, String dataTypeName, String functionName, Data[] parameterData) {
        StringJoiner parameterStringList = new StringJoiner(FUNCTION_FULLNAME_PARAMETER_SPLITTER);
        Arrays.stream(parameterData).forEach(data -> parameterStringList.add(data.getDataType().getName()));
        return getModuleFunctionName(moduleName, dataTypeName, functionName)
                .concat(FUNCTION_FULLNAME_FUNCTION_NAME_SPLITTER).concat(parameterStringList.toString());
    }

    /**
     * Get module function name without parameters.
     * Current pattern: [module::]type.func (for backward compatibility)
     *
     * @param moduleName Module name (e.g., "user", "base")
     * @param dataTypeName Type name (e.g., "Counter", "Logger") or "global" for global functions
     * @param functionName Function name (e.g., "getCount", "log")
     * @return Function name: type.func (or module::type.func for non-base modules)
     */
    public static String getModuleFunctionName(String moduleName, String dataTypeName, String functionName) {
        return (isBaseModuleOrEmptyModule(moduleName) ? "" : moduleName
                .concat(MODULE_SPLITTER))
                .concat(dataTypeName)
                .concat(".")
                .concat(functionName);
    }

    /**
     * Get module function name using the new consistent pattern.
     * New pattern: module::type::func
     *
     * This pattern is used for:
     * - user::global::sum    - user global function
     * - user::Counter::get   - user type method
     * - base::Logger::log    - base module type method
     *
     * @param moduleName Module name (e.g., "user", "base")
     * @param dataTypeName Type name or "global" for global functions
     * @param functionName Function name
     * @return Full qualified function name: module::type::func
     */
    public static String getFullyQualifiedFunctionName(String moduleName, String dataTypeName, String functionName) {
        StringBuilder sb = new StringBuilder();
        if (moduleName != null && !moduleName.isEmpty()) {
            sb.append(moduleName);
        } else {
            sb.append("base");
        }
        sb.append(MODULE_SPLITTER);
        sb.append(dataTypeName);
        sb.append(MODULE_SPLITTER);
        sb.append(functionName);
        return sb.toString();
    }

    /**
     * Generate function full name from module, type, function name, and parameter definitions.
     * Pattern: module::type::func#paramType1@paramType2
     */
    public static String generateFunctionFullName(String moduleName, String dataTypeName,
                                                   String functionName, ParameterDef[] parameterDefs) {
        StringJoiner parameterStringList = new StringJoiner(FUNCTION_FULLNAME_PARAMETER_SPLITTER);
        if (parameterDefs != null) {
            Arrays.stream(parameterDefs).forEach(parameterDef ->
                parameterStringList.add(parameterDef.getDataType().getName()));
        }
        return getModuleFunctionName(moduleName, dataTypeName, functionName)
                .concat(FUNCTION_FULLNAME_FUNCTION_NAME_SPLITTER).concat(parameterStringList.toString());
    }

    /**
     * Get global function name (for functions not belonging to any type).
     * Pattern: module::global::func
     */
    public static String getGlobalFunctionName(String moduleName, String functionName) {
        return getModuleFunctionName(moduleName, GLOBAL_TYPE, functionName);
    }

    /**
     * Generate full name for a global function with parameters.
     * Pattern: module::global::func#paramType1@paramType2
     */
    public static String generateGlobalFunctionFullName(String moduleName, String functionName,
                                                         ParameterDef[] parameterDefs) {
        return generateFunctionFullName(moduleName, GLOBAL_TYPE, functionName, parameterDefs);
    }

    /**
     * Generate full name for a global function with parameter data.
     * Pattern: module::global::func#paramType1@paramType2
     */
    public static String generateGlobalFunctionFullName(String moduleName, String functionName,
                                                         Data[] parameterData) {
        return generateFunctionFullName(moduleName, GLOBAL_TYPE, functionName, parameterData);
    }

    private static boolean isBaseModuleOrEmptyModule(String moduleName) {
        return "base".equals(moduleName) || "".equals(moduleName);
    }
}
