package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;

public class LongData extends AnyData<Long> {

  public static LongData constLong(Long data) {
    return new LongData(data, true);
  }

  public LongData(Long data) {
    super(data);
  }

  public LongData(Identifier identifier, Long data) {
    super(identifier, data);
  }

  public LongData(Long data, boolean isConst) {
    super(data, isConst);
  }

  public LongData(Identifier identifier, Long data, boolean isConst) {
    super(identifier, data, isConst);
  }

  @Override
  public DataType getDataType() {
    return DataType.SystemDataType.LONG;
  }
}
