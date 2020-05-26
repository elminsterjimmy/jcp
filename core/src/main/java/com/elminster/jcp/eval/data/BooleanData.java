package com.elminster.jcp.eval.data;

public class BooleanData extends AnyData<Boolean> {

  public static BooleanData BOOLEAN_TRUE = new BooleanData(Boolean.TRUE, true);
  public static BooleanData BOOLEAN_FALSE = new BooleanData(Boolean.FALSE, true);

  public BooleanData(Boolean data) {
    super(data);
  }

  public BooleanData(String name, Boolean data, boolean isConst) {
    super(name, data, isConst);
  }

  public BooleanData(Boolean data, boolean isConst) {
    super(data, isConst);
  }

  @Override
  public DataType getDataType() {
    return DataType.SystemDataType.BOOLEAN;
  }
}
