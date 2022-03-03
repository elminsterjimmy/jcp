package com.elminster.jcp.module.system;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;

public class PrintFunction extends SystemModuleFunction {

  @Override
  public String getName() {
    return "PRINT";
  }

  @Override
  protected Data doFunction(Data[] parameters, EvalContext evalContext) {
    Assert.isTrue(getArguments().length > 0);
    Data msg = getArguments()[0];
    System.out.println(msg.get());
    return AnyData.EMPTY;
  }

  @Override
  public Identifier getId() {
    return Identifier.fromName("print");
  }

  @Override
  public ParameterDef[] getParameterDefs() {
    return new ParameterDef[]{
        new ParameterDef("msg", DataType.SystemDataType.STRING)
    };
  }

  @Override
  public DataType getResultDataType() {
    return DataType.SystemDataType.VOID;
  }
}
