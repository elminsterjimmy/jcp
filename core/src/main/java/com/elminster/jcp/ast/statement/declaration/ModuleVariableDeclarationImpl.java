package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.module.Modulable;

public class ModuleVariableDeclarationImpl extends VariableDeclarationImpl implements Modulable {

    private final String moduleName;

    public ModuleVariableDeclarationImpl(Identifier id,
                                         String moduleName,
                                         DataType dataType) {
        super(id, dataType);
        this.moduleName = moduleName;
    }

    public ModuleVariableDeclarationImpl(Identifier id,
                                         String moduleName,
                                         DataType dataType,
                                         Expression initExpress) {
        super(id, dataType, initExpress);
        this.moduleName = moduleName;
    }

    public ModuleVariableDeclarationImpl(String id,
                                         String moduleName,
                                         DataType dataType) {
        super(id, dataType);
        this.moduleName = moduleName;
    }

    public ModuleVariableDeclarationImpl(String id,
                                         String moduleName,
                                         DataType dataType,
                                         Expression initExpress) {
        super(id, dataType, initExpress);
        this.moduleName = moduleName;
    }

    @Override
    public String getModule() {
        return moduleName;
    }
}
