package com.elminster.jcp.eval;

import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.context.EvalContext;

public interface Evaluable {

  Data eval(EvalContext evalContext) throws Exception;
}
