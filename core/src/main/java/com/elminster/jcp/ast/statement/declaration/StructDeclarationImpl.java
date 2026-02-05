package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.AbstractStatement;
import com.elminster.jcp.eval.data.DataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of type declaration (formerly struct).
 * Defines a new type with fields, constructor, and methods.
 */
public class StructDeclarationImpl extends AbstractStatement implements StructDeclaration {

  private final Identifier id;
  private final List<StructFieldDef> fields;
  private final MethodDef constructor;
  private final List<MethodDef> instanceMethods;
  private final List<MethodDef> staticMethods;

  /**
   * Field-only constructor (backward compatible).
   */
  public StructDeclarationImpl(Identifier id, List<StructFieldDef> fields) {
    this(id, fields, null, Collections.emptyList(), Collections.emptyList());
  }

  /**
   * Field-only constructor with string id (backward compatible).
   */
  public StructDeclarationImpl(String id, List<StructFieldDef> fields) {
    this(Identifier.fromName(id), fields);
  }

  /**
   * Full constructor with methods support.
   */
  public StructDeclarationImpl(Identifier id, List<StructFieldDef> fields, MethodDef constructor,
                               List<MethodDef> instanceMethods, List<MethodDef> staticMethods) {
    this.id = id;
    this.fields = new ArrayList<>(fields);
    this.constructor = constructor;
    this.instanceMethods = new ArrayList<>(instanceMethods);
    this.staticMethods = new ArrayList<>(staticMethods);
  }

  /**
   * Full constructor with string id.
   */
  public StructDeclarationImpl(String id, List<StructFieldDef> fields, MethodDef constructor,
                               List<MethodDef> instanceMethods, List<MethodDef> staticMethods) {
    this(Identifier.fromName(id), fields, constructor, instanceMethods, staticMethods);
  }

  @Override
  public Identifier getId() {
    return id;
  }

  @Override
  public DataType getDataType() {
    // The type declaration itself doesn't have a data type in the traditional sense
    // The type NAME becomes a new type
    return null;
  }

  @Override
  public List<StructFieldDef> getFields() {
    return fields;
  }

  @Override
  public MethodDef getConstructor() {
    return constructor;
  }

  @Override
  public List<MethodDef> getInstanceMethods() {
    return instanceMethods;
  }

  @Override
  public List<MethodDef> getStaticMethods() {
    return staticMethods;
  }

  @Override
  public String getName() {
    // Return TYPE_DECLARATION for factory lookup
    // This enables both 'struct' and 'type' keywords to work
    return "TYPE_DECLARATION";
  }
}
