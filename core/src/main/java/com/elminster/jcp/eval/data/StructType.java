package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.statement.declaration.StructFieldDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom data type for user-defined structs.
 * Each struct declaration creates a new StructType.
 */
public class StructType implements DataType {

  private final String name;
  private final List<StructFieldDef> fields;

  public StructType(String name, List<StructFieldDef> fields) {
    this.name = name;
    this.fields = new ArrayList<>(fields);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public DataType getParent() {
    return SystemDataType.ANY;
  }

  public List<StructFieldDef> getFields() {
    return fields;
  }

  public StructFieldDef getField(String fieldName) {
    for (StructFieldDef field : fields) {
      if (field.getName().getId().equals(fieldName)) {
        return field;
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    StructType that = (StructType) obj;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return "StructType{" + name + "}";
  }
}
