package com.elminster.jcp.eval.struct;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.StructInstantiation;
import com.elminster.jcp.ast.statement.declaration.MethodDef;
import com.elminster.jcp.ast.statement.declaration.StructFieldDef;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.collection.FastStack;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.context.DefaultEvalContext;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.StructData;
import com.elminster.jcp.eval.data.StructType;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluator for struct/type instantiation.
 * Creates a new instance with provided field values, optionally
 * running an explicit constructor.
 */
public class StructInstantiationEvaluator extends AbstractAstEvaluator {

  public StructInstantiationEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    StructInstantiation structInst = (StructInstantiation) astNode;
    String structTypeName = structInst.getStructType().getId();

    // Look up the struct type
    StructType structType = (StructType) evalContext.getDataType(structTypeName);
    if (structType == null) {
      throw new IllegalArgumentException("Unknown struct type: " + structTypeName);
    }

    List<Expression> argExprs = structInst.getFieldValues();

    // Check if type has explicit constructor
    if (structType.getConstructor() != null) {
      return evalExplicitConstructor(evalContext, structType, argExprs);
    } else {
      return evalAutoConstructor(evalContext, structType, argExprs);
    }
  }

  /**
   * Handle explicit constructor invocation.
   */
  private Data evalExplicitConstructor(EvalContext evalContext, StructType structType,
                                        List<Expression> argExprs) {
    MethodDef ctor = structType.getConstructor();
    ParameterDef[] params = ctor.getParameters();

    // Validate argument count
    if (params.length != argExprs.size()) {
      throw new IllegalArgumentException(
          String.format("Constructor %s expects %d arguments, but got %d",
              structType.getName(), params.length, argExprs.size()));
    }

    // Create instance with default field values
    StructData instance = new StructData(structType);
    for (StructFieldDef field : structType.getFields()) {
      instance.setField(field.getName().getId(), getDefaultValue(field.getDataType()));
    }

    // Create constructor scope
    FastStack<EvalContext> contextStack = evalContext.getContextStack();
    DefaultEvalContext ctorContext = new DefaultEvalContext();
    contextStack.push(ctorContext);

    try {
      // Bind 'this' to instance
      Data thisData = new AnyData<>(Identifier.fromName("this"), structType, instance, false);
      ctorContext.getVariables().put("this", thisData);

      // Evaluate and bind constructor parameters
      for (int i = 0; i < params.length; i++) {
        Data argValue = AstEvaluatorFactory.getEvaluator(argExprs.get(i)).eval(evalContext);

        // Type check
        if (!argValue.getDataType().isCastableTo(params[i].getDataType())) {
          CannotCastException ex = new CannotCastException(argValue.getDataType(), params[i].getDataType());
          ex.setLocation(getSourceLocation());
          throw ex;
        }

        Data paramData = new AnyData<>(Identifier.fromName(params[i].getId()),
            params[i].getDataType(), argValue.get(), false);
        ctorContext.getVariables().put(params[i].getId(), paramData);
      }

      // Execute constructor body
      new EvalVisitor(evalContext).visit(ctor.getBody());

      return instance;
    } finally {
      contextStack.pop();
    }
  }

  /**
   * Handle auto-generated constructor (positional field assignment).
   */
  private Data evalAutoConstructor(EvalContext evalContext, StructType structType,
                                    List<Expression> fieldValueExprs) {
    List<StructFieldDef> fields = structType.getFields();

    // Validate field count
    if (fields.size() != fieldValueExprs.size()) {
      throw new IllegalArgumentException(
          String.format("Type %s expects %d fields, but got %d",
              structType.getName(), fields.size(), fieldValueExprs.size()));
    }

    // Evaluate field values and type-check them
    Map<String, Data> fieldValues = new HashMap<>();
    for (int i = 0; i < fields.size(); i++) {
      StructFieldDef fieldDef = fields.get(i);
      Expression valueExpr = fieldValueExprs.get(i);

      Data value = AstEvaluatorFactory.getEvaluator(valueExpr).eval(evalContext);

      // Type check
      if (!value.getDataType().isCastableTo(fieldDef.getDataType())) {
        CannotCastException ex = new CannotCastException(value.getDataType(), fieldDef.getDataType());
        ex.setLocation(getSourceLocation());
        throw ex;
      }

      fieldValues.put(fieldDef.getName().getId(), value);
    }

    // Create and return the instance
    StructData structData = new StructData(structType);
    for (Map.Entry<String, Data> entry : fieldValues.entrySet()) {
      structData.setField(entry.getKey(), entry.getValue());
    }
    return structData;
  }

  /**
   * Get default value for a data type.
   */
  private Data getDefaultValue(DataType dataType) {
    if (dataType == DataType.SystemDataType.INT) {
      return new AnyData<>(0, DataType.SystemDataType.INT);
    } else if (dataType == DataType.SystemDataType.BOOLEAN) {
      return new AnyData<>(false, DataType.SystemDataType.BOOLEAN);
    } else if (dataType == DataType.SystemDataType.STRING) {
      return new AnyData<>("", DataType.SystemDataType.STRING);
    } else {
      // Reference types default to null-like empty
      return new AnyData<>((Object) null, dataType);
    }
  }
}
