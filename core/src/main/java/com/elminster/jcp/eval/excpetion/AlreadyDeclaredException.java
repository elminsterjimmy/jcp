package com.elminster.jcp.eval.excpetion;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.Function;

import java.util.Arrays;
import java.util.StringJoiner;

public class AlreadyDeclaredException extends DeclarationException {

  public static void throwFunctionAlreadyDeclaredException(Function function) {
    throw new FunctionAlreadyDeclaredException(function);
  }

  public static void throwVariableAlreadyDeclaredException(Identifier identifier) {
    throw new VariableAlreadyDeclaredException(identifier);
  }

  public static void throwDataTypeAlreadyDeclaredException(Identifier identifier) {
    throw new DataTypeAlreadyDeclaredException(identifier);
  }

  static class FunctionAlreadyDeclaredException extends AlreadyDeclaredException {

    private static final String MESSAGE_PATTERN = "Function [%s] with parameter [%s] already declared.";
    private final Function function;

    public FunctionAlreadyDeclaredException(Function function) {
      super();
      this.function = function;
    }

    @Override
    public String getMessage() {
      return generateMessage(function);
    }

    private static String generateMessage(Function function) {
      StringJoiner parameterJoiner = new StringJoiner(" ");
      Arrays.stream(function.getParameterDefs()).forEach(parameterDef -> parameterJoiner.add(parameterDef
              .getDataType().getName()));
      return String.format(MESSAGE_PATTERN, function.getName(), parameterJoiner);
    }
  }

  static class VariableAlreadyDeclaredException extends AlreadyDeclaredException {

    private static final String MESSAGE_PATTERN = "Variable [%s] already declared.";
    private final Identifier identifier;

    public VariableAlreadyDeclaredException(Identifier identifier) {
      super();
      this.identifier = identifier;
    }

    @Override
    public String getMessage() {
      return String.format(MESSAGE_PATTERN, identifier.getId());
    }
  }

  static class DataTypeAlreadyDeclaredException extends AlreadyDeclaredException {

    private static final String MESSAGE_PATTERN = "DataType [%s] already declared.";
    private final Identifier identifier;

    public DataTypeAlreadyDeclaredException(Identifier identifier) {
      super();
      this.identifier = identifier;
    }

    @Override
    public String getMessage() {
      return String.format(MESSAGE_PATTERN, identifier.getId());
    }
  }
}
