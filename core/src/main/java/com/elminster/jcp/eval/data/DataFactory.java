package com.elminster.jcp.ast.data;

public class DataFactory {
  public static final DataFactory INSTANCE = new DataFactory();

  private DataFactory() {}

  public Data createSystemDataVariable(String id, DataType dataType) {
    return createSystemData(id, dataType, false);
  }

  public Data createSystemDataConst(String id, DataType dataType) {
    return createSystemData(id, dataType, true);
  }

  private Data createSystemData(String id, DataType dataType, boolean isConst) {
    Data data = null;
    if (dataType instanceof DataType.SystemDataType) {
      switch((DataType.SystemDataType)dataType) {
        case ANY:
          data = new AnyData(id, null, isConst);
          break;
        case BOOLEAN:
          data = new BooleanData(id, false, isConst);
          break;
        case INT:
          data = new IntegerData(id, 0, isConst);
          break;
        case STRING:
          data = new StringData(id, null, isConst);
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
    }
    return data;
  }
}
