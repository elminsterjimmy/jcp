package com.elminster.jcp.ast.data;

public class FlowDataFactory {
  public static final FlowDataFactory INSTANCE = new FlowDataFactory();

  private FlowDataFactory() {}

  public FlowData createFlowDataVariable(String id, DataType dataType) {
    return createFlowData(id, dataType, false);
  }

  public FlowData createFlowDataConst(String id, DataType dataType) {
    return createFlowData(id, dataType, true);
  }

  private FlowData createFlowData(String id, DataType dataType, boolean isConst) {
    FlowData data = null;
    switch(dataType) {
      case ANY:
        data = new AnyFlowData(id, null, isConst);
        break;
      case BOOLEAN:
        data = new BooleanFlowData(id, false, isConst);
        break;
      case INT:
        data = new IntegerFlowData(id, 0, isConst);
        break;
      case STRING:
        data = new StringFlowData(id, null, isConst);
        break;
      default:
        throw new IllegalStateException("Unknown data type [" + dataType + "]");
    }
    return data;
  }
}
