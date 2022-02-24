package com.elminster.jcp.ast.func.module.system;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.ParameterDef;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;

public class PrintlnFunction extends SystemModuleFunction {

  @Override
  public String getName() {
    return "PRINTLN";
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
    return Identifier.fromName("println");
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
