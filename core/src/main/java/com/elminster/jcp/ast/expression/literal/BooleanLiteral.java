package com.elminster.jcp.ast.expression.literal;

public interface BooleanLiteral extends Literal<Boolean> {


    default String getName() {
        return "BooleanLiteral";
    }

    static BooleanLiteral of(Boolean value) {
        return () -> value;
    }
}
