package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;

public class AnyData<T extends Object> extends DataBase<T> {

  protected final DataType dataType;

  public static AnyData EMPTY = new AnyData(null, true);

  public AnyData() {
    super(null);
    this.dataType = DataType.SystemDataType.ANY;
  }

  public AnyData(T data) {
    this(data, DataType.SystemDataType.ANY);
  }

  public AnyData(T data, DataType dataType) {
    super(data);
    this.dataType = dataType;
  }

  public AnyData(Identifier identifier, T data) {
    super(identifier, data);
    this.dataType = DataType.SystemDataType.ANY;
  }

  public AnyData(T data, boolean isConst) {
    this(data, DataType.SystemDataType.ANY, isConst);
  }

  public AnyData(T data, DataType dataType, boolean isConst) {
    super(data, isConst);
    this.dataType = dataType;
  }

  public AnyData(Identifier identifier, DataType dataType, T data) {
    this(identifier, dataType, data, false);
  }

  public AnyData(Identifier identifier, T data, boolean isConst) {
    this(identifier, DataType.SystemDataType.ANY, data, isConst);
  }

  public AnyData(Identifier identifier, DataType dataType, T data, boolean isConst) {
    super(identifier, data, isConst);
    this.dataType = dataType;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name=" + identifier.getId() +
        ", data=" + data +
        ", isConst=" + isConst +
        '}';
  }

  @Override
  public DataType getDataType() {
    return this.dataType;
  }
}
