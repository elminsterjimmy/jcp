package com.elminster.jcp.ast.func.module.system;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.data.FlowDataFactory;

public class PrintlnFunction extends SystemModuleFunction {

  @Override
  public String getName() {
    return "PRINTLN";
  }

  @Override
  protected FlowData doFunction(FlowData[] parameters) {
    Assert.isTrue(getParameters().length > 0);
    FlowData msg = getParameters()[0];
    System.out.println(msg.get());
    return AnyFlowData.EMPTY;
  }

  @Override
  public String getId() {
    return "println";
  }

  @Override
  public FlowData[] getParameterDefs() {
    return new FlowData[]{
        FlowDataFactory.INSTANCE.createFlowDataVariable("msg", DataType.STRING)
    };
  }

  @Override
  public DataType getResultDataType() {
    return DataType.VOID;
  }
}
