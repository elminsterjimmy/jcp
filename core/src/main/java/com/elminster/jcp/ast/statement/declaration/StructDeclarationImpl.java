package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.AbstractStatement;
import com.elminster.jcp.eval.data.DataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of struct declaration.
 * Defines a new struct type with named fields.
 */
public class StructDeclarationImpl extends AbstractStatement implements StructDeclaration {

  private final Identifier id;
  private final List<StructFieldDef> fields;

  public StructDeclarationImpl(Identifier id, List<StructFieldDef> fields) {
    this.id = id;
    this.fields = new ArrayList<>(fields);
  }

  public StructDeclarationImpl(String id, List<StructFieldDef> fields) {
    this(Identifier.fromName(id), fields);
  }

  @Override
  public Identifier getId() {
    return id;
  }

  @Override
  public DataType getDataType() {
    // The struct declaration itself doesn't have a data type in the traditional sense
    // The struct NAME becomes a new type
    return null;
  }

  @Override
  public List<StructFieldDef> getFields() {
    return fields;
  }

  @Override
  public String getName() {
    return "STRUCT_DECLARATION";
  }
}
