package com.elminster.jcp.ast.expression.literal;

public interface StringLiteral extends Literal<String> {

    default String getName() {
        return "StringLiteral";
    }

    static StringLiteral of(String value) {
        return () -> value;
    }
}
