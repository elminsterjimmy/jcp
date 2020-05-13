package com.elminster.jcp.ast.data;

public class BooleanFlowData extends AnyFlowData<Boolean> {

  public static BooleanFlowData BOOLEAN_TRUE = new BooleanFlowData(Boolean.TRUE, true);
  public static BooleanFlowData BOOLEAN_FALSE = new BooleanFlowData(Boolean.FALSE, true);

  public BooleanFlowData(Boolean data) {
    super(data);
  }

  public BooleanFlowData(String name, Boolean data, boolean isConst) {
    super(name, data, isConst);
  }

  public BooleanFlowData(Boolean data, boolean isConst) {
    super(data, isConst);
  }

  @Override
  public DataType getDataType() {
    return DataType.BOOLEAN;
  }
}
