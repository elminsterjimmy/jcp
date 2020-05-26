package com.elminster.jcp.ast.statement;

public class ParameterDef {

  String id;
  String dataType;

  public ParameterDef(String id, String dataType) {
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
  public String getDataType() {
    return dataType;
  }

  /**
   * Sets dataType.
   *
   * @param dataType
   *     value of dataType
   */
  public void setDataType(String dataType) {
    this.dataType = dataType;
  }
}
