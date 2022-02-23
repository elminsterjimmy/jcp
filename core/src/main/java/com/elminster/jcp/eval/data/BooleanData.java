package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;

public class BooleanData extends AnyData<Boolean> {

  public static BooleanData BOOLEAN_TRUE = new BooleanData(Boolean.TRUE, true);
  public static BooleanData BOOLEAN_FALSE = new BooleanData(Boolean.FALSE, true);

  public BooleanData(Boolean data) {
    super(data);
  }

  public BooleanData(Identifier identifier, Boolean data, boolean isConst) {
    super(identifier, data, isConst);
  }

  public BooleanData(Boolean data, boolean isConst) {
    super(data, isConst);
  }

  public static Data newBoolean(Boolean bool) {
    return new BooleanData(bool);
  }

  @Override
  public DataType getDataType() {
    return DataType.SystemDataType.BOOLEAN;
  }
}
