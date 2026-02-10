package com.elminster.jcp.eval.excpetion;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Locatable;
import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.ast.statement.function.Function;

import java.util.Arrays;
import java.util.StringJoiner;

public class AlreadyDeclaredException extends DeclarationException {

  private static SourceLocation getLocationSafe(Object obj) {
    return obj instanceof Locatable ? ((Locatable) obj).getLocation() : null;
  }

  public static void throwFunctionAlreadyDeclaredException(Function function) {
    FunctionAlreadyDeclaredException ex = new FunctionAlreadyDeclaredException(function);
    ex.setLocation(getLocationSafe(function));
    throw ex;
  }

  public static void throwVariableAlreadyDeclaredException(Identifier identifier) {
    VariableAlreadyDeclaredException ex = new VariableAlreadyDeclaredException(identifier);
    ex.setLocation(getLocationSafe(identifier));
    throw ex;
  }

  public static void throwDataTypeAlreadyDeclaredException(Identifier identifier) {
    DataTypeAlreadyDeclaredException ex = new DataTypeAlreadyDeclaredException(identifier);
    ex.setLocation(getLocationSafe(identifier));
    throw ex;
  }

  public static class FunctionAlreadyDeclaredException extends AlreadyDeclaredException {

    private static final String MESSAGE_PATTERN = "Function [%s] with parameter [%s] already declared.";
    private final Function function;

    public FunctionAlreadyDeclaredException(Function function) {
      super();
      this.function = function;
    }

    @Override
    public String getMessage() {
      return appendLocation(generateMessage(function));
    }

    private static String generateMessage(Function function) {
      StringJoiner parameterJoiner = new StringJoiner(" ");
      Arrays.stream(function.getParameterDefs()).forEach(parameterDef -> parameterJoiner.add(parameterDef
              .getDataType().getName()));
      return String.format(MESSAGE_PATTERN, function.getName(), parameterJoiner);
    }
  }

  public static class VariableAlreadyDeclaredException extends AlreadyDeclaredException {

    private static final String MESSAGE_PATTERN = "Variable [%s] already declared.";
    private final Identifier identifier;

    public VariableAlreadyDeclaredException(Identifier identifier) {
      super();
      this.identifier = identifier;
    }

    @Override
    public String getMessage() {
      return appendLocation(String.format(MESSAGE_PATTERN, identifier.getId()));
    }
  }

  public static class DataTypeAlreadyDeclaredException extends AlreadyDeclaredException {

    private static final String MESSAGE_PATTERN = "DataType [%s] already declared.";
    private final Identifier identifier;

    public DataTypeAlreadyDeclaredException(Identifier identifier) {
      super();
      this.identifier = identifier;
    }

    @Override
    public String getMessage() {
      return appendLocation(String.format(MESSAGE_PATTERN, identifier.getId()));
    }
  }
}
