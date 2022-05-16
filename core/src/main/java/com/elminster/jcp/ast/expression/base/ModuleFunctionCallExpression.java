package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.module.Modulable;

public class ModuleFunctionCallExpression extends FunctionCallExpression implements Modulable {

    private final String moduleName;

    public ModuleFunctionCallExpression(Identifier id,
                                        String moduleName,
                                        Expression... arguments) {
        super(id, arguments);
        this.moduleName = moduleName;
    }

    @Override
    public String getModule() {
        return moduleName;
    }
}
