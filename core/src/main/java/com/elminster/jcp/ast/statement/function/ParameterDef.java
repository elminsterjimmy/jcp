package com.elminster.jcp.ast.statement.function;

import com.elminster.jcp.eval.data.DataType;

public class ParameterDef {

  String id;
  DataType dataType;

  public ParameterDef(String id, DataType dataType) {
    this.id = id;
    this.dataType = dataType;
  }

  /**
   * Gets id.
   *
   * @return value of id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets id.
   *
   * @param id
   *     value of id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets dataType.
   *
   * @return value of dataType
   */
  public DataType getDataType() {
    return dataType;
  }

  /**
   * Sets dataType.
   *
   * @param dataType
   *     value of dataType
   */
  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }
}
