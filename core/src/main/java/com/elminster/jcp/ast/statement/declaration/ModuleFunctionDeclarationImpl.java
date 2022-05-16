package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.module.Modulable;

public class ModuleFunctionDeclarationImpl extends FunctionDeclarationImpl implements Modulable {

    private final String moduleName;

    public ModuleFunctionDeclarationImpl(Identifier id,
                                         String moduleName,
                                         DataType returnType,
                                         ParameterDef[] parameterDefines,
                                         Statement... statements) {
        super(id, returnType, parameterDefines, statements);
        this.moduleName = moduleName;
    }

    public ModuleFunctionDeclarationImpl(Identifier id,
                                         String moduleName,
                                         DataType returnType,
                                         ParameterDef[] parameterDefines,
                                         Block block) {
        super(id, returnType, parameterDefines, block);
        this.moduleName = moduleName;
    }

    @Override
    public String getModule() {
        return moduleName;
    }
}
