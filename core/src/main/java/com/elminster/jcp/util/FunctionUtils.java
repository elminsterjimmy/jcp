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
 * Function key format (used as the function map key):
 *
 *   module::typeFqn#methodName@paramFqn1@paramFqn2
 *
 * - module   : module name (e.g. "base", "user", "mymodule")
 * - typeFqn  : fully-qualified type name — Java FQN for ExternalClassType
 *              (e.g. "java.util.Date"), simple name for user/system types
 *              (e.g. "Counter", "global")
 * - method   : method name (e.g. "toString", "get")
 * - paramFqn : fully-qualified param type (same convention as typeFqn)
 *
 * Separators chosen to be safe from Java FQNs and identifiers:
 *   "::"  module/type boundary
 *   "#"   type/method boundary
 *   "@"   param separator
 *
 * Examples:
 *   base::java.util.Date#toString@
 *   base::java.util.Date#compareTo@java.util.Date
 *   base::Strings#length@String
 *   user::Counter#get@
 *   user::global#sum@Integer@Integer
 *
 * @author jgu
 * @version 1.0
 */
public class FunctionUtils {

    static final String PARAM_SPLITTER = "@";
    static final String METHOD_SPLITTER = "#";
    static final String MODULE_SPLITTER = "::";

    private static final String FUNCTION_TOSTRING_PARAMETER_SPLITTER = ",";
    private static final String FUNCTION_TOSTRING_PARAMETER_STARTER = "(";
    private static final String FUNCTION_TOSTRING_PARAMETER_ENDER = ")";

    /** Default module for user-defined functions/types */
    public static final String USER_MODULE = "user";

    /** Default module for base/framework functions/types */
    public static final String BASE_MODULE = "base";

    /** Type name for global functions (not belonging to any type) */
    public static final String GLOBAL_TYPE = "global";

    // -----------------------------------------------------------------------
    // Key construction: module::typeFqn#method@paramFqn1@paramFqn2
    // -----------------------------------------------------------------------

    /**
     * Build the base function name (no params): module::typeFqn#method
     */
    public static String getModuleFunctionName(String moduleName, String typeFqn, String methodName) {
        return resolveModule(moduleName)
                .concat(MODULE_SPLITTER)
                .concat(typeFqn)
                .concat(METHOD_SPLITTER)
                .concat(methodName);
    }

    /**
     * Build the full function key from module, type FQN, method name, and param DataTypes.
     * Pattern: module::typeFqn#method@paramFqn1@paramFqn2
     */
    public static String generateFunctionFullName(String moduleName, String typeFqn,
                                                   String methodName, DataType[] paramTypes) {
        StringJoiner params = new StringJoiner(PARAM_SPLITTER);
        for (DataType pt : paramTypes) {
            params.add(pt.getFqn());
        }
        return getModuleFunctionName(moduleName, typeFqn, methodName)
                .concat(PARAM_SPLITTER).concat(params.toString());
    }

    /**
     * Build the full function key from module, type FQN, method name, and param Data values.
     */
    public static String generateFunctionFullName(String moduleName, String typeFqn,
                                                   String methodName, Data[] paramData) {
        StringJoiner params = new StringJoiner(PARAM_SPLITTER);
        for (Data d : paramData) {
            params.add(d.getDataType().getFqn());
        }
        return getModuleFunctionName(moduleName, typeFqn, methodName)
                .concat(PARAM_SPLITTER).concat(params.toString());
    }

    /**
     * Build the full function key from an Identifier and ParameterDef array.
     * Used by user-declared functions where the identifier already encodes module::type#method.
     */
    public static String generateFunctionFullName(Identifier identifier, ParameterDef[] parameterDefs) {
        StringJoiner params = new StringJoiner(PARAM_SPLITTER);
        for (ParameterDef pd : parameterDefs) {
            params.add(pd.getDataType().getFqn());
        }
        return identifier.getId().concat(PARAM_SPLITTER).concat(params.toString());
    }

    /**
     * Build the full function key from an Identifier and DataType array.
     */
    public static String generateFunctionFullName(Identifier identifier, DataType[] parameterDataTypes) {
        StringJoiner params = new StringJoiner(PARAM_SPLITTER);
        for (DataType dt : parameterDataTypes) {
            params.add(dt.getFqn());
        }
        return identifier.getId().concat(PARAM_SPLITTER).concat(params.toString());
    }

    /**
     * Build the full function key from an Identifier and Data array.
     */
    public static String generateFunctionFullName(Identifier identifier, Data[] parameterData) {
        StringJoiner params = new StringJoiner(PARAM_SPLITTER);
        for (Data d : parameterData) {
            params.add(d.getDataType().getFqn());
        }
        return identifier.getId().concat(PARAM_SPLITTER).concat(params.toString());
    }

    /**
     * Build the full function key for a global function with ParameterDef array.
     */
    public static String generateFunctionFullName(String moduleName, String typeFqn,
                                                   String methodName, ParameterDef[] parameterDefs) {
        StringJoiner params = new StringJoiner(PARAM_SPLITTER);
        if (parameterDefs != null) {
            for (ParameterDef pd : parameterDefs) {
                params.add(pd.getDataType().getFqn());
            }
        }
        return getModuleFunctionName(moduleName, typeFqn, methodName)
                .concat(PARAM_SPLITTER).concat(params.toString());
    }

    // -----------------------------------------------------------------------
    // Global function helpers
    // -----------------------------------------------------------------------

    public static String getGlobalFunctionName(String moduleName, String functionName) {
        return getModuleFunctionName(moduleName, GLOBAL_TYPE, functionName);
    }

    public static String generateGlobalFunctionFullName(String moduleName, String functionName,
                                                         ParameterDef[] parameterDefs) {
        return generateFunctionFullName(moduleName, GLOBAL_TYPE, functionName, parameterDefs);
    }

    public static String generateGlobalFunctionFullName(String moduleName, String functionName,
                                                         Data[] parameterData) {
        return generateFunctionFullName(moduleName, GLOBAL_TYPE, functionName, parameterData);
    }

    // -----------------------------------------------------------------------
    // Display / toString helpers (not used as map keys)
    // -----------------------------------------------------------------------

    public static String functionToString(Identifier identifier, DataType[] parameterDataTypes) {
        String functionName = identifier.getId();
        StringJoiner parameterStringList = new StringJoiner(FUNCTION_TOSTRING_PARAMETER_SPLITTER);
        Arrays.stream(parameterDataTypes).forEach(dataType -> parameterStringList.add(dataType.getName()));
        return functionName
                .concat(FUNCTION_TOSTRING_PARAMETER_STARTER)
                .concat(parameterStringList.toString())
                .concat(FUNCTION_TOSTRING_PARAMETER_ENDER);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static String resolveModule(String moduleName) {
        return (moduleName == null || moduleName.isEmpty()) ? BASE_MODULE : moduleName;
    }
}
