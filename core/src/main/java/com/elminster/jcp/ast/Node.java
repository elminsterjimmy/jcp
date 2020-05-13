package com.elminster.jcp.ast;

import com.elminster.jcp.debug.Debuggable;
import com.elminster.jcp.eval.Evaluable;

public interface Node extends Evaluable, Debuggable {

  String getName();
}
