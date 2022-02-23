package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;
import org.apache.commons.lang3.ClassUtils;

public class DataFactory {
  public static final DataFactory INSTANCE = new DataFactory();

  private DataFactory() {}

  public Data createSystemDataVariable(Identifier id, DataType dataType, Object value) {
    return createSystemData(id, dataType, value, false);
  }

  public Data createSystemDataConst(Identifier id, DataType dataType, Object value) {
    return createSystemData(id, dataType, value,true);
  }

  public <T> Data createConstValue(T value) {
    if (null == value) {
      return new AnyData(null, true);
    }
    Class<?> clazz = value.getClass();
    if (ClassUtils.isPrimitiveOrWrapper(clazz)) {
      if (clazz == Integer.class || clazz == int.class) {
        return new IntegerData((int) value, true);
      } else if (clazz == Boolean.class || clazz == boolean.class) {
        return new BooleanData((boolean) value, true);
      } else if (clazz == String.class) {
        return new StringData((String) value, true);
      } else {
        return new AnyData(value, true);
      }
    } else {
      return new AnyData(value, true);
    }
  }

  private Data createSystemData(Identifier id, DataType dataType, Object value, boolean isConst) {
    Data data = null;
    if (dataType instanceof DataType.SystemDataType) {
      switch((DataType.SystemDataType)dataType) {
        case ANY:
          data = new AnyData(id, value, isConst);
          break;
        case BOOLEAN:
          data = new BooleanData(id, (Boolean) value, isConst);
          break;
        case INT:
          data = new IntegerData(id, (Integer) value, isConst);
          break;
        case STRING:
          data = new StringData(id, (String) value, isConst);
          break;
        case ANY_ARRAY:
          data = new ArrayData(DataType.SystemDataType.ANY, id, null, isConst);
          break;
        case BOOLEAN_ARRAY:
          data = new ArrayData(DataType.SystemDataType.BOOLEAN, id, null, isConst);
          break;
        case STRING_ARRAY:
          data = new ArrayData(DataType.SystemDataType.STRING, id, null, isConst);
          break;
        case INT_ARRAY:
          data = new ArrayData(DataType.SystemDataType.INT, id, null, isConst);
          break;
        default:
          throw new IllegalStateException("Unknown data type [" + dataType + "]");
      }
    } else {
      return new AnyData(null, isConst) {

        @Override
        public DataType getDataType() {
          return dataType;
        }
      };
    }
    return data;
  }

  public Data createSystemDataVariable(Identifier id, DataType dataType) {
    return createSystemDataVariable(id, dataType, null);
  }
}
