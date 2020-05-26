package com.elminster.jcp.eval.data;

public class DataFactory {
  public static final DataFactory INSTANCE = new DataFactory();

  private DataFactory() {}

  public Data createSystemDataVariable(String id, DataType dataType, Object value) {
    return createSystemData(id, dataType, value, false);
  }

  public Data createSystemDataConst(String id, DataType dataType, Object value) {
    return createSystemData(id, dataType, value,true);
  }

  private Data createSystemData(String id, DataType dataType, Object value, boolean isConst) {
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
      return new AnyData() {

        @Override
        public boolean isConst() {
          return isConst;
        }

        @Override
        public DataType getDataType() {
          return dataType;
        }
      };
    }
    return data;
  }

  public Data createSystemDataVariable(String id, DataType dataType) {
    return createSystemDataVariable(id, dataType, null);
  }
}
