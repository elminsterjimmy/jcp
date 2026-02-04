package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.data.DataType;

/**
 * Represents a single field definition within a struct.
 * Contains the field name and its data type.
 */
public class StructFieldDef {

  private final Identifier name;
  private final DataType dataType;

  public StructFieldDef(Identifier name, DataType dataType) {
    this.name = name;
    this.dataType = dataType;
  }

  public StructFieldDef(String name, DataType dataType) {
    this(Identifier.fromName(name), dataType);
  }

  public Identifier getName() {
    return name;
  }

  public DataType getDataType() {
    return dataType;
  }

  @Override
  public String toString() {
    return "StructFieldDef{" +
        "name=" + name.getId() +
        ", dataType=" + dataType.getName() +
        '}';
  }
}
