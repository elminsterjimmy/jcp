package com.elminster.jcp.eval.data;

import java.util.Collection;

/**
 * Stateless utility for finding the JCP DataType that can adapt to a given Java type.
 * Priority: SystemDataType first (skip ANY), then context-registered types.
 */
public final class TypeCompatibilityUtil {

    private TypeCompatibilityUtil() {
    }

    /**
     * Find the JCP DataType that can adapt to a given Java type.
     * Checks SystemDataType constants only (framework-owned, highest priority).
     * Skips ANY — returning ANY is the same as "no match".
     */
    public static DataType findJcpTypeForJava(Class<?> javaType) {
        for (DataType.SystemDataType sdt : DataType.SystemDataType.values()) {
            if (sdt == DataType.SystemDataType.ANY) {
                continue;
            }
            if (sdt.isAdaptableTo(javaType)) {
                return sdt;
            }
        }
        return null;
    }

    /**
     * Find the JCP DataType that can adapt to a given Java type,
     * also checking context-registered types.
     * Priority: SystemDataType first, then context-registered types.
     */
    public static DataType findJcpTypeForJava(Class<?> javaType,
                                               Collection<DataType> registeredTypes) {
        DataType system = findJcpTypeForJava(javaType);
        if (system != null) {
            return system;
        }
        for (DataType dt : registeredTypes) {
            if (dt instanceof DataType.SystemDataType) {
                continue;
            }
            if (dt.isAdaptableTo(javaType)) {
                return dt;
            }
        }
        return null;
    }
}
