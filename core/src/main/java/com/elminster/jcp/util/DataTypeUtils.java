package com.elminster.jcp.util;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.eval.data.AbstractDataType;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.context.EvalContext;

/**
 * The DataType Utils.
 *
 * @author jgu
 * @version 1.0
 */
public class DataTypeUtils {

  public static DataType getDataType(String name, EvalContext ctx) {
    if (isArray(name)) {
      String baseTypeName = name.substring(0, name.length() - 2);
      DataType baseType = getDataType(baseTypeName, ctx);
      return new AbstractDataType() {
        @Override
        public String getName() {
          return baseType.getName() + "[]";
        }
      };
    } else {
      return ctx.getDataType(name);
    }
  }

  public static DataType getDataTypeAndCreateOnMissing(String name, EvalContext ctx) {
    DataType dt = ctx.getDataType(name);
    // data type not found
    if (null == dt) {
      if (isArray(name)) {
        String baseTypeName = name.substring(0, name.length() - 2);
        DataType baseType = getDataTypeAndCreateOnMissing(baseTypeName, ctx);
        dt = new AbstractDataType() {
          @Override
          public String getName() {
            return baseType.getName() + "[]";
          }
        };
      } else {
        dt = new AbstractDataType() {
          @Override
          public String getName() {
            return name;
          }
        };
      }
    }
    return dt;
  }

  private static boolean isArray(String name) {
    return name.endsWith("[]");
  }

  public static DataType getDataTypeAndCreateOnMissing(LiteralExpression literalExpression) {
    if (literalExpression instanceof StringLiteral) {
      return DataType.SystemDataType.STRING;
    }
    if (literalExpression instanceof BooleanLiteral) {
      return DataType.SystemDataType.BOOLEAN;
    }
    if (literalExpression instanceof IntLiteral) {
      return DataType.SystemDataType.INT;
    }
    return DataType.SystemDataType.ANY;
  }
}
