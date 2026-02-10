package com.elminster.jcp.eval.excpetion;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Locatable;
import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.eval.data.DataType;

import java.util.Arrays;
import java.util.StringJoiner;

public class UndeclaredException extends DeclarationException {

  protected UndeclaredException() {
    super();
  }

  protected UndeclaredException(String message, SourceLocation location) {
    super(message, location);
  }

  private static SourceLocation getLocationSafe(Object obj) {
    return obj instanceof Locatable ? ((Locatable) obj).getLocation() : null;
  }

  public static void throwFunctionUndeclaredException(Identifier identifier, DataType... dataTypes) {
    throw new FunctionUndeclaredException(identifier, getLocationSafe(identifier), dataTypes);
  }

  public static void throwVariableUndeclaredException(Identifier identifier) {
    throw new VariableUndeclaredException(identifier, getLocationSafe(identifier));
  }

  public static void throwDataTypeUndeclaredException(Identifier identifier) {
    throw new DataTypeUndeclaredException(identifier, getLocationSafe(identifier));
  }

  public static class FunctionUndeclaredException extends UndeclaredException {

    private static final String MESSAGE_PATTERN = "Function [%s] with parameter [%s] undeclared.";
    private final Identifier identifier;
    private final DataType[] dataTypes;

    public FunctionUndeclaredException(Identifier identifier, DataType... dataTypes) {
      this(identifier, null, dataTypes);
    }

    public FunctionUndeclaredException(Identifier identifier, SourceLocation location, DataType... dataTypes) {
      super(null, location);
      this.identifier = identifier;
      this.dataTypes = dataTypes;
    }

    @Override
    public String getMessage() {
      return appendLocation(generateMessage(identifier, dataTypes));
    }

    private static String generateMessage(Identifier identifier, DataType... dataTypes) {
      StringJoiner parameterJoiner = new StringJoiner(", ");
      Arrays.stream(dataTypes).forEach(dataType -> parameterJoiner.add(dataType.getName()));
      return String.format(MESSAGE_PATTERN, identifier.getId(), parameterJoiner);
    }
  }

  public static class VariableUndeclaredException extends UndeclaredException {

    private static final String MESSAGE_PATTERN = "Variable [%s] undeclared.";
    private final Identifier identifier;

    public VariableUndeclaredException(Identifier identifier) {
      this(identifier, null);
    }

    public VariableUndeclaredException(Identifier identifier, SourceLocation location) {
      super(null, location);
      this.identifier = identifier;
    }

    @Override
    public String getMessage() {
      return appendLocation(String.format(MESSAGE_PATTERN, identifier.getId()));
    }
  }

  public static class DataTypeUndeclaredException extends UndeclaredException {

    private static final String MESSAGE_PATTERN = "DataType [%s] undeclared.";
    private final Identifier identifier;

    public DataTypeUndeclaredException(Identifier identifier) {
      this(identifier, null);
    }

    public DataTypeUndeclaredException(Identifier identifier, SourceLocation location) {
      super(null, location);
      this.identifier = identifier;
    }

    @Override
    public String getMessage() {
      return appendLocation(String.format(MESSAGE_PATTERN, identifier.getId()));
    }
  }
}
