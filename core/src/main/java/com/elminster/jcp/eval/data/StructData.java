package com.elminster.jcp.eval.data;

import com.elminster.jcp.ast.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime data for struct instances.
 * Stores field values in a map keyed by field name.
 */
public class StructData extends AnyData<Map<String, Data>> {

  private final StructType structType;

  public StructData(StructType structType) {
    super(new HashMap<>(), structType, false);  // explicitly not const
    this.structType = structType;
  }

  public StructData(Identifier identifier, StructType structType) {
    super(identifier, structType, new HashMap<>(), false);  // explicitly not const
    this.structType = structType;
  }

  public StructData(Identifier identifier, StructType structType, Map<String, Data> fieldValues) {
    super(identifier, structType, new HashMap<>(fieldValues), false);  // explicitly not const
    this.structType = structType;
  }

  /**
   * Get the value of a field.
   */
  public Data getField(String fieldName) {
    return data.get(fieldName);
  }

  /**
   * Set the value of a field.
   */
  public void setField(String fieldName, Data value) {
    if (isConst) {
      throw new IllegalStateException("cannot modify a const struct instance.");
    }
    data.put(fieldName, value);
  }

  public StructType getStructType() {
    return structType;
  }

  @Override
  public String toString() {
    return "StructData{" +
        "type=" + structType.getName() +
        ", name=" + identifier.getId() +
        ", fields=" + data +
        '}';
  }
}
