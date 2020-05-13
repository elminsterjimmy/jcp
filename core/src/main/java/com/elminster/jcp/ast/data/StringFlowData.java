package com.elminster.jcp.ast.data;

public class StringFlowData extends AnyFlowData<String> {

  public StringFlowData(String name, String data, boolean isConst) {
    super(name, data, isConst);
  }

  public StringFlowData(String data) {
    super(data);
  }

  public StringFlowData(String data, boolean isConst) {
    super(data, isConst);
  }

  public static FlowData newString(String string) {
    return new StringFlowData(string);
  }

  @Override
  public DataType getDataType() {
    return DataType.STRING;
  }
}
